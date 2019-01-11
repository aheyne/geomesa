/***********************************************************************
 * Copyright (c) 2013-2019 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.fs.storage.api;

import org.opengis.feature.simple.SimpleFeatureType;

import java.util.Map;
import java.util.Optional;

public interface PartitionSchemeFactory {

    /**
     * Attempt to load a partition scheme
     *
     * @param name name of the scheme to load
     * @param sft simple feature type
     * @param options scheme options
     * @return partition scheme, if available
     */
    Optional<PartitionScheme> load(String name, SimpleFeatureType sft, Map<String, String> options);
}
