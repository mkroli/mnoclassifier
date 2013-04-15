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

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

import com.github.mkroli.mnoclassifier.classifier.Classifier
import com.github.mkroli.mnoclassifier.service.AkkaComponent
import com.github.mkroli.mnoclassifier.service.ConfigurationComponent
import com.github.mkroli.mnoclassifier.service.ReturnClassifier
import com.github.mkroli.mnoclassifier.service.SwitchClassifier
import com.github.mkroli.mnoclassifier.service.Train
import com.github.mkroli.mnoclassifier.types.TelephoneNumber

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.FSM
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.routing.Broadcast
import akka.routing.RoundRobinRouter

import router._

package router {
  sealed trait State
  case object Committed extends State
  case object InCommit extends State
  case object UnCommitted extends State

  sealed case class FSMData(
    trainingData: List[(ActorRef, Train)] = Nil,
    classifications: List[(ActorRef, TelephoneNumber)] = Nil)
}

trait NetworkOperatorClassifierRouterActorComponent {
  self: ConfigurationComponent with AkkaComponent =>

  class NetworkOperatorClassifierRouterActor extends Actor with FSM[State, FSMData] {
    val trainingActor = context.actorOf(
      Props(new NetworkOperatorClassifierActor),
      "trainingActor")
    val router = context.actorOf(Props(new NetworkOperatorClassifierActor)
      .withRouter(RoundRobinRouter(config.getConfig("classification").getInt("actors"))),
      "networkOperatorClassifierRouterActor")

    startWith(Committed, FSMData())

    when(Committed) {
      case Event(train @ Train(_, _), data) =>
        goto(UnCommitted) using data.copy(
          classifications = Nil,
          trainingData = sender -> train :: data.trainingData)
      case Event(tn @ TelephoneNumber(_), data) =>
        router forward tn
        stay using data.copy(classifications = Nil)
    }

    when(InCommit) {
      case Event(train @ Train(_, _), data) =>
        goto(UnCommitted) using data.copy(trainingData = sender -> train :: data.trainingData)
      case Event(tn @ TelephoneNumber(_), data) =>
        stay using data.copy(classifications = sender -> tn :: data.classifications)
      case Event(classifier: Classifier[_, _], data) =>
        router ! Broadcast(SwitchClassifier(classifier))
        goto(Committed)
    }

    when(UnCommitted, stateTimeout = 10 seconds) {
      case Event(StateTimeout, data) =>
        goto(InCommit) using data.copy(trainingData = Nil)
      case Event(train @ Train(_, _), data) =>
        trainingActor forward train
        stay using data.copy(trainingData = Nil)
      case Event(tn @ TelephoneNumber(_), data) =>
        goto(InCommit) using data.copy(
          classifications = sender -> tn :: data.classifications,
          trainingData = Nil)
      case Event(classifier: Classifier[_, _], data) =>
        /* 
         * Do a (mini) commit and classify from data but stay uncommitted.
         * This is to prevent single classifications in a big training queue
         * from never being processed. However the classifications are done
         * using old data.
         */
        router ! Broadcast(SwitchClassifier(classifier))
        data.classifications.foreach {
          case (s, tn) => router.tell(tn, s)
        }
        stay using data.copy(classifications = Nil)
    }

    onTransition {
      case UnCommitted -> InCommit => {
        trainingActor ? ReturnClassifier map {
          case classifier: Classifier[_, _] =>
            self ! classifier
        }
      }
      case _ -> UnCommitted => {
        nextStateData.trainingData.foreach {
          case (s, train) => trainingActor.tell(train, sender)
        }
      }
      case _ -> Committed => {
        nextStateData.classifications.foreach {
          case (s, tn) => router.tell(tn, s)
        }
      }
    }

    whenUnhandled {
      case Event(rc @ ReturnClassifier, _) =>
        trainingActor forward rc
        stay
      case Event(msg, _) =>
        trainingActor forward msg
        router ! Broadcast(msg)
        stay
    }

    initialize
  }
}
