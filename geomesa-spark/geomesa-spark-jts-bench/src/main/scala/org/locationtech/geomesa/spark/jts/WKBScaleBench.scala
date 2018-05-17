/***********************************************************************
 * Copyright (c) 2013-2018 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.spark.jts

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

@BenchmarkMode(Array(Mode.Throughput))
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MINUTES)
@Fork(1)
@Warmup(iterations = 1)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.MICROSECONDS)
class WKBScaleBench extends SparkSupport with BenchmarkDataSupport {
  @Setup(Level.Trial)
  def setupData(): Unit = {
    createPointsTable
    createCountriesTable
  }

  @Benchmark
  def jtsGroupPointsByCountries: Long = {
    println("Benchmark start")
    val res = spark.sql(
      """
        |select name, points.geom as p_geom
        |from points join countries on st_contains(countries.geom, points.geom)
      """.stripMargin)
    val count = res.count()
    println(s"Result: $count")
    count
  }
}
