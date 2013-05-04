package alesia.utils.remote.actors

import java.io.File
import scala.collection.mutable.HashMap
import scala.concurrent.Future
import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging
import alesia.utils.remote.Config
import alesia.utils.remote.FileReader
import alesia.utils.remote.FileWriter1
import alesia.utils.remote.MsgFilePackage
import alesia.utils.remote.MsgFinished
import alesia.utils.remote.MsgGetExperimentResults
import alesia.utils.remote.MsgReadingResultsFinished
import alesia.utils.remote.MsgStartExperiment
import alesia.utils.remote.ExpID
import alesia.utils.remote.FileID
import alesia.utils.remote.MsgFilePackageInternal
import alesia.utils.remote.MsgReady

class FileActor(expDir: String, eID: ExpID) extends Actor {
	val log = Logging(context.system, this)
	log.info("FileActor at service.")
	import context.dispatcher // execturion context for Futures

	// State: (all mutable!)
	val fileNamesPlus = HashMap[FileID, String]()
	val contents = HashMap[FileID, Array[Byte]]()

	val stillWorking = HashMap[FileID, Boolean]().withDefault(Unit => false) // means file system not finished with creating files
	var startExperiment = false // is set true, when StartExperiment command received (wait for file operations to exit)

	val f = Future { val v = (new File(Config.resultsFolder(expDir))); v.mkdirs } // creating Results Folder. TODO: catch error  

	override def postRestart(t: Throwable) = { context.stop(self) }

	override def receive = {
		// From Parent
		case MsgFilePackage(content: Array[Byte], filename: String, folder: String, isLastPart: Boolean, eID: ExpID, fID: FileID) =>
			storeMessage(content, filename, folder, fID); if (isLastPart) writeFile(fID)
		case a: MsgGetExperimentResults => getExperimentResults
		// From self
		case MsgFinished(id: FileID) => { // File writing finished
			log.info("FileActor: File writing finished. Name: " + fileNamesPlus(id))
			stillWorking += id -> false
			if (startExperiment && stillWorking.values.toList.forall(b => b == false)) { context.parent ! MsgReady(); startExperiment = false }
		}
		case MsgReadingResultsFinished(content: Array[Byte], filename: String, folder: String, isLastPart: Boolean, eID: ExpID, fID: FileID) =>
			log.info("FileActor: Result file reading finished"); context.parent ! MsgFilePackageInternal(content, filename, folder, isLastPart, eID, fID) //; context.stop(self) //LATER
		case a: MsgStartExperiment => if (stillWorking.values.toList.forall(b => b == false)) context.parent ! MsgReady() else startExperiment = true
	}

	def storeMessage(content: Array[Byte], filename: String, folder: String, id: FileID) {
		stillWorking += id -> true
		fileNamesPlus += id -> (expDir + Config.separator + folder + Config.separator + filename)
		contents += id -> (contents.getOrElse(id, Array()) ++ content)
	}

	def writeFile(id: FileID) {
		val s = self
		FileWriter1.createFileAndFolders(contents(id), fileNamesPlus(id), (() => s ! MsgFinished(id))) // non Blocking
		//		fileNamesPlus.remove(id) // debugging...
		//		contents.remove(id)
	}

	def getExperimentResults {
		log.info("FileActor: getting Exp results")
		val s = self
		val p = context.parent
		FileReader.readFiles(Config.resultsFolder(expDir), Config.contextFolder, eID, (msg => s ! msg))
	}
}

object FileActor {
	def apply(expDir: String, eID: ExpID): Props = Props(new FileActor(expDir, eID))
}