package alesia.utils.remote.actors

import alesia.utils.remote.MsgFilePackage
import akka.event.Logging
import akka.actor.Actor
import alesia.utils.remote.ID
import scala.collection.mutable.HashMap
import alesia.utils.remote.FileManager
import alesia.utils.remote.FileSystemIO
import alesia.utils.remote.Config

class FileActor extends Actor {
	val log = Logging(context.system, this)
	log.info("WorkerActor at service.")
	import context.dispatcher // execturion context for Futures

	implicit var experimentNumber = 0 // TODO: set this on construction or by message

	// State: (all mutable!)
	val fileNamesPlus = HashMap[ID, String]()
	val contents = HashMap[ID, Array[Byte]]()

	override def receive = {
		case msg: MsgFilePackage => {
			fileNamesPlus += msg.id -> (msg.folder + Config.separator + msg.filename)
			contents += msg.id -> (contents(msg.id) ++ msg.content)
			if (msg.isLastPart) {
				FileSystemIO.createFiles(contents(msg.id), fileNamesPlus(msg.id))
				fileNamesPlus.remove(msg.id)
				contents.remove(msg.id)
			}
		}
	}
}