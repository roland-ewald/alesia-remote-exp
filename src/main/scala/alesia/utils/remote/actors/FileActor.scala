package alesia.utils.remote.actors

import java.io.File

import scala.language.postfixOps
import scala.Array.canBuildFrom
import scala.collection.mutable.HashMap
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt


import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.actor.actorRef2Scala
import alesia.utils.remote.Config
import alesia.utils.remote.ExpID
import alesia.utils.remote.FileID
import alesia.utils.remote.MsgFilePackage
import alesia.utils.remote.MsgReadFile
import alesia.utils.remote.MsgReady
import alesia.utils.remote.MsgStartExperiment

/**
 * Manages file transfer.
 * 
 * Buffers file parts until the last part was received, then writing starts
 * Also creates needed Folders
 *
 * The meaning of children is that way: as long as this actor has child actors,
 * it assumes that file writing is still ongoing (@see RetryActor).
 * When all children have terminated the experiment execution may start (if the necessary
 * start message has been received).
 *
 * @param expDir full working directory (for writing files in)
 * @param eID experiment id
 */
class FileActor(expDir: String, eID: ExpID) extends AbstractActor {
  log.info("FileActor at service.")

  // storage of the file parts until all part have been received:
  val fileNamesPlus = scala.collection.mutable.HashMap[FileID, String]() // (full name)
  val contents = scala.collection.mutable.HashMap[FileID, Array[Byte]]()

  // is set true, when StartExperiment command received (is now waiting for file operations to finish)
  var startExperiment = false

  // creating experiment, results, and libs folders
  Seq(expDir, Config.resultsFolder(expDir), Config.libsFolder(expDir)).foreach(new File(_).mkdir())

  override def receive = {
    case a: MsgFilePackage => storeMessage(a.content, a.filename, a.folder, a.fID, a.isLastPart)
    case a: MsgReadFile => readFile(a.filenamePlus, a.sendTo)
    case a: MsgStartExperiment => if (context.children.isEmpty) context.parent ! MsgReady() else startExperiment = true
    case Terminated(_) => if (startExperiment && context.children.isEmpty) context.parent ! MsgReady(); startExperiment = false
  }

  def storeMessage(content: Array[Byte], filename: String, folder: String, id: FileID, isLastPart: Boolean) {
    fileNamesPlus += id -> (expDir + Config.separator + folder + Config.separator + filename)
    contents += id -> (contents.getOrElse(id, Array()) ++ content)

    if (isLastPart) writeFile(id)
  }

  def writeFile(id: FileID) {
    context.watch(context.actorOf(RetryActor(FileWritingActor(contents(id), fileNamesPlus(id)), None, FailSuccessSemantic.onTermination, FailSuccessSemantic.onTimeout, 30 seconds)))
  }

  def readFile(filenamePlus: String, sendTo: ActorRef) {
    context.watch(context.actorOf(FileReadingActor(new File(filenamePlus), expDir, eID, sendTo)))
  }
}

object FileActor {
  def apply(expDir: String, eID: ExpID): Props = Props(new FileActor(expDir, eID))
}