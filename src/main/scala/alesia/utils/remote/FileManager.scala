package alesia.utils.remote

import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.io.Source
import akka.dispatch.OnSuccess
import java.io.PrintWriter

object FileReader {
	def readFiles(foldername: String, workfolder: String, eID: expID, clsr: MsgFilePackage => Unit, finished: () => Unit)(implicit executor: ExecutionContext) {
		val fFiles = (new File(foldername)).listFiles()
		fFiles.foreach(f => readFile(f, workfolder, eID, clsr, finished))
	}

	def readFile(file: File, workfolder: String, eID: expID, clsr: MsgFilePackage => Unit, finished: () => Unit)(implicit executor: ExecutionContext) = {
		val fileFuture = Future {
			val source = Source.fromFile(file)(scala.io.Codec.ISO8859)
			val lines = source.map(_.toByte).toArray
			val filename = file.getName().drop(workfolder.length)
			val folder: String = if (file.getParentFile().equals((new File(workfolder)).getName())) "." else file.getParentFile().getName() // support only up to depth of 1, like "libs\filename.txt"

			var rest = lines
			var last = false // last sweep
			val fID = new fileID // create ID for this file
			while (!last) {
				var content: Array[Byte] = new Array(0)
				if (rest.length > 899999) {
					val split = rest.splitAt(900000) // ~900 KB max per package
					content = split._1
					rest = split._2 // iterator of this loop 
				} else {
					content = rest
					last = true
				}

				val msg = MsgFilePackage(content, filename, folder, last, eID, fID)
				clsr(msg)
			} //success
			;
		}

		fileFuture.onSuccess {
			case _ => finished;
		}
		fileFuture.onFailure {
			case exception => ; // TODO: Folder or File was not created
		}
	}
}

object FileWriter1 {
	def createFileAndFolders(content: Array[Byte], filenamePlus: String, clsr: () => Unit)(implicit executor: ExecutionContext): Unit = { // TODO: add actor for callback (errors etc)
		val file = new File(filenamePlus)
		val folderFuture = Future {
			(new File(filenamePlus)).getParentFile().mkdirs()
		}
		folderFuture.onSuccess {
			case _ => createFile(content, file, clsr());
		}
		folderFuture.onFailure {
			case exception => ; // TODO: Folder or File was not created
		}
	}
	def createFile(content: Array[Byte], file: File, clsr: Unit)(implicit executor: ExecutionContext): Unit = {
		val fos = new DataOutputStream(new FileOutputStream(file))
		if (file.exists()) {} // error: file exists. ignore? rewrite?

		// now clear to write
		val fileFuture = Future {
			fos.write(content)
			fos.close // includes flush
		}
		fileFuture.onSuccess {
			case _ => clsr; // success, execute Closure
		}
		fileFuture.onFailure {
			case exception => ; // TODO: Folder or File was not created
		}
	}
}

