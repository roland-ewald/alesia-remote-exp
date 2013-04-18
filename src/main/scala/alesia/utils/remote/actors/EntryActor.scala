package alesia.utils.remote.actors

import java.io.File
import java.io.PrintWriter
import akka.actor.Actor
import akka.event.Logging
import alesia.utils.remote.MsgExperimentResults
import alesia.utils.remote.ID
import alesia.utils.remote.MsgCreateExperiment
import alesia.utils.remote.Config

class EntryActor extends Actor {
	val log = Logging(context.system, this)
	log.info("EntryActor at service.")

	override def receive = {
		case MsgExperimentResults(id: ID, content: String) => {
			log.info("Results received")
			val file = new File("Entrypoint_results.txt")

			file.createNewFile()
			val pw = new PrintWriter(file)
			try { pw.println(content); pw.println("At Entrypoint at " + System.currentTimeMillis()) } finally { pw.close() }
		}
		case MsgCreateExperiment(lines: Array[Byte]) => context.actorFor(Config.actorAdress(Config.workerActorName, Config.workerASName, Config.workerIP, Config.workerPort)) ! MsgCreateExperiment(lines)
	}
}