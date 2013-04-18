package alesia.utils.remote

case class ID

case class MsgCreateExperiment(classfileContent: Array[Byte])
case class MsgCreateExperiment2(classfileContent: Array[Byte], number: Int, id: ID)
case class MsgExperimentReady
case class MsgIsExperimentReady
case class MsgIsExperimentCreated
case class MsgExperimentResults(id: ID, content: String)