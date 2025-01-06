package global.alyssa.hl7mllpdemo.processor;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Composite;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Primitive;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.v23.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v23.message.ORU_R01;
import ca.uhn.hl7v2.model.v23.segment.MSH;
import ca.uhn.hl7v2.model.v23.segment.OBX;
import ca.uhn.hl7v2.model.v23.segment.PID;
import ca.uhn.hl7v2.parser.PipeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class HL7MessageProcessor {

    private static final Logger log = LoggerFactory.getLogger(HL7MessageProcessor.class);

    private static final char START_BLOCK = 0x0B;
    private static final char END_BLOCK = 0x1C;
    private static final char CARRIAGE_RETURN = 0x0D;

    public String processMessage(String hl7Message) {
        try {
            PipeParser parser = new PipeParser();
            Message message = parser.parse(hl7Message);
            Map<String, Object> messageData = extractMessageData(message);

            logMessageDetails(messageData);

            return generateAckMessage(message, messageData.get("messageControlId").toString());
        } catch (HL7Exception e) {
            log.error("Failed to process message", e);
            return manuallyConstructDefaultErrorAck("UNKNOWN");
        }
    }

    private void logMessageDetails(Map<String, Object> messageData) {
        log.info("Processing Message: Control ID = {}", messageData.get("messageControlId"));
        log.info("Patient ID: {}", messageData.get("patientId"));

        List<Map<String, String>> observations = (List<Map<String, String>>) messageData.get("observations");
        for (int i = 0; i < observations.size(); i++) {
            Map<String, String> observation = observations.get(i);
            log.info("Observation [{}]: {} = {} {}",
                    i + 1,
                    observation.get("type"),
                    observation.get("value"),
                    observation.get("units"));
        }
    }

    public Map<String, Object> extractMessageData(Message message) throws HL7Exception {
        if (!(message instanceof ORU_R01 oruMessage)) {
            throw new HL7Exception("Unsupported message type: " + message.getName());
        }

        Map<String, Object> messageData = new HashMap<>();
        MSH msh = oruMessage.getMSH();

        messageData.put("messageControlId", getFieldValue(msh.getMessageControlID()));
        messageData.put("sendingApplication", getFieldValue(msh.getSendingApplication().getNamespaceID()));
        messageData.put("sendingFacility", getFieldValue(msh.getSendingFacility().getNamespaceID()));
        messageData.put("receivingApplication", getFieldValue(msh.getReceivingApplication().getNamespaceID()));
        messageData.put("receivingFacility", getFieldValue(msh.getReceivingFacility().getNamespaceID()));
        messageData.put("dateTimeOfMessage", getFieldValue(msh.getDateTimeOfMessage()));

        PID pid = oruMessage.getRESPONSE().getPATIENT().getPID();
        messageData.put("patientId", getFieldValue(pid.getPatientIDInternalID(0).getID()));

        messageData.put("observations", extractObservations(oruMessage));
        return messageData;
    }

    private List<Map<String, String>> extractObservations(ORU_R01 oruMessage) {
        List<Map<String, String>> observations = new ArrayList<>();
        ORU_R01_ORDER_OBSERVATION orderObservation = oruMessage.getRESPONSE().getORDER_OBSERVATION();

        for (int i = 0; i < orderObservation.getOBSERVATIONReps(); i++) {
            try {
                OBX obx = orderObservation.getOBSERVATION(i).getOBX();
                Map<String, String> observationData = new HashMap<>();
                observationData.put("type", getFieldValue(obx.getObservationIdentifier().getIdentifier()));
                observationData.put("value", getFieldValue(obx.getObservationValue(0).getData()));
                observationData.put("units", getFieldValue(obx.getUnits()));
                observationData.put("observationDateTime", getFieldValue(obx.getDateTimeOfTheObservation()));
                observations.add(observationData);
            } catch (Exception e) {
                log.warn("Failed to extract OBX segment data at index {}", i, e);
            }
        }

        return observations;
    }

    public String normalizeHL7Message(String hl7Message) {
        return hl7Message != null && hl7Message.contains("\n") && !hl7Message.contains("\r")
                ? hl7Message.replace("\n", "\r")
                : hl7Message;
    }

    private String getFieldValue(Type field) {
        if (field == null) return "";
        try {
            if (field instanceof Primitive primitive) {
                return primitive.getValue() != null ? primitive.getValue() : "";
            } else if (field instanceof Composite composite) {
                return Arrays.stream(composite.getComponents())
                        .map(this::getFieldValue)
                        .filter(value -> !value.isEmpty())
                        .collect(Collectors.joining(" "));
            }
            return field.encode();
        } catch (HL7Exception e) {
            log.warn("Failed to encode field", e);
            return "";
        }
    }

    private String generateAckMessage(Message incomingMessage, String controlId) {
        try {
            Message ack = incomingMessage.generateACK();
            return START_BLOCK + ack.encode() + END_BLOCK + CARRIAGE_RETURN;
        } catch (Exception e) {
            log.error("Error generating acknowledgment message", e);
            return manuallyConstructDefaultErrorAck(controlId);
        }
    }

    private String manuallyConstructDefaultErrorAck(String controlId) {
        controlId = controlId != null && !controlId.isEmpty() ? controlId : "UNKNOWN";
        return START_BLOCK +
                String.format("MSH|^~\\&|ACK_SOURCE|ACK_DEST|%s|ACK||ACK^A01|%s|P|2.3\rMSA|AE|%s|Application Error\r",
                        getCurrentTimestamp(), controlId, controlId) +
                END_BLOCK + CARRIAGE_RETURN;
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }
}