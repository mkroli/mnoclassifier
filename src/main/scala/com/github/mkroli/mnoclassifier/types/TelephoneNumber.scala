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
package com.github.mkroli.mnoclassifier.types

case class TelephoneNumber(vec: Seq[Int]) {
  vec.foreach(digit => require(digit >= 0 && digit < 10))

  def this(s: String) = this(s.map(c => (c - '0').toInt))
}

case object TelephoneNumber {
  def apply(s: String) = new TelephoneNumber(s)
}

object TelephoneNumberParser {
  def unapply(s: String) = {
    try {
      Some(TelephoneNumber(s))
    } catch {
      case _: Throwable => None
    }
  }
}
