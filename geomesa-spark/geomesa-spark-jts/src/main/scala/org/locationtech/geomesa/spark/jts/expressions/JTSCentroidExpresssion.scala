/***********************************************************************
 * Copyright (c) 2018 Astraea, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/


package org.locationtech.geomesa.spark.jts.expressions
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.codegen.CodegenFallback
import org.apache.spark.sql.catalyst.expressions.{Expression, UnaryExpression}
import org.apache.spark.sql.types._



/**
 *
 *
 * @since 5/17/18
 */
case class JTSCentroidExpresssion(child: Expression) extends UnaryExpression with CodegenFallback {
  override def dataType: DataType = ???

  override def eval(input: InternalRow): TraversableOnce[InternalRow] = {
    ???
  }
}
