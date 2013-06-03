package alesia.utils.remote.actors

import akka.actor.actorRef2Scala
import alesia.utils.remote.Config
import alesia.utils.remote.ExpID
import alesia.utils.remote.MsgExperimentInit
import alesia.utils.remote.MsgFilePackage
import alesia.utils.remote.MsgHandshake
import alesia.utils.remote.MsgReadFile
import alesia.utils.remote.MsgStartExperiment

/**
 * Central Actor on the Entry ActorSystem
 */
class EntryActor extends AbstractActor {
	log.info("EntryActor at service.")

	val expID = new ExpID
	val fileActor = context.actorOf(FileActor(Config.entrypointDir, expID))

	// remote actor:
	val worker = context.actorFor(Config.actorAdress(Config.workerActorName, Config.workerASName, Config.workerIP, Config.workerPort))
	worker ! MsgExperimentInit(expID, Config.expMainClass)
	// wating for handshake...

	override def receive = {
		case MsgHandshake(aRef) => {
			fileActor ! MsgReadFile(Config.entrypointDir + Config.separator + Config.classFileName, aRef) // send class file
			aRef ! MsgStartExperiment( expID) // signalls, all files have been sent, start exec
		}
		case a: MsgFilePackage => fileActor ! a // for result file
	}
}
