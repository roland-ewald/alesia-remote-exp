package alesia.utils.remote.actors

import akka.actor.Actor
import akka.event.Logging
import akka.dispatch.MessageDispatcher

abstract class AbstractActor extends Actor {
	val log = Logging(context.system, this)
	import context.dispatcher
	implicit val executionContext: MessageDispatcher = dispatcher
}