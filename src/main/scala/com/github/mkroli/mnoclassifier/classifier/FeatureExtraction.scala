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
package com.github.mkroli.mnoclassifier.classifier

import com.github.mkroli.mnoclassifier.types.TelephoneNumber

abstract class Feature

case class SimpleFeature(position: Int, digit: Int) extends Feature

case class MarkovFeature(position: Int, digit1: Int, digit2: Int) extends Feature

object FeatureExtraction {
  def simpleFeatureExtraction(tn: TelephoneNumber) =
    (0 until tn.vec.length).map { i => SimpleFeature(i, tn.vec(i)) }

  def markovFeatureExtraction(tn: TelephoneNumber) =
    (1 until tn.vec.length).map { i => MarkovFeature(i, tn.vec(i - 1), tn.vec(i)) }
}
