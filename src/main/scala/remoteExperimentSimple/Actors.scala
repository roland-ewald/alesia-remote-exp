package remoteExperimentSimple

import java.io.File
import java.io.PrintWriter

import scala.io.Source
import scala.sys.process.Process

import akka.actor.Actor
import akka.actor.actorRef2Scala
import akka.event.Logging
import akka.util.duration.intToDurationInt

object Actors {
	class WorkerActor extends Actor {
	    val log = Logging(context.system, this)
	    log.info("WorkerActor at service.")    
	    var schedule:akka.actor.Cancellable = null
	    var schedule1:akka.actor.Cancellable = null
	    var stream:Stream[String] = null
	    var pb:scala.sys.process.ProcessBuilder = null
	    
	    override def receive = {
	    	case MsgCreateExperiment(classfileContent:String) => {
	    	  log.info("Msg received"); 
	    	  
	    	  val file = new File(Config.experimentFileName)
	    	  file.deleteOnExit()
	    	  
	    	  val pw = new PrintWriter(file)
	    	  try {pw.println(classfileContent)} finally {pw.close()} // note: that operation takes time
	    	  
	    	  schedule1 = context.system.scheduler.schedule(0 seconds, 1 seconds, self, MsgIsExperimentCreated)	    
	    	}
	    	case MsgIsExperimentCreated => {
	    		log.info("Experiment File created");
	    		val file = new File(Config.experimentFileName)
	    		if(file.exists()) {
	    			schedule1.cancel
	    			
	    			pb = Process(Config.experimentCommandSeq) 
	    			stream = pb.lines
	    			
	    			schedule = context.system.scheduler.schedule(0 seconds, 1 seconds, self, MsgIsExperimentReady)
	    		}	  
	    	}
	    	case MsgIsExperimentReady => {
	    	  // test weather the experiment is ready
	    	  // yes: send file
	    	  //      send stream?
	    	  //      cancel schedule
	    	  // else: nothing (for now)
	    	  log.info("Experiment was startet");
	    	  if(pb.hasExitValue) {
	    	    log.info("Experiment has exited")
	    		  // get file
	    		  // send file
	    	      val file = new File(Config.resultFileName)
	    		  if(file.exists()) {// send file
	    			  val source = Source.fromFile(file)
	    			  val lines = source.mkString // send them
	    			  source.close ()
	    			  
	    			  context.actorFor(Config.actorAdress(Config.entryActorName, Config.entryASName, Config.entryIP, Config.entryPort)) ! MsgExperimentResults(Config.resultFileName, lines)
	    			  
	    			  file.delete()
	    		  } else {
	    		    log.error("no result file found")
	    		  }
	    		    
	    		  schedule.cancel
	    		  stream = null
	    		  pb = null
	    		  schedule = null
	    	  }
	    	  // nothing
	    	}
	    }
	    
	    
	}
	
	class EntryActor extends Actor { 
	    val log = Logging(context.system, this)
	    log.info("EntryActor at service.")    

	    override def receive = {
	    	case MsgExperimentResults(filename:String, content:String) => {
	    	  log.info("Results received")
	    	  val file = new File("Entrypoint_results.txt")
	    	  
	    	  file.createNewFile()
	    	  val pw = new PrintWriter(file)
	    	  try {pw.println(content);pw.println("At Entrypoint at "+System.currentTimeMillis())} finally {pw.close()}
	    	}
	    }
	}
	
}

case class MsgCreateExperiment(classfileContent:String)
case class MsgExperimentReady
case class MsgIsExperimentReady
case class MsgExperimentResults(filename:String, content:String)
case class MsgIsExperimentCreated