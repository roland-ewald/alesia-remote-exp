package alesia.utils.remote

//case class ID
case class expID
case class actorID
case class fileID

// Messages
// Remote (and other)
case class MsgExperimentInit(eID: expID, mainClazz: String) // EntryActor -> WorkerActor
case class MsgFilePackage(content: Array[Byte], filename: String, folder: String, isLastPart: Boolean, eID: expID, fID: fileID) // used alot
case class MsgStartExperiment(mainClazz: String, eID: expID) // EntryActor -> WorkerActor -> WEActor -> Watchdog

// Worker internally
case class MsgReady // FileActor -> WEActor
case class MsgFinished1 // WatchDog -> WEActor
case class MsgGetExperimentResults // WEACtor -> FileActor
case class MsgFilePackageInternal(content: Array[Byte], filename: String, folder: String, isLastPart: Boolean, eID: expID, fID: fileID) // used alot

// Internal (to self)
case class MsgFinished(fID: fileID) // FileActor -> SELF
case class MsgReadingResultsFinished(content: Array[Byte], filename: String, folder: String, isLastPart: Boolean, eID: expID, fID: fileID) // FileActor -> SELF

// Startup (ony Entry machine)
case class MsgCreateExperiment(classfileContent: Array[Byte]) // ExecuteEntry -> Entry Actor