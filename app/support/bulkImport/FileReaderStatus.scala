package support.bulkImport

/**
 * Author: matthijs 
 * Created on: 24 May 2014.
 */
object FileReaderStatusType extends Enumeration {
  type  Status = Value
  val READY,SUICIDE, NO_WORK = Value
}


class FileReaderStatus(state: FileReaderStatusType.Status) {
  var status: FileReaderStatusType.Status = state
}
