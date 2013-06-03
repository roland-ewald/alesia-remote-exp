package alesia.utils.remote.actors

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.event.Logging
import alesia.utils.remote.Config
import alesia.utils.remote.ExpID
import alesia.utils.remote.MsgExperimentConcluded
import alesia.utils.remote.MsgFilePackage
import alesia.utils.remote.MsgReady
import alesia.utils.remote.MsgStartExperiment
import alesia.utils.remote.MsgReadFile

/**
 * Central Actor for an Experiment on the Worker side
 * @param eID the ID of this experiment
 * @param expDir the directory for this experiment
 * @param mainClazz main class to start the experiment with
 * @param remoteActor ActorRef to remote EntryActor
 */
class WorkerExperimentActor(eID: ExpID, expDir: String, mainClazz: String, remoteActor: ActorRef) extends AbstractActor {
	log.info("WorkerExperimentActor at service.")

	val fileActor = context.actorOf(FileActor(expDir, eID), name = "FileActor")

	override def receive = {
		case a: MsgStartExperiment => fileActor ! a
		case a: MsgReady => startExperimentExecution // from File Actor ("writing files finished")
		case a: MsgExperimentConcluded => experimentExecutionFinished
		case a: MsgFilePackage => fileActor ! a
	}

	def startExperimentExecution {
		log.info("WorkerExpActor: Saving Exp finished, creating Watchdog to start.");
		context.actorOf(WatchdogActor(Config.contextFolder, expDir, Config.resultsFolderName, mainClazz), name = "WatchdogActor")
	}

	def experimentExecutionFinished {
		fileActor ! MsgReadFile(Config.resultsFolder(expDir) + Config.separator + Config.resultFileName, remoteActor) // Watchdog says experiment finished
	}
}

object WorkerExperimentActor {
	def apply(id: ExpID, expDir: String, mainClazz: String, remoteActor: ActorRef): Props = { Props(new WorkerExperimentActor(id, expDir, mainClazz, remoteActor: ActorRef)) }
}