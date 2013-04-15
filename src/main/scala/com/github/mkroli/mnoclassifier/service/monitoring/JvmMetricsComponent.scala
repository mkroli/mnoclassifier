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
package com.github.mkroli.mnoclassifier.service.monitoring

import com.yammer.metrics.scala.Instrumented

trait JvmMetricsComponent extends Instrumented {
  val startTime = System.currentTimeMillis

  metrics.gauge("java.memory.heap.used") {
    Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory
  }

  metrics.gauge("java.memory.heap.max") {
    Runtime.getRuntime.maxMemory
  }

  metrics.gauge("java.uptime") {
    System.currentTimeMillis - startTime
  }
}
