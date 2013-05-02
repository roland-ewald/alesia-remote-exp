package alesia.utils.remote.mainclasses

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import scala.io.Source
import java.io.File
import alesia.utils.remote.actors.EntryActor
import alesia.utils.remote.Config
import alesia.utils.remote.MsgCreateExperiment

object ExecuteEntry {
	def main(args: Array[String]) = {
		val entryAS = ActorSystem(Config.entryASName, ConfigFactory.load(ConfigFactory.parseString(Config.configString(Config.entryIP, Config.entryPort))))
		val entry = entryAS.actorOf(Props[EntryActor], name = Config.entryActorName)

		val file = new File(Config.experimentFileOriginalFile)
		if (file.exists()) { // send file
			val source = Source.fromFile(Config.experimentFileOriginalFile)(scala.io.Codec.ISO8859)
			val lines = source.map(_.toByte).toArray //source.mkString // send them
			source.close()

			entry ! MsgCreateExperiment(lines)

			Thread.sleep(30000)
			entryAS.shutdown // shutdown after 30 sec
		} else {
			System.out.println("Experiment .class File not found ('" + file.getAbsolutePath + "')")
		}
	}
}