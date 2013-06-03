package alesia.utils.remote.actors

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.event.Logging
import akka.pattern.ask
import alesia.utils.remote.Config
import alesia.utils.remote.ExpID
import alesia.utils.remote.MsgExperimentInit
import alesia.utils.remote.MsgHandshake

/**
 * Central Actor on the Worker ActorSystem.
 * This is always existent and can be reached by its address from remote
 * Each new connection gets its own experiment folder (with a new number at the end)
 * COnnections are answered with a Handshake message, with the ActorRef of the relevant WorkerExperimentActor
 */
class WorkerActor extends AbstractActor {
	log.info("WorkerActor at service.")

	var experimentNumber = 1 // determines next experiment folder

	override def receive = {
		case MsgExperimentInit(id: ExpID, mainClazz: String) => newExperiment(sender, id, mainClazz)
	}

	def newExperiment(sender: ActorRef, id: ExpID, mainClazz: String) {
		val experimentDir = Config.experimentDirectory(experimentNumber)
		experimentNumber = experimentNumber + 1
		val experiment = context.actorOf(WorkerExperimentActor(id, experimentDir, mainClazz, sender), name = Config.experimentDirectory(experimentNumber))

		sender ! MsgHandshake(experiment)
	}
}

