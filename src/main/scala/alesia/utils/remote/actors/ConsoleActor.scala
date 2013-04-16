package alesia.utils.remote.actors

import akka.actor.Actor
import akka.event.Logging
import akka.dispatch.Future
import alesia.utils.remote.ID

class ConsoleActor extends Actor {
	import context.dispatcher
	val log = Logging(context.system, this)
	log.info("ConsoleActor at service.")

	def receive = {
		case MsgGet(message: String, id: ID) => {
			val sendr = sender
			val f = Future {
				System.out.println(message)
				readLine()
			} onSuccess {
				case resp: String => sendr ! MsgGot(resp, id)
			}
		}
	}

	case class MsgGet(message: String, id: ID)
	case class MsgGot(response: String, id: ID)
}