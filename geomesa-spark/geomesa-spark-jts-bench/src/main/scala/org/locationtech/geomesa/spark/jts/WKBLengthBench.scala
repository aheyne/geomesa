/***********************************************************************
 * Copyright (c) 2018 Astraea, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/


package org.locationtech.geomesa.spark.jts

import java.util.concurrent.TimeUnit

import org.locationtech.geomesa.spark.jts.util.WKBUtils.WKBData
import org.locationtech.geomesa.spark.jts.util._
import org.openjdk.jmh.annotations._

@BenchmarkMode(Array(Mode.Throughput))
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
@Warmup(iterations = 3)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
class WKBLengthBench extends BenchmarkDataSupport  {

  @Param(Array("POINT", "LINESTRING", "POLYGON_1", "POLYGON_2", "MULTIPOINT", "MULTILINESTRING", "MULTIPOLYGON"))
  var testCase: String = _

  @transient
  var wkb: WKBData = _

  @Setup(Level.Trial)
  def setupData(): Unit = {
    val wkt = wktExamples(testCase)
    val geom = WKTUtils.read(wkt)
    wkb = WKBUtils.write(geom)
  }

  @Benchmark
  def jtsLength: Double = {
    val geom = WKBUtils.read(wkb)
    geom.getLength
  }

  @Benchmark
  def gmLength: Double = {
    val geom = GMWKBUtils.read(wkb)
    geom.getLength
  }
}


