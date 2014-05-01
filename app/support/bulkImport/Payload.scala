package support.bulkImport

/**
 * Created with IntelliJ IDEA.
 * User: matthijs
 * Date: 6/30/13
 * Time: 11:25 AM
 * To change this template use File | Settings | File Templates.
 */
class Payload {

  def this(table: String, lineNumber: Long, line: String) {
    this()
  }

  def getTable: String = {
    table
  }

  def getLine: String = {
    line
  }

  def getLineNumber: Long = {
    lineNumber
  }

  private final val table: String = null
  private final val line: String = null
  private final val lineNumber: Long = 0L
}