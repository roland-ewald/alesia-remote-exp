package alesia.utils.remote.actors

import scala.collection.mutable.HashMap
import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging
import alesia.utils.remote.Config
import alesia.utils.remote.FileReader
import alesia.utils.remote.FileWriter1
import alesia.utils.remote.MsgFilePackage
import alesia.utils.remote.MsgGetExperimentResults
import alesia.utils.remote.MsgReady
import alesia.utils.remote.MsgStartExperiment
import alesia.utils.remote.MsgFinished
import alesia.utils.remote.MsgReadingResultsFinished
import alesia.utils.remote.fileID
import alesia.utils.remote.expID

class FileActor(expDir: String, eID: expID) extends Actor {
	val log = Logging(context.system, this)
	log.info("FileActor at service.")
	import context.dispatcher // execturion context for Futures

	// State: (all mutable!)
	val fileNamesPlus = HashMap[fileID, String]()
	val contents = HashMap[fileID, Array[Byte]]()

	val stillWorking = HashMap[fileID, Boolean]().withDefault(Unit => false) // means file system not finished with creating files
	var startExperiment = false // is set true, when StartExperiment command received (wait for file operations to exit)

	override def receive = {
		// From Parent
		case MsgFilePackage(content: Array[Byte], filename: String, folder: String, isLastPart: Boolean, eID: expID, fID: fileID) =>
			storeMessage(content, filename, folder, fID); if (isLastPart) writeFile(fID)
		case MsgGetExperimentResults => getExperimentResults
		// From self
		case MsgFinished(id: fileID) => { // File writing finished
			log.info("FileActor: File writing finished. Name: " + fileNamesPlus(id))
			stillWorking += id -> false
			if (startExperiment && stillWorking.values.toList.forall(b => b == false)) context.parent ! MsgReady()
		}
		case a: MsgReadingResultsFinished =>
			log.info("FileActor: Result file reading finished"); context.parent ! a //; context.stop(self) LATER 
		case a: MsgStartExperiment => startExperiment = true; if (stillWorking.values.toList.forall(b => b == false)) context.parent ! MsgReady()
	}

	def storeMessage(content: Array[Byte], filename: String, folder: String, id: fileID) {
		stillWorking += id -> true
		fileNamesPlus += id -> (expDir + Config.separator + folder + Config.separator + filename)
		contents += id -> (contents.getOrElse(id, Array(0): Array[Byte]) ++ content)
	}

	def writeFile(id: fileID) {
		val s = self
		FileWriter1.createFileAndFolders(contents(id), fileNamesPlus(id), (() => s ! MsgFinished(id))) // non Blocking
		//		fileNamesPlus.remove(id) // debugging...
		//		contents.remove(id)
	}

	def getExperimentResults {
		val s = self
		val p = context.parent
		FileReader.readFiles(Config.resultsFolder(expDir), Config.contextFolder, eID, msg => p ! msg, () => s ! MsgReadingResultsFinished)
	}
}

object FileActor {
	def apply(expDir: String, eID: expID): Props = Props(new FileActor(expDir, eID))
}