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

import scala.Option.option2Iterable
import scala.concurrent.Future

import org.json4s.DefaultFormats
import org.json4s.jvalue2extractable
import org.json4s.native.JsonMethods.parse
import org.json4s.string2JsonInput

import com.github.mkroli.mnoclassifier.service.AkkaComponent
import com.github.mkroli.mnoclassifier.service.NetworkOperatorClassifierActorComponent
import com.github.mkroli.mnoclassifier.service.Train
import com.github.mkroli.mnoclassifier.service.helper.Logging
import com.github.mkroli.mnoclassifier.types.NetworkOperator
import com.github.mkroli.mnoclassifier.types.TelephoneNumber

import akka.pattern.ask
import unfiltered.kit.GZip
import unfiltered.netty.async.Planify
import unfiltered.request.Body
import unfiltered.request.POST
import unfiltered.request.Path
import unfiltered.request.RequestContentType
import unfiltered.request.Seg
import unfiltered.response.PlainTextContent
import unfiltered.response.ResponseString

trait TrainingPlanComponent extends Logging {
  self: AkkaComponent with NetworkOperatorClassifierActorComponent =>

  lazy val trainingPlan = Planify(GZip.async {
    case req @ POST(Path(Seg("api" :: "train" :: Nil))) => {
      val body = Body.string(req)
      val samples = req match {
        case RequestContentType(ct) if ct startsWith "application/json" =>
          implicit val formats = DefaultFormats
          parse(body).children
            .flatMap { child =>
              child.extractOpt[NetworkOperatorTelephoneNumberMapping] match {
                case Some(NetworkOperatorTelephoneNumberMapping(o, t)) =>
                  val train = Train(TelephoneNumber(t), NetworkOperator(o))
                  debug("Will train using {}", train)
                  Some(networkOperatorClassifierActor ? train)
                case None =>
                  info("Could not parse training data: {}", child)
                  None
              }
            }
        case _ =>
          val TrainingLine = """([^\t]*)\t(\d*)""".r
          body.split("""[\r\n]+""").toList.flatMap {
            case TrainingLine(o, t) =>
              val train = Train(TelephoneNumber(t), NetworkOperator(o))
              debug("Will train using {}", train)
              Some(networkOperatorClassifierActor ? train)
            case line =>
              info("Could not parse training data: {}", line)
              None
          }
      }
      def onCompletion(l: List[Future[_]]): Unit = l match {
        case Nil => req.respond(PlainTextContent ~>
          ResponseString("learned from %d samples".format(samples.size)))
        case f :: tail =>
          f.onComplete(_ => onCompletion(tail))
      }
      onCompletion(samples)
    }
  })
}
