package alesia.utils.remote.actors

import scala.concurrent.duration.Duration

import akka.actor.Actor
import akka.actor.Props
import akka.actor.ReceiveTimeout
import akka.actor.Terminated
import akka.actor.actorRef2Scala
import akka.event.Logging

/**
 * Encapsulates actor failure and ensures messages processing.
 * 
 * This actor is supposed to have one child actor that has to fulfill a single task
 * on construction.
 * 
 * If the child actor fails, it will be reconstructed. If it succeeds, this actor will stop.
 * 
 * This actor can also deliver a message to the child actor on construction.
 *
 * @param p Props of child Actor
 * @param msg possible Message for child actor. delivery is ensured until actor succeeds
 * @param success FailSuccessSemantic for success. Possible Semantics are: on Termination, onMessageSent(to this actor=its parent), onTimeout
 * @param fail: Semantic for failure
 * @param to: duration for timeout
 */
class RetryActor(p: Props, msg: Option[Any], success: FailSuccessSemantic.FailSuccessSemantic, fail: FailSuccessSemantic.FailSuccessSemantic, to: Duration) extends AbstractActor {
	log.info("RetryActor at service.")

	if (success == fail) throw new IllegalArgumentException()

	val a = context.actorOf(p)
	context.watch(a)
	msg match { case Some(x) => a ! x; case None => ; }

	if (success == FailSuccessSemantic.onTimeout || fail == FailSuccessSemantic.onTimeout) context.setReceiveTimeout(to)

	override def receive = {
		case Terminated(`a`) => if (success == FailSuccessSemantic.onTermination) context.stop(self) else if (fail == FailSuccessSemantic.onTermination) retry else context.stop(self)
		case ReceiveTimeout =>
			if (success == FailSuccessSemantic.onTimeout) context.stop(self) else if (fail == FailSuccessSemantic.onTimeout) context.stop(a); retry
		case x => if (sender.equals(a)) { if (success == FailSuccessSemantic.onMessageSent) context.stop(self) else if (fail == FailSuccessSemantic.onMessageSent) retry } else context.parent ! x
	}

	def retry {
		val a = context.actorOf(p)
		context.watch(a)
		a ! msg
	}

}

object RetryActor {
	def apply(p: Props, msg: Option[Any], success: FailSuccessSemantic.FailSuccessSemantic, fail: FailSuccessSemantic.FailSuccessSemantic, to: Duration): Props = Props(new RetryActor(p, msg, success, fail, to))
}

/**
 * Enum determining Fail or Success Semantics
 */
object FailSuccessSemantic extends Enumeration {
	type FailSuccessSemantic = Value
	val onTermination, onMessageSent, onTimeout = Value
}

//
case class MsgRetry()