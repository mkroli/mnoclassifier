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

import scala.math.BigInt.int2bigInt
import com.github.mkroli.mnoclassifier.service.AkkaComponent
import com.github.mkroli.mnoclassifier.service.ConfigurationComponent
import com.github.mkroli.mnoclassifier.service.Train
import com.github.mkroli.mnoclassifier.service.helper.Logging
import com.github.mkroli.mnoclassifier.types.NetworkOperator
import com.yammer.metrics.scala.Instrumented
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.pattern.ask
import akka.pattern.pipe
import com.github.mkroli.mnoclassifier.service.monitoring.MetricsSampleActorComponent

trait NetworkOperatorClassifierSelfTestingActorComponent {
  self: AkkaComponent with ConfigurationComponent with NetworkOperatorClassifierRouterActorComponent with MetricsSampleActorComponent =>

  private val selfTestRatio = config.getConfig("classification").getDouble("selfTestRatio")
  private val selfTestSamples = config.getConfig("classification").getLong("selfTestSamples")

  class NetworkOperatorClassifierSelfTestingActor extends Actor with Instrumented with Logging {
    val child = context.actorOf(Props(
      new NetworkOperatorClassifierRouterActor),
      "networkOperatorClassifierActor")

    val blockSize = {
      if (selfTestRatio == 0.0) BigInt(-1)
      else BigInt((selfTestSamples / selfTestRatio).toLong)
    }
    def countInRatio(trainingMessageCount: BigInt) =
      selfTestRatio > 0.0 &&
        trainingMessageCount > selfTestSamples &&
        ((trainingMessageCount % blockSize) / selfTestSamples).toInt == 0

    var trainingMessageCount = BigInt(0)
    var testList: List[(Train, Boolean, ActorRef)] = Nil
    var successRate = Double.NaN

    metrics.gauge("classification.latestHits") {
      successRate
    }

    def updateSuccessRate(s: Double) = {
      successRate = s
      metricsSampleActor ! Sample("classification.successRate", successRate)
    }

    override def receive = {
      case msg @ Train(telephoneNumber, networkOperator) =>
        trainingMessageCount = trainingMessageCount + 1
        if (countInRatio(trainingMessageCount)) {
          val s = sender
          (child ? telephoneNumber).mapTo[Seq[(NetworkOperator, BigDecimal)]].map {
            case result: Seq[(NetworkOperator, BigDecimal)] if !result.isEmpty =>
              val success = (result.head match { case (n, _) => n }) == networkOperator
              (msg, success, s)
            case Nil => Unit
          } pipeTo self
        } else {
          child.forward(msg)
        }
      case (msg @ Train(_, _), success: Boolean, s: ActorRef) => {
        testList = (msg, success, s) :: testList
        if (testList.size == selfTestSamples) {
          val successCount = testList.filter {
            case (_, b, _) => b
          }.size
          updateSuccessRate(successCount.toDouble / selfTestSamples.toDouble)
          testList.foreach {
            case (t, _, s) => child.tell(t, s)
          }
          testList = Nil
        }
      }
      case msg => child.forward(msg)
    }
  }
}
