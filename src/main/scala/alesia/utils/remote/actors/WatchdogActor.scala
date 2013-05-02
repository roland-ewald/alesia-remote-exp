package alesia.utils.remote.actors

import java.io.File
import scala.concurrent.Future
import scala.sys.process.Process
import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.event.Logging
import alesia.utils.remote.Config
import alesia.utils.remote.MsgFinished1

class WatchdogActor(clazzName: String, directory: String) extends Actor {
	import context.dispatcher // execturion context for Futures
	val log = Logging(context.system, this)
	log.info("WatchdogActor at service.")

	override def receive = {
		case _ => {
			log.info("WatchdogActor: starting execution.");

			// create the process that is the experiment:
			val pb = Process(Config.experimentCommandSeq(1), new File(Config.contextFolder + Config.separator + Config.experimentDirectory(1)))
			val p = context.parent

			val f = Future {
				val res = pb.!! // executes the console lines. see execution context
				log.info("Watchdog: " + res)
				p ! MsgFinished1()
			}
			f.onFailure {
				case e => log.info("Error: " + e)
			}
		}
	}
}

object WatchdogActor {
	def apply(clazzName: String, directory: String): Props = { Props(new WatchdogActor(clazzName, directory)) }
}