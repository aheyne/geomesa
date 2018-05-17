/***********************************************************************
 * Copyright (c) 2018 Astraea, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/


package org.locationtech.geomesa.spark.jts

import java.util.concurrent.TimeUnit

import com.vividsolutions.jts.geom.Point
import org.apache.spark
import org.apache.spark.sql
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.locationtech.geomesa.spark.jts.udf.SpatialRelationFunctions
import org.locationtech.geomesa.spark.jts.util.WKBUtils.WKBData
import org.locationtech.geomesa.spark.jts.util._
import org.locationtech.geomesa.spark.jts._
import org.openjdk.jmh.annotations._

@BenchmarkMode(Array(Mode.Throughput))
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
@Warmup(iterations = 3)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
class WKBCentroidBench extends SparkSupport with BenchmarkDataSupport  {

  @Param(Array("POINT", "LINESTRING", "POLYGON_1", "POLYGON_2", "MULTIPOINT", "MULTILINESTRING", "MULTIPOLYGON"))
  var testCase: String = _

  @transient
  var wkb: WKBData = _

  @transient
  var df: DataFrame = _

  import spark.implicits._

  def udfImpl = org.apache.spark.sql.functions.udf(SpatialRelationFunctions.ST_Centroid _)

  //def expressionImpl =

  @Setup(Level.Trial)
  def setupData(): Unit = {
    val wkt = wktExamples(testCase)
    val geom = WKTUtils.read(wkt)
    wkb = WKBUtils.write(geom)
    df = createDataFrame(geom)
  }

  @Benchmark
  def jtsCentroid: Point = {
    val geom = WKBUtils.read(wkb)
    geom.getCentroid
  }

  @Benchmark
  def gmCentroid: Point = {
    val geom = WKBUtils.read(wkb)
    geom.getCentroid
  }

  @Benchmark
  def udfCentroid: Point = {
    df.select(udfImpl($"geom")).as[Point].first
  }

//  @Benchmark
//  def expressionCentroid: Point = {
//
//  }

}


