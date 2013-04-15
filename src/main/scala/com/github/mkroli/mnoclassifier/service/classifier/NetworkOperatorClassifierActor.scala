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
package com.github.mkroli.mnoclassifier.service.classifier

import com.github.mkroli.mnoclassifier.classifier.BayesClassifier
import com.github.mkroli.mnoclassifier.classifier.FeatureExtraction
import com.github.mkroli.mnoclassifier.classifier.MarkovFeature
import com.github.mkroli.mnoclassifier.service.ReturnClassifier
import com.github.mkroli.mnoclassifier.service.SwitchClassifier
import com.github.mkroli.mnoclassifier.service.Train
import com.github.mkroli.mnoclassifier.service.helper.Logging
import com.github.mkroli.mnoclassifier.types.NetworkOperator
import com.github.mkroli.mnoclassifier.types.TelephoneNumber
import com.yammer.metrics.scala.Instrumented
import akka.actor.Actor
import akka.actor.actorRef2Scala
import com.github.mkroli.mnoclassifier.classifier.Classifier
import com.github.mkroli.mnoclassifier.service.SwitchClassifier

class NetworkOperatorClassifierActor extends Actor with Instrumented with Logging {
  var networkOperatorClassifier: Classifier[MarkovFeature, NetworkOperator] =
    new BayesClassifier[MarkovFeature, NetworkOperator]

  override def preRestart(reason: Throwable, message: Option[Any]) {
    self ! SwitchClassifier(networkOperatorClassifier)
  }

  override def receive = {
    case train @ Train(telephoneNumber, networkOperator) =>
      debug("training with {}", train)
      metrics.counter("classification.samples") += 1
      try {
        networkOperatorClassifier =
          networkOperatorClassifier.train(networkOperator,
            FeatureExtraction.markovFeatureExtraction(telephoneNumber))
      } catch {
        case t: Throwable => warn("training failed", t)
      }
      sender ! Unit
    case tn @ TelephoneNumber(_) =>
      debug("classifying {}", tn)
      metrics.counter("classification.classifications") += 1
      val result = networkOperatorClassifier.test(
        FeatureExtraction.markovFeatureExtraction(tn)).map {
          case (o, d) => (o -> BigDecimal(d))
        }
      sender ! result
    case SwitchClassifier(classifier) =>
      debug("switching classifier")
      networkOperatorClassifier = classifier.asInstanceOf[Classifier[MarkovFeature, NetworkOperator]]
    case ReturnClassifier => sender ! networkOperatorClassifier
  }
}
