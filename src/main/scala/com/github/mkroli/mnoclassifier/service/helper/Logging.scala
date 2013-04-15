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

import org.slf4j.LoggerFactory

trait Logging {
  val logger = LoggerFactory.getLogger(getClass)

  private def logWithObjects(l: (String, Array[Object]) => Unit)(msg: String, o: Any*) =
    l(msg, o.toArray.map(o => o.asInstanceOf[Object]))

  def trace(msg: String, o: Any*) = logWithObjects(logger.trace)(msg, o)

  def debug(msg: String, o: Any*) = logWithObjects(logger.debug)(msg, o)

  def info(msg: String, o: Any*) = logWithObjects(logger.info)(msg, o)

  def warn(msg: String) = logger.warn(msg)

  def warn(msg: String, t: Throwable) = logger.warn(msg, t)

  def error(msg: String) = logger.error(msg)

  def error(msg: String, t: Throwable) = logger.error(msg, t)
}
