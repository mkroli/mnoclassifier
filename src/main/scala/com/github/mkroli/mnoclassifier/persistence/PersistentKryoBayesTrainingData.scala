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
package com.github.mkroli.mnoclassifier.persistence

import java.io.InputStream
import java.io.OutputStream

import com.github.mkroli.mnoclassifier.classifier.BayesClassifier
import com.github.mkroli.mnoclassifier.classifier.MarkovFeature
import com.github.mkroli.mnoclassifier.types.NetworkOperator
import com.twitter.chill.KryoInjection

import resource.managed

import scala.language.postfixOps

object PersistentKryoBayesTrainingData {
  def serialize(noc: BayesClassifier[MarkovFeature, NetworkOperator])(os: OutputStream) {
    managed(os).foreach(_.write(KryoInjection(noc)))
  }

  def deserialize(is: InputStream): BayesClassifier[MarkovFeature, NetworkOperator] = {
    managed(is).acquireAndGet { is =>
      val bytes = Stream.continually(is.read).takeWhile(-1 !=).map(_.toByte).toArray
      KryoInjection
        .invert(bytes)
        .map(_.asInstanceOf[BayesClassifier[MarkovFeature, NetworkOperator]])
        .getOrElse(new BayesClassifier)
    }
  }
}
