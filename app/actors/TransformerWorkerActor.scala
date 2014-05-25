package actors

import akka.actor.ActorRef
import models.Transformer
import org.apache.commons.lang3.StringUtils
import play.Logger
import play.api.libs.ws._
import support.bulkImport._
import au.com.bytecode.opencsv.CSVParser
import java.util.regex.Matcher
import java.util.regex.Pattern
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global
import play.api.libs.ws.WS.WSRequestHolder
import java.net.ConnectException

class TransformerWorkerActor(val supervisor: ActorRef, val transformer: Transformer, fileActor : ActorRef) extends AbstractWorkerActor(supervisor) {

  val complexHolder : WSRequestHolder =
    WS.url(transformer.webserviceURL)
      .withHeaders("content-type" -> {"text/xml;charset=" + transformer.webserviceCharSet})
      .withRequestTimeout(transformer.webserviceTimeout)


  override def onReceive(message: Any) {
    message match {
      case payload : Payload =>
        sendSoapMessage(payload)
      case fileReaderStatus: FileReaderStatus =>
        val result: WorkerResult = new WorkerResult(WorkerResultStatus.NO_WORK)
        result.setResult("I did not get a job")
        supervisor.tell(result, getSelf())
      case _ =>
        val result: WorkerResult = new WorkerResult(WorkerResultStatus.FAILED)
        result.setResult("I do not know what you want me to do with this.")
        sender.tell(result, getSelf())
    }
  }

  override def preStart() {
    Logger.debug(self.toString + " - Starting worker actor")
    if (supervisor != null)
      supervisor.tell(new WorkerResult(WorkerResultStatus.READY), getSelf())
    if (fileActor != null)
      fileActor.tell("let start", getSelf())
    else
      Logger.error("fileActor is null")
  }


  private def sendSoapMessage(payload: Payload) {

    val result: WorkerResult = new WorkerResult(WorkerResultStatus.FAILED)
    result.setFailedInput(payload.getLine)
    result.setLineNumber(payload.getLineNumber)

    try {
      val soapBody = tranformLineToSoapMessage(payload, transformer)
      val futureResponse : Future[Response] =  complexHolder.post(soapBody)
      futureResponse onSuccess {
        case response : Response =>
          if (response.body.indexOf("<soap:Fault>") > 0) {
            Logger.debug("onSuccess soap:Fault")
            result.status = WorkerResultStatus.FAILED
            result.setResult("Failed: [line: " + payload.getLineNumber + "] " + response.status + ": " + response.body)
            supervisor.tell(result, getSelf())
          } else {
            Logger.debug("onSuccess - Done")
            result.status = WorkerResultStatus.DONE
            result.setResult("Did: [line: " + payload.getLineNumber + "] " + payload.getLine)
            supervisor.tell(result, getSelf())
            fileActor.tell("give me some more", getSelf())
          }
        case _ =>
          Logger.debug("onSuccess _")
          result.status = WorkerResultStatus.FAILED
          result.setResult("Unexpected response to SOAP message")
      }
      futureResponse onFailure {
        case t : ConnectException =>
          Logger.trace("onFailure ConnectException")
          result.status = WorkerResultStatus.TIMEOUT
          result.setResult(t.getMessage)
          supervisor.tell(result, getSelf())
        case _ =>
          Logger.debug("onFailure _")
          result.status = WorkerResultStatus.FAILED
          result.setResult("Error sending SOAP message")
          supervisor.tell(result, getSelf())
      }

    } catch {
      // Soap body could not be created
      case e: Exception =>
        result.status = WorkerResultStatus.FAILED
        result.setResult("Failed to create SOAP message: " + e.getMessage)
        supervisor.tell(result, getSelf())
    }
  }

  override protected def processPayload(payload: Payload) : WorkerResult = {
    new WorkerResult(WorkerResultStatus.FAILED)
  }

  //private var xml10pattern: String = "[^" + "\u0009\r\n" + "\u0020-\uD7FF" + "\uE000-\uFFFD" + "\ud800\udc00-\udbff\udfff" + "]"

  private def replaceValuesInTemplate(template: String, values: java.util.Map[String, String]): String = {
    val sb: StringBuffer = new StringBuffer
    val pattern: Pattern = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL)
    val matcher: Matcher = pattern.matcher(template)
    while (matcher.find) {
      val key: String = matcher.group(1)
      val replacement: String = values.get(key)
      if (replacement == null) {
        throw new IllegalArgumentException("Template contains unmapped key: " + key)
      }
      val withoutCtrlChars: String = replacement.replaceAll("[\\x00-\\x09\\x11\\x12\\x14-\\x1F\\x7F]", "")
      if (withoutCtrlChars != null && withoutCtrlChars.length > 0) {
        val field: String = "<![CDATA[" + withoutCtrlChars.replace("$", "\\$") + "]]>"
        matcher.appendReplacement(sb, field)
      }
      else {
        matcher.appendReplacement(sb, "")
      }
    }
    matcher.appendTail(sb)
    SOAPCreator.translate(sb.toString)
  }


  private def tranformLineToSoapMessage(payload: Payload, transformer: Transformer): String = {
    try {
      val values: java.util.Map[String, String] = parseCsvLine(payload.getLine)
      values.put("user", transformer.webserviceUser)
      values.put("password", transformer.webservicePassword)
      values.put("timestamp", transformer.timeStampString)
      replaceValuesInTemplate(transformer.webserviceTemplate, values)
    }
    catch {
      case e: Exception =>
        Logger.error("Parsing line [" + payload.getLineNumber + "] failed: " + e.getMessage)
        throw new Exception("Parsing line [" + payload.getLineNumber + "] failed: " + e.getMessage)
    }
  }

  private def parseCsvLine(line: String): java.util.Map[String, String] = {
    val ar: java.util.Map[String, String] = new java.util.HashMap[String, String]
    val csv: CSVParser = new CSVParser(',', '"', 0.asInstanceOf[Char])
    try {
      val values: Array[String] = csv.parseLine(replaceEscapeChars(line))
      var count: Int = 0
      for (value <- values) {
        ar.put(String.valueOf(count), StringUtils.defaultString(value.toString))
        count += 1
      }
    }
    catch {
      case e: Exception =>
    }
    ar
  }

  private def replaceEscapeChars(line: String): String = {
    val newLine: String = StringUtils.replace(line, "\\n", "\n")
    StringUtils.replace(newLine, "\\\"", "\"\"")
  }

}