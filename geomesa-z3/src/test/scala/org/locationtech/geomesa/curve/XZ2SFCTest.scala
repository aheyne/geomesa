/*
 * Copyright (c) 2013-2016 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0 which
 * accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 */

package org.locationtech.geomesa.curve

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class XZ2SFCTest extends Specification {

  val sfc = new XZ2SFC(6)

  "XZ2" should {
    "index polygons and query them" >> {
      val poly = sfc.index(10, 10, 12, 12)

      val containing = Seq(
        (9.0, 9.0, 13.0, 13.0),
        (-180.0, -90.0, 180.0, 90.0),
        (0.0, 0.0, 180.0, 90.0),
        (0.0, 0.0, 20.0, 20.0)
      )
      val overlapping = Seq(
        (11.0, 11.0, 13.0, 13.0),
        (9.0, 9.0, 11.0, 11.0),
        (10.5, 10.5, 11.5, 11.5),
        (11.0, 11.0, 11.0, 11.0)
      )
      forall(containing ++ overlapping) { bbox =>
        val ranges = sfc.ranges(Seq(bbox)).map(r => (r.lower, r.upper))
        val matches = ranges.exists(r => r._1 <= poly && r._2 >= poly)
        if (!matches) {
          println(s"$bbox - no match")
        }
        matches must beTrue
      }
    }

    "index points and query them" >> {
      val poly = sfc.index(11, 11, 11, 11)

      val containing = Seq(
        (9.0, 9.0, 13.0, 13.0),
        (-180.0, -90.0, 180.0, 90.0),
        (0.0, 0.0, 180.0, 90.0),
        (0.0, 0.0, 20.0, 20.0)
      )
      val overlapping = Seq(
        (11.0, 11.0, 13.0, 13.0),
        (9.0, 9.0, 11.0, 11.0),
        (10.5, 10.5, 11.5, 11.5),
        (11.0, 11.0, 11.0, 11.0)
      )
      forall(containing ++ overlapping) { bbox =>
        val ranges = sfc.ranges(Seq(bbox)).map(r => (r.lower, r.upper))
        val matches = ranges.exists(r => r._1 <= poly && r._2 >= poly)
        if (!matches) {
          println(s"$bbox - no match")
        }
        matches must beTrue
      }
    }

    "index complex features and query them2" >> {
      // geometries taken from accumulo FilterTest
      val r = """\((\d+\.\d*),(\d+\.\d*),(\d+\.\d*),(\d+\.\d*)\)""".r
      val source = Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("geoms.list"))
      val geoms = try {
        source.getLines.toArray.flatMap { l =>
          r.findFirstMatchIn(l).map { m =>
            (m.group(1).toDouble, m.group(2).toDouble, m.group(3).toDouble, m.group(4).toDouble)
          }
        }
      } finally {
        source.close()
      }

      val ranges = sfc.ranges(Seq((45.0, 23.0, 48.0, 27.0)))
      forall(geoms) { geom =>
        val index = sfc.index(geom)
        val matches = ranges.exists(r => r.lower <= index && r.upper >= index)
        if (!matches) {
          println(s"$p - invalid match")
        }
        matches must beTrue
      }
    }
  }
}