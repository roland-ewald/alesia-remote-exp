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
	def readFiles(foldername: String, workfolder: String, eID: ExpID, clsr: MsgReadingResultsFinished => Unit)(implicit executor: ExecutionContext) {
		val fFiles = (new File(foldername)).listFiles()
		fFiles.foreach(f => readFile(f, workfolder, eID, clsr))
	}

	def readFile(file: File, workfolder: String, eID: ExpID, clsr: MsgReadingResultsFinished => Unit)(implicit executor: ExecutionContext) = {
		System.out.println("Reading File: " + file + ", folder: " + workfolder)
		val fileFuture = Future {
			val source = Source.fromFile(file)(scala.io.Codec.ISO8859)
			val lines = source.map(_.toByte).toArray
			val filename = file.getName() //.drop(workfolder.length)
			val folder: String = if (file.getParentFile().equals((new File(workfolder)).getName())) "." else file.getParentFile().getName() // support only up to depth of 1, like "libs\filename.txt"

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

				val msg = MsgReadingResultsFinished(content, filename, folder, last, eID, fID)
				System.out.println("FileReader: sending one Message.")
				clsr(msg)
			} //success
			;
		}

		fileFuture.onSuccess {
			case _ => ;
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
		System.out.println("Creating File: " + file)
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

