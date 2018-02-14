/***********************************************************************
 * Copyright (c) 2013-2018 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.tools.export.formats

import java.io._
import java.nio.file.{Files, StandardCopyOption}
import javax.xml.parsers.DocumentBuilderFactory

import com.typesafe.scalalogging.LazyLogging
import org.geotools.feature.visitor.{BoundsVisitor, CalcResult}
import org.geotools.geojson.feature.FeatureJSON
import org.geotools.geometry.jts.ReferencedEnvelope
import org.geotools.referencing.CRS
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}
import org.w3c.dom.Document

import scala.io.{Codec, Source}

class LeafletExporter(dataWriter: Writer, htmlFile: File) extends FeatureExporter with LazyLogging {

  private val json = new FeatureJSON()

  private var first = true

  private val visitor = new BoundsVisitor

  override def start(sft: SimpleFeatureType): Unit = {
    dataWriter.write("""{type:"FeatureCollection",features:[""")
  }

  override def export(features: Iterator[SimpleFeature]): Option[Long] = {
    var count = 0L
    features.foreach { feature =>
      visitor.visit(feature)
      if (first) {
        first = false
      } else {
        dataWriter.write(",")
      }
      json.writeFeature(feature, dataWriter)
      count += 1L
    }
    dataWriter.flush()
    Some(count)
  }

  override def close(): Unit  = {
    dataWriter.write("]}\n")
    dataWriter.close()

    logger.info("Writing Leaflet HTML")
    Files.copy(getClass.getResourceAsStream("leafletIndex.html"), htmlFile.toPath, StandardCopyOption.REPLACE_EXISTING)

    val htmlRaw = Source.fromInputStream(getClass.getResourceAsStream("leafletIndex.html"), Codec.UTF8.toString()).mkString

    val visitorResult: CalcResult = visitor.getResult
    val envelope: ReferencedEnvelope = visitorResult.getValue.asInstanceOf[ReferencedEnvelope]
    val centerX: Double = envelope.getMedian(0)
    val centerY: Double = envelope.getMedian(1)
    htmlRaw.replace("|mapCenter.x|", centerX.toString).replace("|mapCenter.y|", centerY.toString)

    val bbox = envelope.toBounds(CRS.decode("EPSG:4326"))
    val maxDimention = math.max(bbox.getWidth, bbox.getHeight)

    val factory = DocumentBuilderFactory.newInstance
    val builder = factory.newDocumentBuilder
    val htmlDoc: Document = builder.parse(getClass.getResourceAsStream("leafletIndex.html"))
    htmlDoc.

    File html = new File(getClass.getResource("leafletIndex.html"))

  }


}
