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

import scala.collection.parallel.immutable.ParMap
import scala.language.postfixOps

trait Classifier[F, C] {
  def train(c: C, features: Seq[F]): Classifier[F, C]

  def test(features: Seq[F]): Seq[(C, Double)]

  val nearZero = BigDecimal(0.000001)
  val zero = BigDecimal(0)
}

class BayesClassifier[F, C](
  samples: BigDecimal,
  val classCount: ParMap[C, BigDecimal],
  features: BigDecimal,
  val featureCount: ParMap[C, (BigDecimal, ParMap[F, BigDecimal])],
  val fast: Boolean) extends Classifier[F, C] {
  def this() = this(0, ParMap(), 0, ParMap(), true)

  private def getOrElse[K, V](m: ParMap[K, V], k: K, e: V) = m get k orElse Some(e) get

  override def train(c: C, features: Seq[F]) = {
    require(features.size > 0)

    def incrMap[T](m: ParMap[T, BigDecimal], k: T) =
      m + (k -> (getOrElse(m, k, zero) + 1)) par

    val cc = incrMap[C](classCount, c)
    val fc = featureCount + (c -> features.foldRight(getOrElse(featureCount, c, (zero, ParMap[F, BigDecimal]()))) {
      case (f, (sum, fc)) =>
        ((sum + features.size), incrMap[F](fc, f))
    })
    new BayesClassifier(samples + 1, cc, this.features + features.size, fc.par, fast)
  }

  override def test(features: Seq[F]) = {
    features.flatMap { f =>
      featureCount.map {
        case (c, (sum, fl)) => c -> getOrElse(fl, f, nearZero) / sum
      }
    }.groupBy {
      case (c, _) => c
    }.map {
      case (c, l) =>
        val p_F_C = l.map { case (_, v) => v }.product
        val p_C = classCount(c) / samples
        val p_C_F = if (fast) {
          p_F_C * p_C
        } else {
          val fc = featureCount.foldRight(zero) {
            case ((_, (_, fc)), c) => c + features.map(getOrElse(fc, _, zero)).sum
          }
          val p_F = fc / this.features
          p_F_C * p_C / p_F
        }
        c -> (p_C_F).toDouble
    }.toList.sortBy {
      case (_, p) => -p
    }
  }
}
