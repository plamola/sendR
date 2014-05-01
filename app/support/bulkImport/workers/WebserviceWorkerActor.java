package support.bulkImport.workers;

import akka.actor.ActorRef;
import models.Transformer;
import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.libs.WS;
import support.bulkImport.Payload;
import support.bulkImport.SOAPCreator;
import support.bulkImport.WorkerResult;
import au.com.bytecode.opencsv.CSVParser;
import org.apache.commons.lang3.StringEscapeUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class WebserviceWorkerActor extends AbstractWorkerActor {

    private final Transformer transformer;

    // XML 1.0
    // #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
    private static String xml10pattern = "[^"
            + "\u0009\r\n"
            + "\u0020-\uD7FF"
            + "\uE000-\uFFFD"
            + "\ud800\udc00-\udbff\udfff"
            + "]";



    public WebserviceWorkerActor(ActorRef inJobController, Transformer transformer) {
        super(inJobController);
        this.transformer = transformer;
    }

    @Override
    protected void processPayload(Payload payload, WorkerResult result) {
        String soapBody;
        try {
            soapBody = tranformLineToSoapMessage(payload, transformer, result);
            if (soapBody == null) {
                result.setStatus(WorkerResult.Status.FAILED);
                return;
            }
        } catch (Exception e) {
            result.setResult(e.getMessage());
            result.setStatus(WorkerResult.Status.FAILED);
            return;
        }

        result.setLineNumber(payload.getLineNumber());
        try {
            WS.WSRequestHolder requestHolder =
                    WS.url(transformer.webserviceURL)
                            .setContentType("text/xml;charset=" + transformer.webserviceCharSet);
            requestHolder.setTimeout(transformer.webserviceTimeout);
            Logger.trace(self().toString() + " - Ready to send request to " + transformer.webserviceURL);
            WS.Response response = requestHolder.post(soapBody).get();

            if (response.getBody().indexOf("<soap:Fault>") > 0) {
                result.setFailedInput(payload.getLine());
                result.setResult("Failed: [line: " + payload.getLineNumber() + "] " + response.getStatus() + ": " + response.getBody());
                result.setStatus(WorkerResult.Status.FAILED);
            } else {
                result.setResult("Did: [line: " + payload.getLineNumber() + "] " + payload.getLine());
                result.setStatus(WorkerResult.Status.DONE);
            }
        } catch (Exception e) {
            // not a direct result of the parsed response; must try again?
            result.setFailedInput(payload.getLine());
            result.setResult("Failed: [line: " + payload.getLineNumber() + "] " + e.getMessage());
            result.setStatus(WorkerResult.Status.TIMEOUT);

        }
    }


    String tranformLineToSoapMessage(Payload payload, Transformer transformer, WorkerResult result) { //throws Exception {
        try {
            Map<String, String> values = parseCsvLine(payload.getLine());

            values.put("user", transformer.webserviceUser);
            values.put("password", transformer.webservicePassword);
            values.put("timestamp", transformer.timeStampString);

            String bodyContent = transformer.webserviceTemplate;
            bodyContent = replaceValuesInTemplate(bodyContent, values);
            return bodyContent;
        } catch (Exception e) {
            Logger.error("Parsing line [" + payload.getLineNumber() + "] failed: " + e.getMessage());
            result.setResult("Parsing line [" + payload.getLineNumber() + "] failed: " + e.getMessage());
            return null;
            //throw new Exception("Parsing line [" + payload.getLineNumber() + "] failed: " + e.getMessage());
        }
    }


    private static String replaceValuesInTemplate(final String template,
                                        final Map<String, String> values) {


        final StringBuffer sb = new StringBuffer();
        final Pattern pattern =
                Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(template);
        while (matcher.find()) {
            final String key = matcher.group(1);

            final String replacement = values.get(key);

            if (replacement == null) {
                throw new IllegalArgumentException(
                        "Template contains unmapped key: "
                                + key);
            }
            String withoutCtrlChars = replacement.replaceAll("[\\x00-\\x09\\x11\\x12\\x14-\\x1F\\x7F]", "");
            //String withoutCtrlChars = replacement.replaceAll(xml10pattern, "");

            if  (withoutCtrlChars != null && withoutCtrlChars.length() > 0) {
                final String field = "<![CDATA[" + withoutCtrlChars.replace("$", "\\$") + "]]>";
                matcher.appendReplacement(sb, field);
            } else {
                matcher.appendReplacement(sb, "");
            }

        }
        matcher.appendTail(sb);

        // Remove the empty elements from the XML.
        return SOAPCreator.translate(sb.toString());
    }


    private Map<String, String> parseCsvLine(String line) {
        Map<String, String> ar = new HashMap<String, String>();
        CSVParser csv = new CSVParser(',', '"', (char)0);
        try {
            String[] values = csv.parseLine(replaceEscapeChars(line));
            int count = 0;
            for(String value :values) {
                ar.put(String.valueOf(count), StringUtils.defaultString(value.toString()));
                count++;
            }
        } catch (Exception e) {

        }
        return ar;

    }

    private String replaceEscapeChars(String line) {
        String newLine = StringUtils.replace(line,"\\n","\n");
        newLine = StringUtils.replace(newLine,"\\\"","\"\"");
        return newLine;
    }


}