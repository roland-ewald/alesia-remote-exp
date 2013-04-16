package remoteExperimentSimple

import java.lang.reflect.Array

object Config {

	val workerIP = "127.0.0.1"
	val workerPort = "2500"
	val workerASName = "WorkerActorSystem"
	val workerActorName = "WorkerActor"

	val entryIP = "127.0.0.1"
	val entryPort = "2501"
	val entryASName = "EntryActorSystem"
	val entryActorName = "EntryActor"

	val separator: String = System.getProperty("file.separator");
	val contextFolders = "C:\\Users\\JoWa\\Desktop\\HiWi Akka\\workspace\\akka-remote-test2"

	val experimentCommandSeq = Seq("java", "-cp", "\"" + contextFolders + "\"", "HelloWoerld")
	val resultFileName = contextFolders + "\\results.txt"
	val experimentFileOriginalFile = "src\\main\\resources\\HelloWoerld.class"
	val experimentFileName = "HelloWoerld.class"

	def configString(ip: String, port: String): String = {
		"""
			  akka {
			    actor {
			      provider = "akka.remote.RemoteActorRefProvider"
			    }
			    remote {
			      transport = "akka.remote.netty.NettyRemoteTransport"
			      netty {
			        hostname = """" + ip + """"
			        port = """ + port + """
			      }
			    }
			  }
		"""
	} // NOTE: IP IS SURROUNDED BY "'s
	def actorAdress(name: String, asName: String, ip: String, port: String): String = "akka://" + asName + "@" + ip + ":" + port + "/user/" + name

}