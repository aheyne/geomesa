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
import org.locationtech.geomesa.utils.conf.GeoMesaSystemProperties.SystemProperty
import org.locationtech.geomesa.utils.io.CloseWithLogging

import scala.util.control.NonFatal

trait LeafletExportCommand[DS <: DataStore] extends ExportCommandInterface[DS] {

  import LeafletExportCommand._
  import org.locationtech.geomesa.tools.export.ExportCommandInterface._
  import Command.user

  override val name = "export-leaflet"
  override def params: LeafletExportParams

  override def execute(): Unit = {
    profile(withDataStore(export)) { (count, time) =>
      Command.user.info(s"Leaflet export completed in ${time}ms${count.map(" for " + _ + " features").getOrElse("")}")
    }
  }

  protected def export(ds: DS): Option[Long] = {

    val (query, _) = createQuery(getSchema(ds), None, params)

    val features = try { getFeatures(ds, query) } catch {
      case NonFatal(e) =>
        throw new RuntimeException("Could not execute export query. Please ensure " +
            "that all arguments are correct", e)
    }

    val GEOMESA_HOME = SystemProperty("geomesa.home")
    val root = new File(GEOMESA_HOME.getOrElse("/tmp"))

    val dest: File = Option(params.file) match {
      case Some(file) => checkDestination(file)
      case None =>
        checkDestination(new File(root, "leaflet"))
    }
    val indexFile: File = new File(dest, "index.html")
    val exporter = new LeafletExporter(indexFile)

    try {
      user.info("Writing output to " + indexFile.toString)
      exporter.start(features.getSchema)
      val res = export(exporter, features)

      // Use println to ensure we write the destination to stdout
      // so the bash wrapper can pick it up.
      System.out.println("Successfully wrote Leaflet html to: " + indexFile.toString)

      res
    } finally {
      CloseWithLogging(exporter)
    }
  }
}

object LeafletExportCommand {

  def checkDestination(file: File): File = {
    try {
      file.mkdir()
      if (! file.isDirectory) {
        throw new RuntimeException(s"Output destination ${file.toString} must not exist or must be a directory.")
      }
      file
    } catch {
      case e: SecurityException => throw new RuntimeException("Unable to create output destination, check permissions.", e)
    }
  }
}