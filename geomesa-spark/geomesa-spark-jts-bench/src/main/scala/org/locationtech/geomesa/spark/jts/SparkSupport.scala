/***********************************************************************
 * Copyright (c) 2018 Astraea, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.spark.jts

import org.apache.spark.sql.SparkSession

/**
 *
 *
 * @since 5/17/18
 */
trait SparkSupport {
  @transient
  lazy implicit val spark = SparkSession.builder
    .master("local[*]")
    .appName(getClass.getSimpleName)
    .config("spark.ui.enabled", false)
    .config("spark.ui.showConsoleProgress", false)
    .getOrCreate
    .withJTS
}
