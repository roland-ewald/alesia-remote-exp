package alesia.utils.remote

case class ID

case class MsgCreateExperiment(classfileContent: String)
case class MsgCreateExperiment2(classfileContent: String, number: Int, id: ID)
case class MsgExperimentReady
case class MsgIsExperimentReady
case class MsgIsExperimentCreated
case class MsgExperimentResults(id: ID, content: String)