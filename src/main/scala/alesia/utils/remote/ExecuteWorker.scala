package alesia.utils.remote

import com.typesafe.config.ConfigFactory
import akka.actor._
import akka.actor.ActorSystem
import akka.actor.Props
import akka.event.Logging
import alesia.utils.remote.actors.WorkerActor

object ExecuteWorker {
	def main(args: Array[String]) = {
		val workerAS = ActorSystem(Config.workerASName, ConfigFactory.load(ConfigFactory.parseString(Config.configString(Config.workerIP, Config.workerPort))))

		val worker = workerAS.actorOf(Props[WorkerActor], name = Config.workerActorName)
		Thread.sleep(30000)
		workerAS.shutdown // shutdown after 30 sec
	}
}