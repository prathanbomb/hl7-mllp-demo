package global.alyssa.hl7mllpdemo.processor;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HL7MessageProcessor {

    private static final Logger log = LoggerFactory.getLogger(HL7MessageProcessor.class);

    public void processMessage(Message message) {
        if (message instanceof ORU_R01 oruMessage) {
            processORUMessage(oruMessage);
        } else {
            log.warn("Unsupported message type: {}", message.getName());
        }
    }

    public void processORUMessage(ORU_R01 oruMessage) {
        try {
            // Process MSH Segment
            MSH msh = oruMessage.getMSH();
            log.info("Processing ORU_R01: Message Control ID = {}", msh.getMessageControlID().getValue());

            // Process PID Segment
            ORU_R01_PATIENT patient = oruMessage.getRESPONSE().getPATIENT();
            PID pid = patient.getPID();
            String patientId = pid.getPatientIDInternalID(0).getID().getValue();
            log.info("Patient ID (PID-3): {}", patientId);

            // Process OBR Segment
            ORU_R01_ORDER_OBSERVATION orderObservation = oruMessage.getRESPONSE().getORDER_OBSERVATION();
            OBR obr = orderObservation.getOBR();
            log.info("Observation Request ID (OBR-1): {}", obr.getSetIDObservationRequest().getValue());

            // Process OBX Segments
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

    public String normalizeHL7Message(String hl7Message) {
        // If the message contains \n but not \r, replace \n with \r
        if (hl7Message.contains("\n") && !hl7Message.contains("\r")) {
            return hl7Message.replace("\n", "\r");
        }
        return hl7Message;
    }

    public Map<String, Object> extractMessageData(Message message) throws HL7Exception {
        Map<String, Object> messageData = new HashMap<>();

        if (message instanceof ORU_R01 oruMessage) {
            // Extract MSH Segment
            MSH msh = oruMessage.getMSH();
            messageData.put("messageControlId", msh.getMessageControlID().getValue());
            messageData.put("sendingApplication", msh.getSendingApplication().getNamespaceID().getValue());
            messageData.put("sendingFacility", msh.getSendingFacility().getNamespaceID().getValue());
            messageData.put("receivingApplication", msh.getReceivingApplication().getNamespaceID().getValue());
            messageData.put("receivingFacility", msh.getReceivingFacility().getNamespaceID().getValue());
            messageData.put("dateTimeOfMessage", msh.getDateTimeOfMessage().encode());

            // Extract PID Segment
            ORU_R01_PATIENT patient = oruMessage.getRESPONSE().getPATIENT();
            PID pid = patient.getPID();
            messageData.put("patientId", pid.getPatientIDInternalID(0).getID().getValue());

            // Extract Observations (OBX Segments)
            ORU_R01_ORDER_OBSERVATION orderObservation = oruMessage.getRESPONSE().getORDER_OBSERVATION();
            List<Map<String, String>> observations = new ArrayList<>();

            for (int i = 0; i < orderObservation.getOBSERVATIONReps(); i++) {
                ORU_R01_OBSERVATION observation = orderObservation.getOBSERVATION(i);
                OBX obx = observation.getOBX();

                Map<String, String> observationData = new HashMap<>();
                observationData.put("type", obx.getObservationIdentifier().getIdentifier().getValue());
                observationData.put("value", obx.getObservationValue(0).getData().encode());
                observationData.put("units", obx.getUnits().encode());
                observationData.put("observationDateTime", obx.getDateTimeOfTheObservation().encode());

                observations.add(observationData);
            }

            messageData.put("observations", observations);

        } else {
            log.warn("Unsupported message type: {}", message.getName());
            throw new HL7Exception("Unsupported message type: " + message.getName());
        }

        return messageData;
    }
}