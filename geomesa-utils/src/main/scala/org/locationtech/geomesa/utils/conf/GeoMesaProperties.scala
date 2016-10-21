/***********************************************************************
* Copyright (c) 2013-2016 Commonwealth Computer Research, Inc.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Apache License, Version 2.0
* which accompanies this distribution and is available at
* http://www.opensource.org/licenses/apache2.0.php.
*************************************************************************/

package org.locationtech.geomesa.utils.conf

import java.util.Properties

import com.typesafe.scalalogging.LazyLogging

object GeoMesaProperties extends LazyLogging {

   val EmbeddedFile = "/org/locationtech/geomesa/geomesa.properties"

   val props: Properties = {
    val resource = getClass.getResourceAsStream(EmbeddedFile)
    if (resource == null) {
      logger.warn(s"Couldn't load $EmbeddedFile")
      new Properties
    } else {
      val p = new Properties
      try {
        p.load(resource)
      } finally {
        resource.close()
      }
      p
    }
  }

  val ProjectVersion = props.getProperty("geomesa.project.version")
  val BuildDate      = props.getProperty("geomesa.build.date")
  val GitCommit      = props.getProperty("geomesa.build.commit.id")
  val GitBranch      = props.getProperty("geomesa.build.branch")

  val GEOMESA_TOOLS_ACCUMULO_SITE_XML   = PropOrDefault("geomesa.tools.accumulo.site.xml",
    s"${System.getenv("ACCUMULO_HOME")}/conf/accumulo-site.xml")
  val GEOMESA_AUDIT_PROVIDER_IMPL       = PropOrDefault("geomesa.audit.provider.impl")
  val GEOMESA_AUTH_PROVIDER_IMPL        = PropOrDefault("geomesa.auth.provider.impl")
  val GEOMESA_BATCHWRITER_LATENCY_MILLS = PropOrDefault("geomesa.batchwriter.latency.millis")
  val GEOMESA_BATCHWRITER_MAXTHREADS    = PropOrDefault("geomesa.batchwriter.maxthreads")
  val GEOMESA_BATCHWRITER_MEMORY        = PropOrDefault("geomesa.batchwriter.memory")
  val GEOMESA_BATCHWRITER_TIMEOUT_MILLS = PropOrDefault("geomesa.batchwriter.timeout.millis")
  val GEOMESA_CONVERT_CONFIG_URLS       = PropOrDefault("geomesa.convert.config.urls")
  val GEOMESA_CONVERT_SCRIPTS_PATH      = PropOrDefault("geomesa.convert.scripts.path")
  val GEOMESA_FEATURE_ID_GENERATOR      = PropOrDefault("geomesa.feature.id-generator")
  val GEOMESA_FORCE_COUNT               = PropOrDefault("geomesa.force.count")
  val GEOMESA_QUERY_COST_TYPE           = PropOrDefault("geomesa.query.cost.type")
  val GEOMESA_QUERY_TIMEOUT_MILLIS      = PropOrDefault("geomesa.query.timeout.millis")
  val GEOMESA_SCAN_RANGES_TARGET        = PropOrDefault("geomesa.scan.ranges.target")
  val GEOMESA_SCAN_RANGES_BATCH         = PropOrDefault("geomesa.scan.ranges.batch")
  val GEOMESA_SFT_CONFIG_URLS           = PropOrDefault("geomesa.sft.config.urls")
  val GEOMESA_STATS_COMPACT_MILLIS      = PropOrDefault("geomesa.stats.compact.millis")

  case class PropOrDefault(prop: String, dft: String = "") {
    val default = if (dft.nonEmpty) dft
                  else Option(props.getProperty(prop)).getOrElse(dft)
    def get: String = {
      ensureConfig()
      Option(System.getProperty(prop)).getOrElse(default)
    }
    def option: Option[String] = {
      ensureConfig()
      Option{
        Option(System.getProperty(prop)).getOrElse {
          if (default.nonEmpty) default else null
        }
      }
    }
    def set(value: String): Unit = System.setProperty(prop, value)
    def clear(): Unit = System.clearProperty(prop)
  }

  // For dynamic properties that are not in geomesa.properties
  // This ensures the config is always loaded and allows it to be used for all sys props
  def getProperty(prop: String, default: String = ""): String = {
    ensureConfig()
    Option(System.getProperty(prop)).getOrElse{
      if (default.nonEmpty) default
      else Option(props.getProperty(prop)).getOrElse("")
    }
  }

  def ensureConfig(): Unit = if(! ConfigLoader.isLoaded) ConfigLoader.init()
}
