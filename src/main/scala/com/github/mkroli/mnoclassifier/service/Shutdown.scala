/*
 * MnoClassifier learns MSISDN-Operator combinations to afterwards predict Operators.
 * Copyright (C) 2013 MACH Connectivity GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.github.mkroli.mnoclassifier.service

import javax.management.remote.JMXServiceURL
import javax.management.remote.JMXConnectorFactory
import javax.management.ObjectName
import java.lang.management.ManagementFactory
import javax.management.JMX

object Shutdown extends App {
  val port = sys.props.getOrElse("jmx.port", "30750")
  val url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:%s/jmxrmi".format(port))

  try {
    val jmxc = JMXConnectorFactory.connect(url)
    val shutdownService = JMX.newMBeanProxy(
      jmxc.getMBeanServerConnection,
      new ObjectName("%s:name=%s".format(
        classOf[ShutdownMXBean].getPackage.getName, "ShutdownService")),
      classOf[ShutdownMXBean])
    shutdownService.shutdown
  } catch {
    case _: Throwable => sys.exit(1)
  }
}
