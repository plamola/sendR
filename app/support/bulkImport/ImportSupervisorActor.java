package support.bulkImport;


import akka.actor.*;
import akka.routing.RoundRobinRouter;
import models.Transformer;
import org.joda.time.*;
import play.Logger;
import support.FileImporter;
import support.Informer;
import support.bulkImport.workers.WebserviceWorkerActor;

import java.io.*;


public class ImportSupervisorActor extends UntypedActor {

    public enum Status {STARTING, RUNNING, PAUSING, PAUSED, RESUMING, STOPPING, STOPPED}

    private static final int MAX_TIMEOUTS = 100;

    private final SupervisorState supervisorState = new SupervisorState();
    private final String WORKERS = "workers";


    private static Transformer transformer;
    private boolean morePayloadAvailable = true;
    private final FileImporter fileImporter;

    public SupervisorState getStatus() {
        return supervisorState;
    }


    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof WorkerResult) {
            WorkerResult wr = (WorkerResult) message;
            handleReceivedWorkResult(wr);
            return;
        }  else {
            if (message instanceof SupervisorCommand) {
                SupervisorCommand command = (SupervisorCommand) message;
                handleSupervisorCommand(command);
                return;
            }
        }
        if (message instanceof String) {
            Logger.debug("Received message: " + message + " My status: " + supervisorState.getStatus().toString());
        } else {
            Logger.debug("Do not understand");
        }
    }


    private void handleSupervisorCommand(SupervisorCommand command) {
        switch (command.getStatus()) {
            case START:
                supervisorState.setStatus(Status.STARTING);
                sendMessageToInformer("Start request received");
                supervisorState.resetTimeOutCount();
                startWorkers();
                return;
            case PAUSE:
                if (supervisorState.getStatus() == Status.RUNNING) {
                    supervisorState.setStatus(Status.PAUSING);
                    sendMessageToInformer("Pause request received");
                    return;
                }
                if (supervisorState.getStatus() == Status.PAUSED) {
                    supervisorState.setStatus(Status.STARTING);
                    supervisorState.resetTimeOutCount();
                    sendMessageToInformer("Resume request received");
                    startWorkers();
                    return;
                }
                sendMessageToInformer("Unable to pause/resume");
                return;
            case RESUME:
                supervisorState.setStatus(Status.RESUMING);
                break;
            case STOP:
                supervisorState.setStatus(Status.STOPPING);
                break;
            case REPORT:
                sendMessageToInformer("");
                return;
        }
        sendMessageToInformer("Unhandled command:" + command.getStatus());
    }


    private void handleReceivedWorkResult(WorkerResult wr) {
        switch (wr.getStatus()) {
            case READY:
                supervisorState.incrementActiveWorkers();
                if (supervisorState.getActiveWorkers() == supervisorState.getWorkers()) {
                    supervisorState.setStatus(Status.RUNNING);
                    sendMessageToInformer("All workers have reported for duty.");
                }
                Logger.trace(self().toString() + " - reported for duty");
                sendNewPayLoad(getSender());
                break;
            case DONE:
                if (supervisorState.getStatus() == Status.STARTING) {
                    supervisorState.setStatus(Status.RUNNING);
                }
                supervisorState.incrementSuccesCount();

                Logger.trace("[" + supervisorState.getActiveWorkers() + "] " + sender().toString() + ": " + wr.getResult() + " (" + wr.getProcessingTime() + "ms)");
                if ((supervisorState.getSuccesCount() % 1000) == 0 && supervisorState.getSuccesCount() != 0) {
                    sendMessageToInformer(self().toString() + " - Did another 1000");
                    Logger.debug(DateTime.now().toString() + " [" + supervisorState.getActiveWorkers() + "] " + self().toString() + " - Success count: " + supervisorState.getSuccesCount());
                }
                sendNewPayLoad(getSender());
                break;
            case FAILED:
                if (supervisorState.getStatus() == Status.STARTING) {
                    supervisorState.setStatus(Status.RUNNING);
                }
                supervisorState.incrementFailureCount();
                Logger.debug("[" + supervisorState.getActiveWorkers() + "] " + self().toString() + " - Failure count: " + supervisorState.getFailureCount() + " - " + wr.getResult());
                writeToErrorFile(wr.getFailedInput());
                sendMessageToInformer("Error: " + wr.getResult());
                sendNewPayLoad(getSender());
                break;
            case TIMEOUT:
                if (supervisorState.getStatus() == Status.STARTING) {
                    supervisorState.setStatus(Status.RUNNING);
                }
                supervisorState.incrementTimeOutCount();
                Logger.debug("[" + supervisorState.getActiveWorkers() + "] " + self().toString() + " - Time-out - " + wr.getResult());
                if (supervisorState.getTimeOutcount() < (MAX_TIMEOUTS)) {
                    sendMessageToInformer("Time-out " + wr.getResult());
                    //retry the same payload instead of getting a new payload
                    Payload retry = new Payload(transformer.name, wr.getLineNumber(), wr.getFailedInput());
                    getSender().tell(retry, getSelf());
                } else {
                    if (supervisorState.getStatus() != Status.PAUSING) {
                        supervisorState.setStatus(Status.PAUSING);
                        sendMessageToInformer("To many time-outs, going to pause");
                    }
                    // To prevent loss of the current payload, write it to the error file
                    writeToErrorFile(wr.getFailedInput());
                    getSender().tell(PoisonPill.getInstance(), self());
                    return;
                }
                break;
            case SUICIDE:
                Logger.debug("Worker " + sender().path() + " committed suicide");
                supervisorState.decrementActiveWorkers();
                if (supervisorState.getActiveWorkers() == 0) {
                    supervisorState.setStatus(Status.STOPPED);
                    sendMessageToInformer("All workers stopped");
                    Logger.info(self().toString() + " - All workers stopped.");
                }
                break;
            default: {
            }
        }
    }


    private void sendNewPayLoad(ActorRef actor) {
        if (supervisorState.getStatus() != Status.STARTING && supervisorState.getStatus() != Status.RESUMING && supervisorState.getStatus() != Status.RUNNING) {
            // Not an active mode, must stop serving new payloads
            if (supervisorState.getStatus() == Status.STOPPING) {
                supervisorState.setStatus(Status.STOPPED);
                supervisorState.setStopTime(new DateTime());
            }
            if (supervisorState.getStatus() == Status.PAUSING) {
                supervisorState.setStatus(Status.PAUSED);
            }
            getSender().tell(PoisonPill.getInstance(), self());
            return;
        }
        if (morePayloadAvailable) {
            Payload payload = getNextPayload();
            Logger.trace(self().toString() + " - Got new payload");
            if (payload != null) {
                if (supervisorState.getFailureCount() == 0 && supervisorState.getSuccesCount() == 0) {
                    supervisorState.setCurrentFileSpecs(fileImporter.getCurrentFileName(),fileImporter.getNrOfLines());
                }
                Logger.trace(self().toString() + " - Sending payload to " + actor.toString());
                actor.tell(payload, getSelf());
                return;
            }
        }
        getSender().tell(PoisonPill.getInstance(), self());
    }

    private void sendMessageToInformer(String message) {
        Informer.getInstance().sendMessage(supervisorState, message);
    }


    private synchronized void writeToErrorFile(String line) {
        try {
            File file = new File(
                    String.format(
                            "%s.%s",
                            supervisorState.getCurrentFile().substring(0,
                                    supervisorState.getCurrentFile().lastIndexOf(".")),
                            "errors"));
            if (!file.exists()) {
                file.createNewFile();
            }
            OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(file.getAbsolutePath(), true), transformer.importFilecontentType);
            BufferedWriter bw =  new BufferedWriter(writer);
            bw.write(line);
            bw.newLine();
            bw.flush();
            bw.close();
        } catch (Exception e) {
            Logger.error("Problem writing to error file. Cause: " + e.getMessage());
            sendMessageToInformer("Problem writing to error file. Cause: " + e.getMessage());
            supervisorState.setStatus(Status.PAUSING);
        }
    }



    private synchronized Payload getNextPayload() {
        String line = fileImporter.getNextLine();
        if (line != null) {
            supervisorState.incrementPayloadCount();
            return new Payload(transformer.name, supervisorState.getPayloadCount(), line);
        } else {
            sendMessageToInformer("All available lines queued, nothing more to do");
            Logger.debug("All available lines queued, nothing more to do");
            morePayloadAvailable = false;
            return null;
        }
    }


    private void startWorkers() {
        ActorRef router = getContext().actorOf(new Props(new UntypedActorFactory() {
            public UntypedActor create() {
                return new WebserviceWorkerActor(self(), transformer);
            }
        }).withRouter(new RoundRobinRouter(supervisorState.getWorkers())), WORKERS);
        sendMessageToInformer("Starting workers");
    }


    @Override
    public void preStart() {
        supervisorState.setStartTime(new DateTime());
        supervisorState.setStatus(Status.STARTING);
        sendMessageToInformer("Supervisor ready");
        Logger.info(self().toString() + " - Supervisor ready");
    }

    @Override
    public void postStop() {
        DateTime endTime = new DateTime();
        supervisorState.setStatus(Status.STOPPED);
        supervisorState.setStopTime(endTime);
        String spendTime =
                Days.daysBetween(supervisorState.getStartTime(), endTime).getDays() + " days, " +
                        Hours.hoursBetween(supervisorState.getStartTime(), endTime).getHours() % 24 + " hours, " +
                        Minutes.minutesBetween(supervisorState.getStartTime(), endTime).getMinutes() % 60 + " minutes, " +
                        Seconds.secondsBetween(supervisorState.getStartTime(), endTime).getSeconds() % 60 + " seconds.";
        sendMessageToInformer("Stopped supervisor. Time spend: " + spendTime);
        Logger.info(self().toString() + " - Stopping supervisor. Time spend: " + spendTime);
    }


    public ImportSupervisorActor(int workers, Transformer tr) {
        transformer = tr;
        supervisorState.setWorkers(workers);
        supervisorState.setTransformerId(tr.id);
        supervisorState.setTransformerName(tr.name);
        this.fileImporter = new FileImporter(transformer);
        startWorkers();
    }
}
