package alesia.utils.remote.actors

import scala.collection.immutable.HashMap
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.event.Logging
import alesia.utils.remote.Config
import alesia.utils.remote.MsgExperimentInit
import alesia.utils.remote.MsgFilePackage
import alesia.utils.remote.MsgReady
import alesia.utils.remote.MsgStartExperiment
import alesia.utils.remote.MsgStartExperiment
import akka.pattern.ask
import alesia.utils.remote.MsgStartExperiment
import alesia.utils.remote.MsgFinished1
import alesia.utils.remote.MsgGetExperimentResults
import alesia.utils.remote.MsgFilePackage
import alesia.utils.remote.expID

class WorkerActor extends Actor {
	val log = Logging(context.system, this)
	log.info("WorkerActor at service.")

	var experimentActorMap = new HashMap[expID, ActorRef] // WorkerExperimentActors
	var entryActorMap = new HashMap[expID, ActorRef] // EntryActors	
	var experimentNumber = 1 // arbitrary number, just for experiment folders

	override def receive = {
		// From outside
		case MsgExperimentInit(id: expID, mainClazz: String) => newExperiment(sender, id, mainClazz)
		case a: MsgStartExperiment => experimentActorMap(a.eID) ! a // TODO: experiment id not existent
		// From children
		//		case a: MsgExperimentResults => entryActorMap(a.e) ! a // TODO: cleanup references
		//		case a: MsgExperimentResults1 => entryActorMap(a.id) ! a // TODO: remove references		
		// From both
		case a: MsgFilePackage => delegateTwoWayMessages(a, sender) // TODO: experiment id not existent
	}

	// Starts new Exeriment
	def newExperiment(sender: ActorRef, id: expID, mainClazz: String) {
		val experimentDir = Config.experimentDirectory(experimentNumber)
		experimentNumber = experimentNumber + 1
		val experiment = context.actorOf(WorkerExperimentActor(id, experimentDir, mainClazz))

		experimentActorMap += id -> experiment
		entryActorMap += id -> sender
	}

	// decide, weather a MsgFilePackage has to go outside (EntryActor) or inside (WorkerExperimentActor)
	def delegateTwoWayMessages(a: MsgFilePackage, sender: ActorRef) {
		if (context.children.forall(c => !c.equals(sender))) experimentActorMap(a.eID) ! a // Sender is not my child, send to experimentActor
		else entryActorMap(a.eID) ! a // sender is one of my children, send back to entry actor
	}
}

