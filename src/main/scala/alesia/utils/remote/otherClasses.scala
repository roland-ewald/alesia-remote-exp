package alesia.utils.remote

import akka.actor.ActorRef

case class ExpID // ID of an Experiment, should later be used to reconnect to an existing (running) experiment
case class FileID // ID of a File

// Remote massages
case class MsgExperimentInit(eID: ExpID, mainClazz: String) // EntryActor -> WorkerActor, Init of experiment
case class MsgHandshake(path: ActorRef) // WorkerActor -> EntryActor, connects Entry side with its WorkerExperimentActor
case class MsgStartExperiment(mainClazz: String, eID: ExpID) // EntryActor -> WorkerExperimentActor, signalizes that all Experiment Files have been sent, start of Experiment

/**
 * Contains (a part from) a file.
 * Order in which the parts are sent is important.
 * The last part has "isLastPart = true", which triggers actually writing the file
 *
 * @param content the content of this part
 * @param filename the name of the file
 * @param folder a subfolder, where this file will be saved
 * @param isLastPast weather its the last part of this file
 * @param fID FIle this part belongs to
 */
case class MsgFilePackage(content: Array[Byte], filename: String, folder: String, isLastPart: Boolean, eID: ExpID, fID: FileID)
case class MsgReadFile(filenamePlus: String, sendTo: ActorRef) // tells the FileActor to read a file and send it 

// Worker internal messages
case class MsgReady // FileActor -> WEActor, when saving files is conmplete
case class MsgExperimentConcluded // WatchDog -> WEActor, when experiment run has concluded

