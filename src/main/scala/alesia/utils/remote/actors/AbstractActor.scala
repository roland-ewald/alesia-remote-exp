package alesia.utils.remote.actors

import akka.actor.Actor
import akka.event.Logging
import akka.dispatch.MessageDispatcher

/**
 * Super type of all actors.
 */
abstract class AbstractActor extends Actor {

  /** Common logging service. */
  val log = Logging(context.system, this)

  /** Required by futures. */
  implicit val executionContext: MessageDispatcher = context.dispatcher
}