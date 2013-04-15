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
package com.github.mkroli.mnoclassifier.service.http

import scala.collection.GenSeq
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

import org.json4s.DefaultFormats
import org.json4s.JObject
import org.json4s.JString
import org.json4s.JsonDSL._
import org.json4s.native.Serialization

import com.github.mkroli.mnoclassifier.service.AkkaComponent
import com.github.mkroli.mnoclassifier.service.helper.MetricsHelper
import com.github.mkroli.mnoclassifier.service.http.websocket.WebSocketActor
import com.github.mkroli.mnoclassifier.service.monitoring.MetricsSampleActorComponent

import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.util.Timeout
import unfiltered.kit.GZip
import unfiltered.netty.async.Planify
import unfiltered.netty.websockets
import unfiltered.request.GET
import unfiltered.request.Path
import unfiltered.request.Seg
import unfiltered.response.JsonContent
import unfiltered.response.NotFound
import unfiltered.response.ResponseString

trait MetricsPlanComponent extends MetricsHelper {
  self: MetricsSampleActorComponent with AkkaComponent =>

  private def currentMetricsToJson() = currentMetrics().map {
    case (name, _, value) =>
      ("name" -> name) ~ ("value" -> metricValueToJsonValue(value))
  }

  private def currentMetricsToJsonString() =
    Serialization.write(currentMetricsToJson())(DefaultFormats)

  private def metricsHistoryToJson(history: Map[String, GenSeq[Option[(Long, Option[Any])]]]) = {
    Serialization.write(history.map {
      case (name, history) =>
        val (dates, values) = history.map {
          case Some((date, value)) => date -> value
          case None => 0L -> None
        }.unzip
        name -> (("dates" -> dates.toList) ~
          ("values" -> values.map(metricValueToJsonValue).toList))
    })(DefaultFormats)
  }

  lazy val metricsPlan = Planify(GZip.async {
    case req @ GET(Path(Seg("api" :: "metrics" :: Nil))) => {
      req.respond(JsonContent ~> ResponseString(currentMetricsToJsonString()))
    }
    case req @ GET(Path(Seg("api" :: "metrics" :: metric :: Nil))) => {
      currentMetricsToJson().find {
        case JObject(children) => children.exists {
          case ("name", JString(name)) => name == metric
          case _ => false
        }
      } match {
        case Some(json) => req.respond(JsonContent ~>
          ResponseString(Serialization.write(json)(DefaultFormats)))
        case None => req.respond(NotFound)
      }
    }
    case req @ GET(Path(Seg("api" :: "metricsHistory" :: Nil))) => {
      (metricsSampleActor ? GetMetricsHistory).onSuccess {
        case HistoryMessage(history) => req.respond(JsonContent ~>
          ResponseString(metricsHistoryToJson(history)))
      }
    }
  })

  lazy val metricsPushPlan = {
    class MetricsActor extends WebSocketActor {
      case object Tick

      context.system.scheduler.schedule(0 seconds, 1 second)(self ! Tick)

      override def receive = withSubscriptions {
        case Tick => publish(currentMetricsToJsonString())
      }
    }

    class MetricsHistoryActor extends WebSocketActor {
      override def preStart {
        metricsSampleActor ! Subscribe(self)
      }

      override def receive = withSubscriptions {
        case HistoryMessage(history) => publish(metricsHistoryToJson(history))
      }
    }

    implicit val timeout = Timeout(10 seconds)
    websockets.Planify {
      case Path(Seg("wsapi" :: "metrics" :: Nil)) =>
        WebSocketActor(actorSystem.actorOf(Props(new MetricsActor)))
      case Path(Seg("wsapi" :: "metricsHistory" :: Nil)) =>
        WebSocketActor(actorSystem.actorOf(Props(new MetricsHistoryActor)))
    }
  }
}
