/***********************************************************************
 * Copyright (c) 2013-2017 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.accumulo.index.legacy.attribute

import org.locationtech.geomesa.accumulo.index.AccumuloAttributeIndex
import org.locationtech.geomesa.index.utils.SplitArrays
import org.opengis.feature.simple.SimpleFeatureType

// no shards
case object AttributeIndexV4 extends AccumuloAttributeIndex {

  override val version: Int = 4

  override protected def getShards(sft: SimpleFeatureType): IndexedSeq[Array[Byte]] = SplitArrays.EmptySplits
}
