package support.bulkImport

/**
 * Created with IntelliJ IDEA.
 * User: matthijs
 * Date: 7/7/13
 * Time: 10:49 PM
 * To change this template use File | Settings | File Templates.
 */
object SupervisorCommandType extends Enumeration {
    type Status = Value
    val START, PAUSE, RESUME,STOP, REPORT = Value
}

class SupervisorCommand(stat: SupervisorCommandType.Status) {

  def getStatus: SupervisorCommandType.Status = {
    stat
  }

}