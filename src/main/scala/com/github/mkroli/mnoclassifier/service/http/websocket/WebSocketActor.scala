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
package com.github.mkroli.mnoclassifier.service.http.websocket

import scala.concurrent.ExecutionContext

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.util.Timeout
import unfiltered.netty.websockets.Close
import unfiltered.netty.websockets.Open
import unfiltered.netty.websockets.SocketCallback
import unfiltered.netty.websockets.WebSocket

trait WebSocketActor extends Actor {
  private var sockets = Set[WebSocket]()

  private type Handler = PartialFunction[Any, Unit]

  def withSubscriptions(receive: Handler): Handler = receive.orElse {
    case Open(socket) => sockets = sockets + socket
    case Close(socket) => sockets = sockets - socket
  }

  def publish(msg: String) = sockets.foreach(_.send(msg))
}

object WebSocketActor {
  def apply(actor: ActorRef)(implicit executor: ExecutionContext, timeout: Timeout): PartialFunction[SocketCallback, Unit] = {
    case req => actor ! req
  }
}
