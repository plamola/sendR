package support.bulkImport.workers

import akka.actor.ActorRef
import models.Transformer
import org.apache.commons.lang3.StringUtils
import play.Logger
import play.libs.WS
import support.bulkImport.Payload
import support.bulkImport.SOAPCreator
import support.bulkImport.WorkerResult
import au.com.bytecode.opencsv.CSVParser
import java.util.regex.Matcher
import java.util.regex.Pattern

object WebserviceWorkerActor {

}

class WebserviceWorkerActor(val inJobController: ActorRef, val transformer: Transformer) extends AbstractWorkerActor(inJobController) {

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

  //private var xml10pattern: String = "[^" + "\u0009\r\n" + "\u0020-\uD7FF" + "\uE000-\uFFFD" + "\ud800\udc00-\udbff\udfff" + "]"


  override protected def processPayload(payload: Payload, result: WorkerResult) {
    var soapBody: String = null
    try {
      soapBody = tranformLineToSoapMessage(payload, transformer, result)
      if (soapBody == null) {
        result.setStatus(WorkerResult.Status.FAILED)
        return
      }
    }
    catch {
      case e: Exception =>
        result.setResult(e.getMessage)
        result.setStatus(WorkerResult.Status.FAILED)
        return
    }
    result.setLineNumber(payload.getLineNumber)
    try {
      val requestHolder: WS.WSRequestHolder = WS.url(transformer.webserviceURL).setContentType("text/xml;charset=" + transformer.webserviceCharSet)
      requestHolder.setTimeout(transformer.webserviceTimeout)
      Logger.trace(self.toString + " - Ready to send request to " + transformer.webserviceURL)
      val response: WS.Response = requestHolder.post(soapBody).get
      if (response.getBody.indexOf("<soap:Fault>") > 0) {
        result.setFailedInput(payload.getLine)
        result.setResult("Failed: [line: " + payload.getLineNumber + "] " + response.getStatus + ": " + response.getBody)
        result.setStatus(WorkerResult.Status.FAILED)
      }
      else {
        result.setResult("Did: [line: " + payload.getLineNumber + "] " + payload.getLine)
        result.setStatus(WorkerResult.Status.DONE)
      }
    }
    catch {
      case e: Exception =>
        result.setFailedInput(payload.getLine)
        result.setResult("Failed: [line: " + payload.getLineNumber + "] " + e.getMessage)
        result.setStatus(WorkerResult.Status.TIMEOUT)
    }
  }

  private[workers] def tranformLineToSoapMessage(payload: Payload, transformer: Transformer, result: WorkerResult): String = {
    try {
      val values: java.util.Map[String, String] = parseCsvLine(payload.getLine)
      values.put("user", transformer.webserviceUser)
      values.put("password", transformer.webservicePassword)
      values.put("timestamp", transformer.timeStampString)
      var bodyContent: String = transformer.webserviceTemplate
      bodyContent = replaceValuesInTemplate(bodyContent, values)
      bodyContent
    }
    catch {
      case e: Exception =>
        Logger.error("Parsing line [" + payload.getLineNumber + "] failed: " + e.getMessage)
        result.setResult("Parsing line [" + payload.getLineNumber + "] failed: " + e.getMessage)
        null
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