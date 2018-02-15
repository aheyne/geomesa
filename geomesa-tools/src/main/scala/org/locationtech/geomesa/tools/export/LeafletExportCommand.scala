/***********************************************************************
 * Copyright (c) 2013-2018 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.tools.export

import java.io._

import org.geotools.data.DataStore
import org.locationtech.geomesa.tools.Command
import org.locationtech.geomesa.tools.export.formats.LeafletExporter
import org.locationtech.geomesa.utils.io.CloseWithLogging

import scala.util.control.NonFatal

trait LeafletExportCommand[DS <: DataStore] extends ExportCommand[DS] {

  override val name = "export-leaflet"
  override def params: LeafletExportParams

  override def execute(): Unit = {
    profile(withDataStore(export)) { (count, time) =>
      Command.user.info(s"Feature export complete to ${Option(params.file).map(_.getPath).getOrElse("standard out")} " +
          s"in ${time}ms${count.map(" for " + _ + " features").getOrElse("")}")
    }
  }

  override protected def export(ds: DS): Option[Long] = {
    import ExportCommand._

    val (query, _) = createQuery(getSchema(ds), None, params)

    val features = try { getFeatures(ds, query) } catch {
      case NonFatal(e) =>
        throw new RuntimeException("Could not execute export query. Please ensure " +
            "that all arguments are correct", e)
    }

    val (dataFile, htmlFile) = getDestinations(params.file)
    val exporter = new LeafletExporter(getWriter(dataFile), htmlFile)

    try {
      exporter.start(features.getSchema)
      export(exporter, features)
    } finally {
      CloseWithLogging(exporter)
    }
  }

  private def getDestinations(file: File): (File, File) = {
    if (! file.exists()) {
      try { file.mkdir() } catch {
        case e: SecurityException => throw new RuntimeException("Unable to create output destination.", e)
      }
    }
    if(! file.isDirectory) {
      throw new RuntimeException("Output destination must not exist or must be a directory.")
    } else {
      val dataFile: File = new File(file.getAbsolutePath + "/data.js")
      val htmlFile: File = new File(file.getAbsolutePath + "/index.html")
      (dataFile, htmlFile)
    }
  }
}
