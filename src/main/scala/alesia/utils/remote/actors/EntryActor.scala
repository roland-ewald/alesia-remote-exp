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
import alesia.utils.remote.expID
import alesia.utils.remote.fileID
import alesia.utils.remote.MsgFilePackage

class EntryActor extends Actor {
	val log = Logging(context.system, this)
	log.info("EntryActor at service.")

	override def receive = {
		//		case MsgExperimentResults(id: ID, content: String) => {
		//			log.info("Results received")
		//			val file = new File("Entrypoint_results.txt")
		//
		//			file.createNewFile()
		//			val pw = new PrintWriter(file)
		//			try { pw.println(content); pw.println("At Entrypoint at " + System.currentTimeMillis()) } finally { pw.close() }
		//		}
		case MsgCreateExperiment(lines: Array[Byte]) => {
			val worker = context.actorFor(Config.actorAdress(Config.workerActorName, Config.workerASName, Config.workerIP, Config.workerPort))
			val thisExperimentID = new expID
			worker ! MsgExperimentInit(thisExperimentID, Config.expMainClass) // ID of experiment at EntryActor, MainClass
			worker ! MsgFilePackage(lines, Config.experimentFileName, ".", true, thisExperimentID, new fileID) // lines = content, Filename, (sub)Folder, isLastPart, ID
			worker ! MsgStartExperiment(Config.expMainClass, thisExperimentID) // we will use a different Message here later 
		}
		case a: MsgFilePackage => val fileActor = context.actorOf(FileActor(Config.contextFolder + Config.separator + "Results", new expID)); fileActor ! a
	}
}