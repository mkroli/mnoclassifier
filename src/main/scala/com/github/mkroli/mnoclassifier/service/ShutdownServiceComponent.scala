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

import java.lang.management.ManagementFactory

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import javax.management.ObjectName
import scala.language.implicitConversions

case class ShutdownHook[T](priority: Int, hook: () => Responder[T])

trait ShutdownMXBean {
  def shutdown
}

trait ShutdownServiceComponent {
  this: AkkaComponent =>

  lazy val shutdownServiceActor = actorSystem.actorOf(
    Props(new ShutdownServiceActor), name = "shutdownServiceActor")

  ManagementFactory.getPlatformMBeanServer().registerMBean(
    new ShutdownMXBean {
      override def shutdown = actorSystem.stop(shutdownServiceActor)
    },
    new ObjectName("%s:name=%s".format(
      classOf[ShutdownMXBean].getPackage.getName, "ShutdownService")))

  implicit def constToResponder[T](c: T) = Responder.constant(c)

  def addShutdownHook[T](priority: Int)(hook: => Responder[T]) =
    shutdownServiceActor ! ShutdownHook(priority, () => hook)

  class ShutdownServiceActor extends Actor {
    var shutdownHooks: List[ShutdownHook[_]] = Nil

    override def postStop {
      def processShutdownHooks(shutdownHooks: List[ShutdownHook[_]]) {
        shutdownHooks match {
          case head :: tail => head.hook().foreach(_ => processShutdownHooks(tail))
          case Nil => sys.exit
        }
      }
      processShutdownHooks(shutdownHooks.sortBy {
        case ShutdownHook(priority, _) => priority
      })
    }

    override def receive = {
      case hook @ ShutdownHook(_, _) => shutdownHooks = hook :: shutdownHooks
    }
  }
}
