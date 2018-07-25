/***********************************************************************
 * Copyright (c) 2013-2018 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.memory.cqengine

import java.util
import java.util.UUID

import com.googlecode.cqengine.attribute.Attribute
import com.googlecode.cqengine.index.hash.HashIndex
import com.googlecode.cqengine.index.navigable.NavigableIndex
import com.googlecode.cqengine.index.radix.RadixTreeIndex
import com.googlecode.cqengine.index.unique.UniqueIndex
import com.googlecode.cqengine.query.option.DeduplicationStrategy
import com.googlecode.cqengine.query.simple.{All, Equal}
import com.googlecode.cqengine.query.{Query, QueryFactory}
import com.googlecode.cqengine.{ConcurrentIndexedCollection, IndexedCollection}
import com.typesafe.scalalogging.slf4j.LazyLogging
import com.vividsolutions.jts.geom.Geometry
import org.locationtech.geomesa.memory.cqengine.index.GeoIndex
import org.locationtech.geomesa.memory.cqengine.utils._
import org.opengis.feature.`type`.AttributeDescriptor
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}
import org.opengis.filter._

import scala.collection.JavaConversions._

class GeoCQEngine(val sft: SimpleFeatureType,
                  enableFidIndex: Boolean = false,
                  enableGeomIndex: Boolean = true) extends LazyLogging {
  //val cqcache = CQIndexingOptions.buildIndexedCollection(sft)
  val cqcache: IndexedCollection[SimpleFeature] = new ConcurrentIndexedCollection[SimpleFeature]()
  val attributes = SFTAttributes(sft)

  // Add Geometry index on default geometry first.
  // TODO: Add logic to allow for the geo-index to be disabled?  (Low priority)
  if (enableGeomIndex) addGeoIndex(sft.getGeometryDescriptor)

  if (enableFidIndex) addFidIndex()

  // Add other indexes
  sft.getAttributeDescriptors.foreach(addIndex)

  def remove(sf: SimpleFeature): Boolean = {
    cqcache.remove(sf)
  }

  def add(sf: SimpleFeature): Boolean = {
    cqcache.add(sf)
  }

  def addAll(sfs: util.Collection[SimpleFeature]): Boolean = {
    cqcache.addAll(sfs)
  }

  def clear(): Unit = {
    cqcache.clear()
  }

  def getById(id: String): Option[SimpleFeature] = {
    // if this gets used, set enableFidIndex=true
    cqcache.retrieve(new Equal(SFTAttributes.fidAttribute, id)).headOption
  }

  def update(sf: SimpleFeature): Boolean = {
    getById(sf.getID) match {
      case Some(oldsf) => cqcache.remove(oldsf)
      case None =>
    }
    cqcache.add(sf)
  }

  // NB: We expect that FID filters have been handled previously
  def getReaderForFilter(filter: Filter): Iterator[SimpleFeature] =
    filter match {
      case f: IncludeFilter => include(f)
      case f => queryCQ(f, dedup = true)
    }

  def queryCQ(f: Filter, dedup: Boolean = true): Iterator[SimpleFeature] = {
    val visitor = new CQEngineQueryVisitor(sft)

    val query: Query[SimpleFeature] = f.accept(visitor, null) match {
      case q: Query[SimpleFeature] => q
      case _ => throw new Exception(s"Filter visitor didn't recognize filter: $f.")
    }
    val iter = if (dedup) {
      val dedupOpt = QueryFactory.deduplicate(DeduplicationStrategy.LOGICAL_ELIMINATION)
      val queryOptions = QueryFactory.queryOptions(dedupOpt)
      cqcache.retrieve(query, queryOptions).iterator()
    } else {
      cqcache.retrieve(query).iterator()
    }
    if (query.isInstanceOf[All[_]]) iter.filter(f.evaluate(_))
    else iter
  }

  def include(i: IncludeFilter): Iterator[SimpleFeature] =
    cqcache.retrieve(new All(classOf[SimpleFeature])).iterator()

  private def addIndex(ad: AttributeDescriptor): Unit = {
    CQIndexingOptions.getCQIndexType(ad) match {
      case CQIndexType.DEFAULT =>
        ad.getType.getBinding match {
          // Comparable fields should have a Navigable Index
          case c if
          classOf[java.lang.Integer].isAssignableFrom(c) ||
            classOf[java.lang.Long].isAssignableFrom(c) ||
            classOf[java.lang.Float].isAssignableFrom(c) ||
            classOf[java.lang.Double].isAssignableFrom(c) ||
            classOf[java.util.Date].isAssignableFrom(c) => addNavigableIndex(ad)
          case c if classOf[java.lang.String].isAssignableFrom(c) => addRadixIndex(ad)
          case c if classOf[Geometry].isAssignableFrom(c) => addGeoIndex(ad)
          case c if classOf[UUID].isAssignableFrom(c) => addUniqueIndex(ad)
          // TODO: Decide how boolean fields should be indexed
          case c if classOf[java.lang.Boolean].isAssignableFrom(c) => addHashIndex(ad)
        }

      case CQIndexType.NAVIGABLE => addNavigableIndex(ad)
      case CQIndexType.RADIX => addRadixIndex(ad)
      case CQIndexType.UNIQUE => addUniqueIndex(ad)
      case CQIndexType.HASH => addHashIndex(ad)
      case CQIndexType.NONE => // NO-OP
    }
  }

  private def addFidIndex(): Unit = {
    val attribute = SFTAttributes.fidAttribute
    cqcache.addIndex(HashIndex.onAttribute(attribute))
  }

  private def addGeoIndex(ad: AttributeDescriptor): Unit = {
    val geom: Attribute[SimpleFeature, Geometry] = attributes.lookup[Geometry](ad.getLocalName)
    cqcache.addIndex(GeoIndex.onAttribute(sft, geom))
  }

  private def addNavigableIndex(ad: AttributeDescriptor): Unit = {
    val binding = ad.getType.getBinding
    binding match {
      case c if classOf[java.lang.Integer].isAssignableFrom(c) => {
        val attr = attributes.lookup[java.lang.Integer](ad.getLocalName)
        cqcache.addIndex(NavigableIndex.onAttribute(attr))
      }
      case c if classOf[java.lang.Long].isAssignableFrom(c) => {
        val attr = attributes.lookup[java.lang.Long](ad.getLocalName)
        cqcache.addIndex(NavigableIndex.onAttribute(attr))
      }
      case c if classOf[java.lang.Float].isAssignableFrom(c) => {
        val attr = attributes.lookup[java.lang.Float](ad.getLocalName)
        cqcache.addIndex(NavigableIndex.onAttribute(attr))
      }
      case c if classOf[java.lang.Double].isAssignableFrom(c) => {
        val attr = attributes.lookup[java.lang.Double](ad.getLocalName)
        cqcache.addIndex(NavigableIndex.onAttribute(attr))
      }
      case c if classOf[java.util.Date].isAssignableFrom(c) => {
        val attr = attributes.lookup[java.util.Date](ad.getLocalName)
        cqcache.addIndex(NavigableIndex.onAttribute(attr))
      }
      case _ => logger.warn(s"Failed to add a Navigable index for attribute ${ad.getLocalName}")
    }
  }

  private def addRadixIndex(ad: AttributeDescriptor): Unit = {
    if (classOf[java.lang.String].isAssignableFrom(ad.getType.getBinding)) {
      val attribute: Attribute[SimpleFeature, String] = attributes.lookup(ad.getLocalName)
      cqcache.addIndex(RadixTreeIndex.onAttribute(attribute))
    } else {
      logger.warn(s"Failed to add a Radix index for attribute ${ad.getLocalName}.")
    }
  }

  private def addUniqueIndex(ad: AttributeDescriptor): Unit = {
    val attribute: Attribute[SimpleFeature, Any] = attributes.lookup(ad.getLocalName)
    cqcache.addIndex(UniqueIndex.onAttribute(attribute))
  }

  private def addHashIndex(ad: AttributeDescriptor): Unit = {
    val attribute: Attribute[SimpleFeature, Any] = attributes.lookup(ad.getLocalName)
    cqcache.addIndex(HashIndex.onAttribute(attribute))
  }
}
