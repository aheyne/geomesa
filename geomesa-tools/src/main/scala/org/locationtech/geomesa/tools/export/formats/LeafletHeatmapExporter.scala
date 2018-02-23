/***********************************************************************
 * Copyright (c) 2013-2018 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.tools.export.formats

import java.io._

import com.typesafe.scalalogging.LazyLogging
import org.geotools.geojson.feature.FeatureJSON
import org.locationtech.geomesa.tools.export.ExportCommand
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}

import scala.collection.JavaConversions._
import scala.io.Source
import com.vividsolutions.jts.geom._
import scala.collection.mutable

class LeafletHeatmapExporter(indexFile: File) extends FeatureExporter with LazyLogging {

  private var first = true
  private val coordMap = mutable.Map[SimpleCoordinate[Double], Int]()

  val (indexHead, indexTail): (String, String) = {
    val indexStream: InputStream = getClass.getClassLoader.getResourceAsStream("leaflet/heatmapIndex.html")
    try {
      val indexString: String = Source.fromInputStream(indexStream).mkString
      val indexArray: Array[String] = indexString.split("\\|data\\|")
      require(indexArray.length == 2, "Malformed index.html unable to render map.")
      (indexArray(0), indexArray(1))
    } finally {
      indexStream.close()
    }
  }

  val indexWriter: Writer = ExportCommand.getWriter(indexFile, null)

  override def start(sft: SimpleFeatureType): Unit = {
    indexWriter.write(indexHead)
  }

  override def export(features: Iterator[SimpleFeature]): Option[Long] = {
    var count = 0L
    features.foreach { feature =>
      storeFeature(feature)
      count += 1L
    }
    Some(count)
  }

  override def close(): Unit  = {
    val values = normalizeValues(coordMap)
    indexWriter.write("""var heat = L.heatLayer([""" + "\n")
    values.foreach{ c =>
      if (first) {
        first = false
      } else {
        indexWriter.write(",")
      }
      indexWriter.write("\n")
      indexWriter.write(c._1.render(c._2))
    }
    indexWriter.write("\n    ], {radius: 25});\n\n")
    indexWriter.write(indexTail)
    indexWriter.flush()
    indexWriter.close()
  }

  def storeFeature(feature: SimpleFeature): Unit = {
    val coords: Array[Coordinate] = feature.getDefaultGeometry match {
      case geom: Geometry => geom.getCoordinates
      case _ => Array[Coordinate]()
    }
    coords.map(c => SimpleCoordinate(c.x, c.y)).foreach{ sc =>
      coordMap.put(sc, 1) match {
        case Some(count) => coordMap.put(sc, count + 1)
        case None =>
      }
    }
  }

  def normalizeValues(coordMap: mutable.Map[SimpleCoordinate[Double], Int]): Map[SimpleCoordinate[Double], Float] = {
    val max: Float = coordMap.maxBy(_._2)._2
    coordMap.map(c => (c._1, c._2 / max)).toMap
  }

  case class SimpleCoordinate[@specialized(Double) T](x: T, y: T) {
    def render(weight: Float): String = {
      s"""        [${x.toString}, ${y.toString}, ${weight.toString}]"""
    }
  }
}
