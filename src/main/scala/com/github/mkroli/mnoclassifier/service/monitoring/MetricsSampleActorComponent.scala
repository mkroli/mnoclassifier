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
package com.github.mkroli.mnoclassifier.service.monitoring

import scala.collection.GenSeq
import scala.concurrent.duration.DurationInt
import scala.reflect.ClassTag
import scala.language.postfixOps

import com.github.mkroli.mnoclassifier.service.AkkaComponent
import com.github.mkroli.mnoclassifier.service.helper.MetricsHelper

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala

case class Ring[T] private (size: Int, r: Vector[Option[T]], ptr: Int) {
  def put(elem: T) = new Ring(
    size,
    r.updated(ptr, Some(elem)),
    (ptr + 1) % size)

  def apply(): GenSeq[Option[T]] = (0 until r.length).map(i => r((ptr + i) % r.length))
}

case object Ring {
  def apply[T: ClassTag](size: Int) = new Ring[T](size, Vector.fill[Option[T]](size)(None), 0)
}

trait MetricsSampleActorComponent {
  self: AkkaComponent =>

  case class Subscribe(actor: ActorRef)
  case class UnSubscribe(actor: ActorRef)

  case class HistoryMessage(history: Map[String, GenSeq[Option[(Long, Option[Any])]]])
  case object HistoryMessage {
    def fromCurrent(history: Map[String, Ring[(Long, Option[Any])]]) = {
      new HistoryMessage(history.map {
        case (name, ring) => name -> ring()
      })
    }
  }

  case object GetMetricsHistory

  val metricsSampleActorMetricNames = Set(
    "java.memory.heap.used",
    "latestHits",
    "classification.samples",
    "classification.classifications")

  val metricsSampleActor = actorSystem.actorOf(Props(new MetricsSampleActor))

  case class Sample(name: String, value: Any)

  class MetricsSampleActor extends Actor with MetricsHelper {
    case object Tick

    context.system.scheduler.schedule(0 seconds, 5 seconds)(self ! Tick)

    var history = Map[String, Ring[(Long, Option[Any])]]()
      .withDefaultValue(Ring[(Long, Option[Any])](60))

    var subscriptions = Set[ActorRef]()

    override def receive = {
      case Tick => {
        history = currentMetrics().filter {
          case (name, _, _) => metricsSampleActorMetricNames.contains(name)
        }.foldLeft(history) {
          case (history, (name, date, value)) =>
            history + (name -> history(name).put(date, value))
        }
        val hm = HistoryMessage.fromCurrent(history)
        subscriptions.foreach(_ ! hm)
      }
      case Sample(name, value) => {
        val ring = history.getOrElse(name, Ring[(Long, Option[Any])](50))
          .put(System.currentTimeMillis, Some(value))
        history = history + (name -> ring)
      }
      case GetMetricsHistory => sender ! HistoryMessage.fromCurrent(history)
      case Subscribe(actor) => subscriptions = subscriptions + actor
      case UnSubscribe(actor) => subscriptions = subscriptions - actor
    }
  }
}
