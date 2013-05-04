package alesia.utils.remote

//case class ID
case class ExpID
case class ActorID
case class FileID

// Messages
// Remote (and other)
case class MsgExperimentInit(eID: ExpID, mainClazz: String) // EntryActor -> WorkerActor
case class MsgFilePackage(content: Array[Byte], filename: String, folder: String, isLastPart: Boolean, eID: ExpID, fID: FileID) // used alot
case class MsgStartExperiment(mainClazz: String, eID: ExpID) // EntryActor -> WorkerActor -> WEActor -> Watchdog

// Worker internally
case class MsgReady // FileActor -> WEActor
case class MsgFinished1 // WatchDog -> WEActor
case class MsgGetExperimentResults // WEACtor -> FileActor
case class MsgFilePackageInternal(content: Array[Byte], filename: String, folder: String, isLastPart: Boolean, eID: ExpID, fID: FileID) // used alot

// Internal (to self)
case class MsgFinished(fID: FileID) // FileActor -> SELF
case class MsgReadingResultsFinished(content: Array[Byte], filename: String, folder: String, isLastPart: Boolean, eID: ExpID, fID: FileID) // FileActor -> SELF

// Startup (ony Entry machine)
case class MsgCreateExperiment(classfileContent: Array[Byte]) // ExecuteEntry -> Entry Actor