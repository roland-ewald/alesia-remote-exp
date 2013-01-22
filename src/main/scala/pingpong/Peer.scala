package pingpong

import com.typesafe.config.ConfigFactory
import akka.actor._
import akka.actor.ActorSystem
import akka.actor.Props
import akka.event.Logging

object Peer {

	/*
	 * How to run this:
	 * - Uncomment as many JVM's as you want to run (e.g. 2 or 3 should be enough)
	 * - Start the instances ("run" on this file 2 or 3 times)
	 * - On the console of each instance you should now enter a number for the jvm this instance has to use,
	 * 		that ist '0', next console: '1', next console: '2', etc...
	 * 		(This is for the ports. Don't know how to negotiate the ports in a symmetric setting yet.
	 * - The whole thing needs some seconds to fire up and should then put ping pong to log.info
	 * - ignore the errors. Actors actively search for other ActorSystems running on the ports. sadly i can't catch the errors yet. 
	 * - if scala IDE starts bitching (as it does for me :-( use clean 
	 */
	val jVMList: List[JVM] =
		JVM("albert", "127.0.0.1", "2560") ::
		JVM("berta", "127.0.0.1", "2561") ::
//		JVM("caesar", "127.0.0.1", "2562") ::		
//		JVM("dagobert", "127.0.0.1", "2563") ::
//		JVM("erda", "127.0.0.1", "2564") ::
//		JVM("fogel", "127.0.0.1", "2565") ::
//		JVM("gert", "127.0.0.1", "2566") ::
//		JVM("hans", "127.0.0.1", "2567") ::
//		JVM("ilse", "127.0.0.1", "2568") ::
//		JVM("joker", "127.0.0.1", "2569") ::		
		List()
		
	def main(args: Array[String]) = {

		// choose JVM
		var b = true
		var i = 0
		while(b) {
			System.out.println("Please choose number between 0 and "+(jVMList.size-1))
			val s = readLine()
			s match {
				case Int(x) => i = x; b = false
				case _ => 
			}
		}
		val myJvm: JVM = jVMList(i)		
//	  	val myJvmList = new Random(i).shuffle(JVMList)
	  	
		// create actor system
	  	val myActorSystem = ActorSystem(myJvm.name, ConfigFactory.load(ConfigFactory.parseString(configString(myJvm))))
	  	try {
	  		val greeter = myActorSystem.actorOf(Props[ActGreeter], name = "greeter")

	  		greeter ! MsgAddWorker		  		
	  		greeter ! MsgAddWorker
	  		greeter ! MsgAddWorker	
	  		
	  		while(true) {  		  
	  			Thread.sleep(5000)
	  			greeter ! MsgGo (myJvm) // Initiates finding JVM's and Then Ping Pong
	  			System.out.println("One step")
	  		}	
	  	  
	  	  
	  	  
	  	} finally {
	  	  myActorSystem.shutdown
	  	}
	} // END OF MAIN
	
	def configString(jvm:JVM):String = {"""
		  akka {
		    actor {
		      provider = "akka.remote.RemoteActorRefProvider"
		    }
		    remote {
		      transport = "akka.remote.netty.NettyRemoteTransport"
		      netty {
		        hostname = """"+jvm.ip+""""
		        port = """+jvm.port+"""
		      }
		    }
		  }
		"""} // NOTE: IP IS SURROUNDED BY "'s 
	
	def greeterString(jvm:JVM):String = "akka://"+jvm.name+"@"+jvm.ip+":"+jvm.port+"/user/greeter"
}

class ActGreeter() extends Actor {
    val log = Logging(context.system, this)
    log.info("Greeter at service.")    
    var children: List[ActorRef] = List()
	override def receive = {
    	case MsgGo(except) => Peer.jVMList.foreach(j => if(j!= except) try{context.actorFor(Peer.greeterString(j)) ! MsgChallengeExistence} catch {case _ => })
	  	case MsgChallengeExistence => children.foreach(c => c ! MsgTellThemExistence(sender)) 
	  	case MsgIExist => children.foreach(c => c ! MsgTheyExist(sender))
	  	case MsgAddWorker => children = context.actorOf(Props[ActWorker]) :: children
	  	case MsgRemoveWorker => children = children.reverse.tail.reverse; context.stop(children.reverse.head)
	}
}

class ActWorker extends Actor {
    val log = Logging(context.system, this)
    log.info("Worker at service.")
	override def receive = {
	  	case MsgTellThemExistence(who) => who ! MsgIExist
	  	case MsgTheyExist(who) => who ! MsgPing("")
	  	case MsgPing(str) => sender ! MsgPong(str+" Ping from "+sender+"\n")
	  	case MsgPong(str) => if(str.size > 200) log.info(str) else sender ! MsgPing(str+" Pong from "+sender+"\n")
	}
}

case class MsgGo(except: JVM)
case class MsgPing(str:String)
case class MsgPong(str:String)
case class MsgChallengeExistence
case class MsgTellThemExistence(who:ActorRef)
case class MsgIExist
case class MsgTheyExist(who:ActorRef)
case class MsgAddWorker
case class MsgRemoveWorker

case class JVM(name:String, ip:String, port:String)	

object Int {
  def unapply(s : String) : Option[Int] = try {
    Some(s.toInt)
  } catch {
    case _ : java.lang.NumberFormatException => None
  }
}