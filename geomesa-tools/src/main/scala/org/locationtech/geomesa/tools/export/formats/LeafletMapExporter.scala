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

class LeafletMapExporter(indexFile: File) extends FeatureExporter with LazyLogging {

  private val json = new FeatureJSON()

  private var first = true
  private var sft: SimpleFeatureType = _

  val (indexHead, indexTail): (String, String) = {
    val indexStream: InputStream = getClass.getClassLoader.getResourceAsStream("leaflet/featuresIndex.html")
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
    indexWriter.write("""var data = {"type":"FeatureCollection","features":[""")
  }

  override def export(features: Iterator[SimpleFeature]): Option[Long] = {
    var count = 0L
    features.foreach { feature =>
      if (first) {
        sft = feature.getFeatureType
        first = false
      } else {
        indexWriter.write(",")
      }
      json.writeFeature(feature, indexWriter)
      count += 1L
    }
    indexWriter.flush()
    Some(count)
  }

  override def close(): Unit  = {
    indexWriter.write("]};\n\n")
    indexWriter.write(getFeatureInfo)
    indexWriter.write(indexTail)
    indexWriter.close()
  }

  private def getFeatureInfo: String = {
    val str: StringBuilder = new StringBuilder()
    str.append("    function onEachFeature(feature, layer) {\n")
    Option(sft) match {
      case None    => str.append("    }").toString
      case Some(_) =>
        str.append("""        layer.bindPopup("ID: " + feature.id + "<br>" + """)
        str.append(""""GEOM: " + feature.geometry.type + "[" + feature.geometry.coordinates + "]<br>" + """)
        str.append(sft.getAttributeDescriptors.filter(_.getLocalName != "geom") .map(attr =>
          s""""${attr.getLocalName}: " + feature.properties.${attr.getLocalName}"""
        ).mkString(""" + "<br>" + """))
        str.append(");\n    }")
        str.toString()
    }
  }
}
