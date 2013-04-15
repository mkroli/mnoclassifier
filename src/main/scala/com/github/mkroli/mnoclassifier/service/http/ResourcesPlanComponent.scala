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

import org.jboss.netty.handler.codec.http.HttpResponse
import unfiltered.Async
import unfiltered.kit.GZip
import unfiltered.netty.ReceivedMessage
import unfiltered.netty.Resources
import unfiltered.netty.async.Planify
import unfiltered.request.Seg
import unfiltered.response.Pass
import unfiltered.response.ResponseFunction
import unfiltered.request.DelegatingRequest

trait ResourcesPlanComponent {
  lazy val resourcesPlan = {
    val resources = new Resources(
      getClass.getResource("/com/github/mkroli/mnoclassifier/static/"),
      cacheSeconds = 3600)

    def rewrite(rules: PartialFunction[String, String]) = {
      Async.Intent[ReceivedMessage, HttpResponse] {
        case req => resources.intent.lift(new DelegatingRequest(req) with Async.Responder[HttpResponse] {
          override def uri = rules.lift(req.uri).getOrElse(req.uri)
          override def respond(rf: ResponseFunction[HttpResponse]) = req.respond(rf)
        }).getOrElse(Pass)
      }
    }

    Planify(GZip.async(rewrite {
      case Seg(
        Nil |
        "classification" :: Nil |
        "classification" :: _ :: Nil |
        "training" :: Nil |
        "monitoring" :: Nil |
        "configuration" :: Nil) => "/index.html"
      case Seg("presentation" :: Nil) => "/presentation/index.html"
    }))
  }
}
