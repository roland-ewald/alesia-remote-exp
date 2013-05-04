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
import alesia.utils.remote.ExpID
import alesia.utils.remote.MsgFilePackageInternal

class WorkerActor extends Actor {
	val log = Logging(context.system, this)
	log.info("WorkerActor at service.")

	var experimentActorMap = new HashMap[ExpID, ActorRef] // WorkerExperimentActors
	var entryActorMap = new HashMap[ExpID, ActorRef] // EntryActors	
	var experimentNumber = 1 // arbitrary number, just for experiment folders

	override def receive = {
		// From outside
		case MsgExperimentInit(id: ExpID, mainClazz: String) => newExperiment(sender, id, mainClazz)
		case a: MsgStartExperiment => experimentActorMap(a.eID) ! a // TODO: experiment id not existent
		// From children
		//		case a: MsgExperimentResults => entryActorMap(a.e) ! a // TODO: cleanup references
		//		case a: MsgExperimentResults1 => entryActorMap(a.id) ! a // TODO: remove references		
		// From both
		case a: MsgFilePackage =>
			experimentActorMap(a.eID) ! a; log.info("Worker got a Msg Package") // TODO: experiment id not existent
		case a: MsgFilePackageInternal => entryActorMap(a.eID) ! MsgFilePackage(a.content, a.filename, a.folder, a.isLastPart, a.eID, a.fID); log.info("Sending Results back")
	}

	// Starts new Exeriment
	def newExperiment(sender: ActorRef, id: ExpID, mainClazz: String) {
		val experimentDir = Config.experimentDirectory(experimentNumber)
		experimentNumber = experimentNumber + 1
		val experiment = context.actorOf(WorkerExperimentActor(id, experimentDir, mainClazz))

		experimentActorMap += id -> experiment
		entryActorMap += id -> sender
	}
}

