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

import java.io.File
import scala.concurrent.duration.DurationLong
import com.github.mkroli.mnoclassifier.classifier.BayesClassifier
import com.github.mkroli.mnoclassifier.classifier.Feature
import com.github.mkroli.mnoclassifier.persistence.Compression
import com.github.mkroli.mnoclassifier.persistence.PersistentKryoBayesTrainingData
import com.github.mkroli.mnoclassifier.persistence.PersistentTrainingDataFilesystemStore
import com.github.mkroli.mnoclassifier.service.helper.Logging
import com.github.mkroli.mnoclassifier.types.NetworkOperator
import com.yammer.metrics.scala.Instrumented
import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import com.github.mkroli.mnoclassifier.classifier.MarkovFeature
import scala.language.implicitConversions
import scala.language.postfixOps

case class Load(filename: String)
case class Save[C <: BayesClassifier[MarkovFeature, NetworkOperator]](
  classifier: C,
  filename: String,
  expired: () => Boolean)
case object Saved
case class Error(t: Throwable)

trait TrainingDataStoreActorComponent {
  this: NetworkOperatorClassifierActorComponent with ConfigurationComponent with ShutdownServiceComponent with AkkaComponent =>

  private val filename = new File(datadir, config.getConfig("classification").getString("dataFile")).getAbsolutePath()

  lazy val trainingDataStoreActor = actorSystem.actorOf(Props(new TrainingDataStoreActor))

  def saveClassifier(): Responder[Any] =
    saveClassifier(None)
  def saveClassifier(expireIn: Long): Responder[Any] =
    saveClassifier(Some(System.currentTimeMillis + expireIn))
  private def saveClassifier(expireAt: Option[Long]): Responder[Any] = {
    val future = (networkOperatorClassifierActor ? ReturnClassifier)
      .mapTo[BayesClassifier[MarkovFeature, NetworkOperator]].map {
        case classifier: BayesClassifier[MarkovFeature, NetworkOperator] =>
          trainingDataStoreActor ? Save(classifier, filename, () => {
            expireAt match {
              case Some(expireAt) => System.currentTimeMillis >= expireAt
              case None => false
            }
          })
      }.flatMap(f => f)
    new Responder[Any] {
      override def respond(k: (Any) => Unit) = future onComplete k
    }
  }

  trainingDataStoreActor.foreach { trainingDataStoreActor =>
    implicit def functionToRunnable[A](f: => A) = new Runnable {
      override def run() = f
    }

    trainingDataStoreActor ! Load(filename)

    actorSystem.scheduler.schedule(
      config.getConfig("classification").getLong("autosaveInterval") milliseconds,
      config.getConfig("classification").getLong("autosaveInterval") milliseconds)(
        saveClassifier(config.getConfig("classification").getLong("autosaveInterval")))
    addShutdownHook(Int.MinValue)(saveClassifier)
  }

  class TrainingDataStoreActor extends Actor with Instrumented with Logging {
    def bayesFileStore(file: File) = new PersistentTrainingDataFilesystemStore(file) with Compression

    override def receive = {
      case Load(filename) =>
        try {
          info("loading {}", filename)
          val classifier = bayesFileStore(new File(filename))
            .load(PersistentKryoBayesTrainingData.deserialize)
          metrics.gauge("classification.storage.filename")(filename)
          networkOperatorClassifierActor ! SwitchClassifier(classifier)
          info("successfully loaded {}", filename)
        } catch {
          case t: Throwable =>
            warn("failed loading %s".format(filename), t)
            sender ! Error(t)
        }
      case Save(classifier: BayesClassifier[MarkovFeature, NetworkOperator], filename, expired) if !expired() =>
        try {
          info("saving {}", filename)
          val file = new File(filename)
          file.getParentFile().mkdirs()
          bayesFileStore(file).store(PersistentKryoBayesTrainingData.serialize(classifier))
          sender ! Saved
          info("successfully saved {}", filename)
        } catch {
          case t: Throwable =>
            warn("failed saving %s".format(filename), t)
            sender ! Error(t)
        }
      case Save(_, _, _) => sender ! Error(new RuntimeException("Expired"))
    }
  }
}
