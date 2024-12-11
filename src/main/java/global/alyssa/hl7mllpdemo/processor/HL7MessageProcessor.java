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

@Component
public class HL7MessageProcessor {

    private static final Logger log = LoggerFactory.getLogger(HL7MessageProcessor.class);

    public void processMessage(Message message) throws HL7Exception {
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
}