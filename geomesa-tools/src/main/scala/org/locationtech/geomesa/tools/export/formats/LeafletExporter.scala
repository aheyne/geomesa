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
import org.locationtech.geomesa.tools.export.ExportCommandInterface
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}
import org.locationtech.geomesa.tools.Command.user

import scala.io.Source

class LeafletExporter(indexFile: File) extends FeatureExporter with LazyLogging {

  private val json = new FeatureJSON()

  private var first = true

  val (indexHead, indexTail): (String, String) = {
    val indexStream: InputStream = getClass.getClassLoader.getResourceAsStream("leaflet/index.html")
    try {
      val indexString: String = Source.fromInputStream(indexStream).mkString
      val indexArray: Array[String] = indexString.split("\\|data\\|")
      require(indexArray.length == 2, "Malformed index.html unable to render map.")
      (indexArray(0), indexArray(1))
    } finally {
      indexStream.close()
    }
  }

  val indexWriter: Writer = ExportCommandInterface.getWriter(indexFile, null)

  override def start(sft: SimpleFeatureType): Unit = {
    indexWriter.write(indexHead)
    indexWriter.write("""{"type":"FeatureCollection","features":[""")
  }

  override def export(features: Iterator[SimpleFeature]): Option[Long] = {
    var count = 0L
    features.foreach { feature =>
      if (first) {
        first = false
      } else {
        indexWriter.write(",")
      }
      json.writeFeature(feature, indexWriter)
      count += 1L
    }
    indexWriter.flush()
    if (count < 1) user.warn("No features were exported. This will cause the map to fail to render correctly.")
    if (count > 500) user.warn("Large number of features were exported. This can cause performance issues. For large numbers of points try using the flag --heatmap or using GeoServer to render the map.")
    Some(count)
  }

  override def close(): Unit  = {
    indexWriter.write("]};\n")
    indexWriter.write(indexTail)
    indexWriter.close()
  }
}
