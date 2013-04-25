package alesia.utils.remote

import java.io.File
import java.io.FileOutputStream
import scala.io.Source
import akka.dispatch.Future
import akka.dispatch.MessageDispatcher
import akka.dispatch.ExecutionContext

class FileManager(files: List[String], foldername: String) {
	val fFiles = files.map(s => new File(s))
	if (!fFiles.forall(f => f.exists)) fFiles // TODO: error: one file does not exist

	files.foreach(s => {
		val id = ID()
		val file = new File(s)
		val source = Source.fromFile(file)(scala.io.Codec.ISO8859) // TODO: takes time
		val lines = source.map(_.toByte).toArray
		val filename = file.getName()
		val folder = foldername

		var templines = lines
		while (templines.length > 0) {
			var last = false // last sweep
			var content: Array[Byte] = new Array(0)
			if (templines.length > 899999) {
				val split = templines.splitAt(900000) // ~900 KB max per package
				content = split._1
				templines = split._2 // iterator of this loop 
			} else {
				content = templines
				last = true
			}

			val msg = new MsgFilePackage(content, filename, folder, last, id)
			// send it
		}
	})
}

object FileSystemIO {
	def createFiles(content: Array[Byte], filenamePlus: String)(implicit executor: ExecutionContext, experimentNumber: Int): Unit = { // TODO: add actor for callback (errors etc)
		val workfolder = Config.experimentDirectory(experimentNumber) // put number here
		val file = workfolder + Config.separator + filenamePlus

		val fFile = new File(file)
		val fos = new FileOutputStream(fFile)

		if (new File(file).exists()) {} // error: file exists. ignore? rewrite?

		// now clear to write
		val fileFuture = Future {
			fFile.mkdirs()
			fos.write(content)
			fos.close // includes flush
		} onSuccess {
			case _ => ; // success
		} onFailure {
			case exception => ; // TODO: Folder or File was not created
		}

		// this prop. needs to be threads save for, say, 10 files at once 
	}
}

case class MsgFilePackage(content: Array[Byte], filename: String, folder: String, isLastPart: Boolean, id: ID)