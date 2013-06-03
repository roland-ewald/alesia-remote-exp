package alesia.utils.remote

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import alesia.utils.remote.Config
import alesia.utils.remote.actors.EntryActor

/**
 * Central Actor on the Entrypoint ActorSystem
 */
object ExecuteEntry {
	def main(args: Array[String]) = {

		val entryAS = ActorSystem(Config.entryASName, ConfigFactory.load(ConfigFactory.parseString(Config.configString(Config.entryIP, Config.entryPort))))
		val entry = entryAS.actorOf(Props[EntryActor], name = Config.entryActorName)

		Thread.sleep(30000)
		entryAS.shutdown // shutdown after 30 sec
	}
}