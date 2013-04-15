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

import scala.annotation.migration
import scala.collection.JavaConversions.asScalaSet

import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.json4s.string2JsonInput

import com.github.mkroli.mnoclassifier.service.ConfigurationComponent
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigValueType

import unfiltered.kit.GZip
import unfiltered.netty.async.Planify
import unfiltered.request.GET
import unfiltered.request.Path
import unfiltered.request.Seg
import unfiltered.response.JsonContent
import unfiltered.response.ResponseString

trait ConfigPlanComponent {
  self: ConfigurationComponent =>

  lazy val configPlan = Planify(GZip.async {
    case req @ GET(Path(Seg("api" :: "config" :: Nil))) => {

      val renderOptions = ConfigRenderOptions.concise
      val configData = config.entrySet.map { e =>
        val (key, value) = (e.getKey, e.getValue)
        key -> (value.valueType match {
          case ConfigValueType.LIST | ConfigValueType.OBJECT => parse(value.render(renderOptions))
          case ConfigValueType.STRING => value.unwrapped
          case _ => value.render(renderOptions)
        })
      }.toMap
      val data = Serialization.writePretty(configData)(DefaultFormats)
      req.respond(JsonContent ~> ResponseString(data))
    }
  })
}
