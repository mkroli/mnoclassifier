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

import org.jboss.netty.handler.stream.ChunkedWriteHandler

import com.github.mkroli.mnoclassifier.service.ConfigurationComponent
import com.github.mkroli.mnoclassifier.service.ShutdownServiceComponent
import com.github.mkroli.mnoclassifier.service.helper.Logging

import unfiltered.netty.Http

case class NetworkOperatorTelephoneNumberMapping(
  networkOperator: String,
  telephoneNumber: String)

trait HttpServiceComponent extends Logging {
  this: ShutdownServiceComponent with
    ConfigurationComponent with
    ClassificationPlanComponent with
    TrainingPlanComponent with
    ConfigPlanComponent with
    MetricsPlanComponent with
    ResourcesPlanComponent =>

  val httpServer = Http(config.getConfig("http").getInt("port"))
    .chunked(1024 * 1024)
    .handler(metricsPushPlan.onPass(_.sendUpstream(_)))
    .plan(classificationPlan)
    .plan(trainingPlan)
    .plan(configPlan)
    .plan(metricsPlan)
    .plan(resourcesPlan)
    .makePlan(new ChunkedWriteHandler)
    .start()
  addShutdownHook(Int.MaxValue)(httpServer.stop)
}
