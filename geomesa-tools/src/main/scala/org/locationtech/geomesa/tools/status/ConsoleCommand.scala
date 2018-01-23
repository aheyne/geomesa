/***********************************************************************
 * Copyright (c) 2013-2018 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.tools.status

import com.beust.jcommander.Parameters
import org.locationtech.geomesa.tools.Command

/**
 * Note: this class is a placeholder for the 'console' function implemented in the 'geomesa-*' script, to get it
 * to show up in the JCommander help
 */
class ConsoleCommand extends Command {

  override val name = "console"
  override val params = new ConsoleParameters
  override def execute(): Unit = {}
}

@Parameters(commandDescription = "Run a Scala REPL with the GeoMesa classpath and configuration loaded.")
class ConsoleParameters {}
