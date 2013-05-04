package alesia.utils.remote.actors

import java.io.File
import java.io.PrintWriter
import akka.actor.Actor
import akka.event.Logging
import alesia.utils.remote.MsgCreateExperiment
import alesia.utils.remote.Config
import alesia.utils.remote.MsgExperimentInit
import alesia.utils.remote.MsgFilePackage
import alesia.utils.remote.MsgStartExperiment
import alesia.utils.remote.ExpID
import alesia.utils.remote.FileID
import alesia.utils.remote.MsgFilePackage
import alesia.utils.remote.FileReader
import alesia.utils.remote.MsgReadingResultsFinished

class EntryActor extends Actor {
	val log = Logging(context.system, this)
	log.info("EntryActor at service.")
	import context.dispatcher // execturion context for Futures

	override def receive = {
		case MsgCreateExperiment(lines: Array[Byte]) => {
			val worker = context.actorFor(Config.actorAdress(Config.workerActorName, Config.workerASName, Config.workerIP, Config.workerPort))
			val thisExperimentID = new ExpID
			worker ! MsgExperimentInit(thisExperimentID, Config.expMainClass) // ID of experiment at EntryActor, MainClass
			FileReader.readFile(new File(Config.experimentFileOriginalFile), Config.contextFolder, thisExperimentID, (msg => (worker ! MsgFilePackage(msg.content, msg.filename, msg.folder, msg.isLastPart, msg.eID, msg.fID))))
			//			worker ! MsgFilePackage(lines, Config.experimentFileName, ".", true, thisExperimentID, new FileID) // lines = content, Filename, (sub)Folder, isLastPart, ID
			worker ! MsgStartExperiment(Config.expMainClass, thisExperimentID) // we will use a different Message here later 
		}
		case a: MsgFilePackage => val fileActor = context.actorOf(FileActor(Config.contextFolder + Config.separator + "Entrypoint", new ExpID)); fileActor ! a
	}
}