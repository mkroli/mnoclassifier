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
package com.github.mkroli.mnoclassifier.service.helper

import scala.collection.JavaConversions
import scala.math.BigDecimal.double2bigDecimal
import scala.math.BigInt.int2bigInt
import scala.math.BigInt.long2bigInt

import org.json4s.JDecimal
import org.json4s.JInt
import org.json4s.JNull
import org.json4s.JString

import com.yammer.metrics.Metrics
import com.yammer.metrics.core.Counter
import com.yammer.metrics.core.Gauge

trait MetricsHelper {
  def currentMetrics() = {
    JavaConversions.mapAsScalaMap(Metrics.defaultRegistry.allMetrics).map {
      case (name, metric) => (name.getName, System.currentTimeMillis,
        (metric match {
          case counter: Counter => Some(counter.count)
          case gauge: Gauge[_] => Some(gauge.value)
          case _ => None
        }))
    }
  }

  def metricValueToJsonValue(value: Option[Any]) = value match {
    case Some(value) => value match {
      case i: Int => JInt(i)
      case l: Long => JInt(l)
      case i: BigInt => JInt(i)
      case f: Float => JDecimal(f)
      case d: Double if d.isNaN => JNull
      case d: Double => JDecimal(d)
      case d: BigDecimal => JDecimal(d)
      case value => JString(value.toString)
    }
    case None => JNull
  }
}
