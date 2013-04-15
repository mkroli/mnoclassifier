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

import org.json4s.DefaultFormats
import org.json4s.JsonDSL.bigdecimal2jvalue
import org.json4s.JsonDSL.pair2Assoc
import org.json4s.JsonDSL.seq2jvalue
import org.json4s.JsonDSL.string2jvalue
import org.json4s.native.Serialization

import com.github.mkroli.mnoclassifier.service.AkkaComponent
import com.github.mkroli.mnoclassifier.service.NetworkOperatorClassifierActorComponent
import com.github.mkroli.mnoclassifier.service.helper.Logging
import com.github.mkroli.mnoclassifier.types.NetworkOperator
import com.github.mkroli.mnoclassifier.types.TelephoneNumberParser

import akka.pattern.ask
import unfiltered.kit.GZip
import unfiltered.netty.async.Planify
import unfiltered.request.Accepts
import unfiltered.request.GET
import unfiltered.request.Path
import unfiltered.request.Seg
import unfiltered.response.JsonContent
import unfiltered.response.PlainTextContent
import unfiltered.response.ResponseString

trait ClassificationPlanComponent extends Logging {
  self: AkkaComponent with NetworkOperatorClassifierActorComponent =>

  lazy val classificationPlan = Planify(GZip.async {
    case req @ GET(Path(Seg("api" :: TelephoneNumberParser(tn) :: Nil))) => {
      debug("classifying {}", tn)
      (networkOperatorClassifierActor ? tn)
        .mapTo[List[(NetworkOperator, BigDecimal)]].onSuccess {
          case l: List[(NetworkOperator, BigDecimal)] =>
            val top50 = l.take(50)
            req.respond(req match {
              case Accepts.Json(_) => {
                val jsonResults = top50.map {
                  case (NetworkOperator(no), r) => ("networkOperator" -> no) ~ ("relevance" -> r)
                }
                val confidence = {
                  val relevances = l.map {
                    case (_, relevance) => relevance
                  }
                  if (l.size < 2) BigDecimal(0) else relevances.head / relevances.sum
                }
                val json = ("results" -> jsonResults) ~ ("confidence" -> confidence)
                JsonContent ~> ResponseString(Serialization.write(json)(DefaultFormats))
              }
              case _ => {
                PlainTextContent ~> ResponseString(top50.map {
                  case (NetworkOperator(no), r) => "%s\t%s".format(no, r.toDouble)
                }.mkString("\n"))
              }
            })
        }
    }
  })
}
