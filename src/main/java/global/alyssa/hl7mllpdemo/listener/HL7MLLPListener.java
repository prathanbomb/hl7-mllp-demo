package global.alyssa.hl7mllpdemo.listener;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v23.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v23.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v23.group.ORU_R01_PATIENT;
import ca.uhn.hl7v2.model.v23.message.ORU_R01;
import ca.uhn.hl7v2.model.v23.segment.MSH;
import ca.uhn.hl7v2.model.v23.segment.OBR;
import ca.uhn.hl7v2.model.v23.segment.OBX;
import ca.uhn.hl7v2.model.v23.segment.PID;
import ca.uhn.hl7v2.parser.PipeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class HL7MLLPListener implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(HL7MLLPListener.class);

    private static final char START_BLOCK = 0x0B; // Start of HL7 message
    private static final char END_BLOCK = 0x1C;   // End of HL7 message
    private static final char CARRIAGE_RETURN = 0x0D; // Carriage return

    private final int port;

    public HL7MLLPListener(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("MLLP Listener started on port {}", port);

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    log.info("Connection from {}", clientSocket.getRemoteSocketAddress());
                    processClientConnection(clientSocket);
                } catch (Exception e) {
                    log.error("Error handling client connection", e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to start MLLP Listener on port {}", port, e);
        }
    }

    private void processClientConnection(Socket clientSocket) throws IOException, HL7Exception {
        try (InputStream inputStream = clientSocket.getInputStream();
             OutputStream outputStream = clientSocket.getOutputStream()) {

            String hl7Message = readHL7Message(inputStream);
            log.debug("Raw HL7 Message: {}", hl7Message);

            PipeParser parser = new PipeParser();
            Message parsedMessage = parser.parse(sanitizeHL7Message(hl7Message));

            if (parsedMessage != null) {
                parseHL7Message(parsedMessage);
                String ackMessage = generateAckMessage(parsedMessage);
                sendAcknowledgment(outputStream, ackMessage, clientSocket);
            } else {
                log.warn("Received invalid HL7 message. No acknowledgment sent.");
            }
        }
    }

    private String readHL7Message(InputStream inputStream) throws IOException {
        StringBuilder messageBuilder = new StringBuilder();
        int character;
        while ((character = inputStream.read()) != -1) {
            if (character == END_BLOCK) break; // End of message
            messageBuilder.append((char) character);
        }
        return messageBuilder.toString();
    }

    private String sanitizeHL7Message(String hl7Message) {
        if (hl7Message.charAt(0) == START_BLOCK) {
            hl7Message = hl7Message.substring(1); // Strip Start Block
        }
        if (hl7Message.endsWith(String.valueOf(END_BLOCK) + CARRIAGE_RETURN)) {
            hl7Message = hl7Message.substring(0, hl7Message.length() - 2); // Strip End Block and Carriage Return
        }
        return hl7Message;
    }

    private void parseHL7Message(Message message) {
        try {
            if (message instanceof ORU_R01 oruMessage) {
                processORUMessage(oruMessage);
            } else {
                log.warn("Unsupported message type: {}", message.getName());
            }
        } catch (Exception e) {
            log.error("Error processing HL7 message", e);
        }
    }

    private void processORUMessage(ORU_R01 oruMessage) {
        try {
            MSH msh = oruMessage.getMSH();
            log.info("Processing ORU_R01: Message Control ID = {}", msh.getMessageControlID().getValue());

            ORU_R01_PATIENT patient = oruMessage.getRESPONSE().getPATIENT();
            PID pid = patient.getPID();
            String patientId = pid.getPatientIDInternalID(0).getID().getValue();
            log.info("Patient ID (PID-3): {}", patientId);

            ORU_R01_ORDER_OBSERVATION orderObservation = oruMessage.getRESPONSE().getORDER_OBSERVATION();
            OBR obr = orderObservation.getOBR();
            log.info("Observation Request ID (OBR-1): {}", obr.getSetIDObservationRequest().getValue());

            for (int i = 0; i < orderObservation.getOBSERVATIONReps(); i++) {
                ORU_R01_OBSERVATION observation = orderObservation.getOBSERVATION(i);
                OBX obx = observation.getOBX();
                log.info("Observation [{}]: {} = {} {}",
                        i + 1,
                        obx.getObservationIdentifier().getIdentifier().getValue(),
                        obx.getObservationValue(0).getData().encode(),
                        obx.getUnits().encode());
            }
        } catch (Exception e) {
            log.error("Failed to process ORU_R01 message", e);
        }
    }

    private String generateAckMessage(Message incomingMessage) {
        try {
            Message ack = incomingMessage.generateACK();
            return START_BLOCK + ack.encode() + END_BLOCK + CARRIAGE_RETURN;
        } catch (Exception e) {
            log.error("Error generating acknowledgment message", e);
            return START_BLOCK + "MSH|^~\\&|ACK||202201011200||ACK^A01|123|P|2.3\rMSA|AE|123" + END_BLOCK + CARRIAGE_RETURN;
        }
    }

    private void sendAcknowledgment(OutputStream outputStream, String ackMessage, Socket clientSocket) throws IOException {
        log.info("Sending ACK to {}", clientSocket.getRemoteSocketAddress());
        outputStream.write(ackMessage.getBytes());
        outputStream.flush();
        log.debug("ACK sent successfully.");
    }
}