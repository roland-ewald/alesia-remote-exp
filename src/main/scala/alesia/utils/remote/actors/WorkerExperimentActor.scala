package alesia.utils.remote.actors

import alesia.utils.remote.MsgFilePackage
import akka.actor.Props
import alesia.utils.remote.MsgStartExperiment
import alesia.utils.remote.MsgReady
import alesia.utils.remote.MsgFinished1
import akka.event.Logging
import akka.actor.Actor
import alesia.utils.remote.MsgGetExperimentResults
import akka.actor.ActorRef
import alesia.utils.remote.ExpID
import alesia.utils.remote.MsgFilePackageInternal

class WorkerExperimentActor(eID: ExpID, expDir: String, mainClazz: String) extends Actor {
	import context.dispatcher // execturion context for Futures
	val log = Logging(context.system, this)
	log.info("WorkerExperimentActor at service.")

	val fileActor = context.actorOf(FileActor(expDir, eID))
	val watchdogActor = context.actorOf(WatchdogActor(mainClazz, expDir))

	override def receive = {
		// From Parent
		case a: MsgStartExperiment => fileActor ! a // Asking for ready
		// From children
		case a: MsgReady =>
			log.info("WorkerExpActor: Saving Exp finished, calling Watchdog to start."); watchdogActor ! MsgStartExperiment // File Actor says: ready creating exp files
		case a: MsgFinished1 => fileActor ! MsgGetExperimentResults() // Watchdog says experiment finished
		//		case a: MsgExperimentResults1 => context.parent ! a // ???
		// From Both
		case a: MsgFilePackage => fileActor ! a
		case a: MsgFilePackageInternal => context.parent ! a
	}

	def delegateTwoWayMessages(a: MsgFilePackage, sender: ActorRef) {
		val x = context.children.toList
		if (context.children.forall(c => !c.equals(sender))) {
			val c = 15;
		}
		if (context.children.forall(c => !c.equals(sender))) fileActor ! a // Sender is not my child, send to WorkerActor
		else context.parent ! a // sender is one of my children, send back to entry actor
	}
}

object WorkerExperimentActor {
	def apply(id: ExpID, expDir: String, mainClazz: String): Props = { Props(new WorkerExperimentActor(id, expDir, mainClazz)) }
}