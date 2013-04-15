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

import com.github.mkroli.mnoclassifier.classifier.Classifier
import com.github.mkroli.mnoclassifier.service.classifier.NetworkOperatorClassifierSelfTestingActorComponent
import com.github.mkroli.mnoclassifier.types.NetworkOperator
import com.github.mkroli.mnoclassifier.types.TelephoneNumber
import akka.actor.Props
import com.yammer.metrics.scala.Instrumented

case class Train(telephoneNumber: TelephoneNumber,
  networkOperator: NetworkOperator)
case class SwitchClassifier[F, C](classifier: Classifier[F, C])
case object ReturnClassifier

trait NetworkOperatorClassifierActorComponent extends Instrumented {
  this: ConfigurationComponent with AkkaComponent with NetworkOperatorClassifierSelfTestingActorComponent =>

  lazy val networkOperatorClassifierActor =
    actorSystem.actorOf(Props(new NetworkOperatorClassifierSelfTestingActor))
}
