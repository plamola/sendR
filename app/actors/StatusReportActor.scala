package actors

import akka.actor.Actor
import support.bulkImport.ImportMangerSystem
import play.Logger

/**
 * Author: matthijs 
 * Created on: 22 May 2014.
 */
class StatusReportActor extends Actor {

  def receive = {
    case message : String =>
      Logger.debug(self.toString + " - Doing some reporting ")
      ImportMangerSystem.reportOnAllSuperVisors()
  }

}
