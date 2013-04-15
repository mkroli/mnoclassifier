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

import com.github.mkroli.mnoclassifier.service.classifier.NetworkOperatorClassifierRouterActorComponent
import com.github.mkroli.mnoclassifier.service.classifier.NetworkOperatorClassifierSelfTestingActorComponent
import com.github.mkroli.mnoclassifier.service.helper.Logging
import com.github.mkroli.mnoclassifier.service.http.ClassificationPlanComponent
import com.github.mkroli.mnoclassifier.service.http.ConfigPlanComponent
import com.github.mkroli.mnoclassifier.service.http.HttpServiceComponent
import com.github.mkroli.mnoclassifier.service.http.MetricsPlanComponent
import com.github.mkroli.mnoclassifier.service.http.ResourcesPlanComponent
import com.github.mkroli.mnoclassifier.service.http.TrainingPlanComponent
import com.github.mkroli.mnoclassifier.service.monitoring.JvmMetricsComponent
import com.github.mkroli.mnoclassifier.service.monitoring.MetricsSampleActorComponent

object Boot extends App with Logging {
  val applicationContext = new Object with
    MetricsSampleActorComponent with
    JvmMetricsComponent with
    ConfigurationComponent with
    NetworkOperatorClassifierActorComponent with
    NetworkOperatorClassifierRouterActorComponent with
    NetworkOperatorClassifierSelfTestingActorComponent with
    HttpServiceComponent with
    ClassificationPlanComponent with
    TrainingPlanComponent with
    ConfigPlanComponent with
    MetricsPlanComponent with
    ResourcesPlanComponent with
    TrainingDataStoreActorComponent with
    ShutdownServiceComponent with
    AkkaComponent
  debug("application started at {}", System.currentTimeMillis)
  sys.addShutdownHook(debug("application stopped at {}", System.currentTimeMillis))
}
