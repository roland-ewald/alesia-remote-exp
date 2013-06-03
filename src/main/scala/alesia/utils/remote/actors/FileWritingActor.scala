package alesia.utils.remote.actors

import akka.actor.Props
import alesia.utils.remote.ExpID
import java.io.File
import akka.actor.Actor
import akka.event.Logging
import java.io.DataOutputStream
import akka.actor.ActorRef
import scala.concurrent.Future
import java.io.FileOutputStream

/**
 * Handles writing of files.
 *
 * @param content the files complete content
 * @param filenamePlus full filename
 */
class FileWritingActor(content: Array[Byte], filenamePlus: String) extends AbstractActor {
	log.info("FileWritingActor at service.")

	val file = new File(filenamePlus)
	val folderFuture = Future {
		(new File(filenamePlus)).getParentFile().mkdirs()
		createFile(content, file);
	}

	def createFile(content: Array[Byte], file: File): Unit = {
		System.out.println("Creating File: " + file)
		val fos = new DataOutputStream(new FileOutputStream(file))
		if (file.exists()) {} // error: file exists. ignore? rewrite?
		val s = self

		// now clear to write
		val fileFuture = Future {
			fos.write(content)
			fos.close // includes flush
			context.stop(s)
		}
	}
	override def receive = {
		case _ => ;
	}
}

object FileWritingActor {
	def apply(content: Array[Byte], filenamePlus: String): Props = Props(new FileWritingActor(content, filenamePlus))
}