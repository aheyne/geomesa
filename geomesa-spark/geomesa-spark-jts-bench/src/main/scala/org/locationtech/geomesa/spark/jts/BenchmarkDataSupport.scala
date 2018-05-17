/***********************************************************************
 * Copyright (c) 2018 Astraea, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.spark.jts

import com.vividsolutions.jts.geom.Geometry
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.storage.StorageLevel.MEMORY_ONLY
import org.geotools.geojson.feature.FeatureJSON
import org.locationtech.geomesa.spark.jts.util.WKTUtils
import org.opengis.feature.Feature
import org.opengis.feature.simple.SimpleFeature

import scala.beans.BeanProperty
import scala.collection.mutable.ListBuffer

/**
 *
 *
 * @since 5/17/18
 */
trait BenchmarkDataSupport {
  val wktExamples = Map(
    "POINT" -> "POINT (30 10)",
    "LINESTRING" -> "LINESTRING (30 10, 10 30, 40 40)",
    "POLYGON_1" ->  "POLYGON ((30 10, 40 40, 20 40, 10 20, 30 10))",
    "POLYGON_2" ->  "POLYGON ((35 10, 45 45, 15 40, 10 20, 35 10),(20 30, 35 35, 30 20, 20 30))",
    "MULTIPOINT" -> "MULTIPOINT ((10 40), (40 30), (20 20), (30 10))",
    "MULTILINESTRING" -> "MULTILINESTRING ((10 10, 20 20, 10 40), (40 40, 30 30, 40 20, 30 10))",
    "MULTIPOLYGON" -> "MULTIPOLYGON (((40 40, 20 45, 45 30, 40 40)),((20 35, 10 30, 10 10, 30 5, 45 20, 20 35),(30 20, 20 15, 20 25, 30 20)))"
  )

  case class CountryBean(@BeanProperty name: String, @BeanProperty geom: Geometry)
  case class PointBean(@BeanProperty geom: Geometry)

  private val r = new java.util.Random(42)
  private val n = 300000

  lazy val randomPoints: List[PointBean] = {
    val pointsRaw = {
      for (_ <- 0 to n) yield {
        val lat = (r.nextInt(180) - 90).toFloat + r.nextFloat()
        val lon = (r.nextInt(360) - 180).toFloat + r.nextFloat()
        s"POINT (${lon.formatted("%1.4f")} ${lat.formatted("%1.4f")})"
      }
    }
    pointsRaw.map{ wkt =>
      PointBean(WKTUtils.read(wkt))
    }.toList
  }

  lazy val countries: List[CountryBean] = {
    val countriesRaw: List[Feature] = {
      val featJson = new FeatureJSON()
      val is = getClass.getResourceAsStream("/countries.geojson")
      val fc = featJson.readFeatureCollection(is)
      val featIter = fc.features()
      val featurelist = new ListBuffer[Feature]()
      try {
        while (featIter.hasNext) {
          featurelist += featIter.next()
        }
      } finally {
        featIter.close()
      }
      featurelist.toList
    }
    countriesRaw.map { c =>
      val wkt = c.asInstanceOf[SimpleFeature].getDefaultGeometry.toString
      val name = c.getProperty("name").getValue.toString
      CountryBean(name, WKTUtils.read(wkt))
    }
  }

  def createDataFrame(geom: Geometry)(implicit spark: SparkSession): DataFrame = {
    val stable = spark
    import stable.implicits._
    Seq(geom).toDF("geom")
  }

  def createPointsTable(implicit spark: SparkSession): DataFrame = {
    val pointsDF = spark.createDataFrame(randomPoints)
    pointsDF.persist(MEMORY_ONLY)
    pointsDF.createOrReplaceTempView("points")
    pointsDF
  }

  def createCountriesTable(implicit spark: SparkSession): DataFrame = {
    val countriesDF = spark.createDataFrame(countries)
    countriesDF.createOrReplaceTempView("countries")
    countriesDF.persist(MEMORY_ONLY)
    countriesDF
  }


}
