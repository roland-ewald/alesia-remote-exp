package alesia.utils.remote.actors

import java.io.File

import scala.concurrent.Future
import scala.io.Source

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.event.Logging
import alesia.utils.remote.Config
import alesia.utils.remote.ExpID
import alesia.utils.remote.FileID
import alesia.utils.remote.MsgFilePackage

/**
 * Handles reading of Files
 * Companion object provides additional functionality.
 *
 * @param file the full filename
 * @param workfolder the system specific folder. if the files parent folder differs from this it will be created on the target system this files parentfolder
 * @param eID experimentID
 * @param aRef ActorRef to which this File will be send
 */
class FileReadingActor(file: File, workfolder: String, eID: ExpID, aRef: ActorRef) extends AbstractActor {

	log.info("FileWritingActor at service.")
	log.info("Reading File: " + file + ", folder: " + workfolder)
	val fileFuture = Future {
		val source = Source.fromFile(file)(scala.io.Codec.ISO8859)
		val lines = source.map(_.toByte).toArray
		val filename = file.getName() //.drop(workfolder.length)

		val parentFolder = file.getParentFile()
		val folder: String = if (parentFolder.equals(new File(workfolder))) "." else file.getParentFile().getName() // support only up to depth of 1, like "libs\filename.txt"

		var rest = lines
		var last = false // last sweep
		val fID = new FileID // create ID for this file
		while (!last) {
			var content = Array[Byte]()
			if (rest.length > Config.maxPackageSize + 1) {
				val split = rest.splitAt(Config.maxPackageSize) // ~900 KB max per package
				content = split._1
				rest = split._2 // iterator of this loop 
			} else {
				content = rest
				last = true
			}

			val msg = MsgFilePackage(content, filename, folder, last, eID, fID)
			System.out.println("FileReader: sending one Message.")
			aRef ! msg
		} //success
		context.stop(self)
	}

	override def receive = {
		case _ => ;
	}
}

object FileReadingActor {
	def apply(file: File, workfolder: String, eID: ExpID, aRef: ActorRef): Props = Props(new FileReadingActor(file, workfolder, eID, aRef))
	/**
	 * Scans a folder and sends all files there.
	 * Creates one Props for each file.
	 */
	def apply(foldername: String, workfolder: String, eID: ExpID, aRef: ActorRef): List[Props] = {
		val fFiles = (new File(foldername)).listFiles() // here be blocking
		var res: List[Props] = List()
		fFiles.foreach(f => res = (Props(new FileReadingActor(f, workfolder, eID, aRef)) :: res))
		res
	}
}