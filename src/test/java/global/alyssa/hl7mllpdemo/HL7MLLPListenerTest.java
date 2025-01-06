package global.alyssa.hl7mllpdemo;

import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.model.v23.message.ORU_R01;
import global.alyssa.hl7mllpdemo.processor.HL7MessageProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HL7MLLPListenerTest {

    @Test
    void testProcessORUMessage() throws Exception {
        // Sample HL7 Message
        String hl7Message = "MSH|^~\\&|LABOLINK||HIS||20151013104430||ORU^R01|1001|P|2.3\r" +
                "PID|1||000\r" +
                "OBR|1|||||20151013104427||||||||20151013104430\r" +
                "OBX|1|NM|SYSTOLIC||100|mmHg|||||F|||20151013104427\r" +
                "OBX|2|NM|DIASTOLIC||100|mmHg|||||F|||20151013104427\r" +
                "OBX|3|NM|PULSE||100|bpm|||||F|||20151013104427\r" +
                "OBX|4|NM|HEIGHT||150.0|cm|||||F|||20151013104428\r" +
                "OBX|5|NM|WEIGHT||50.0|kg|||||F|||20151013104428\r" +
                "OBX|6|NM|BMI||22.22222222222222|kg/m2|||||F|||20151013104428\r" +
                "OBX|7|NM|SPO2||95.0|%|||||F|||20151013104429\r" +
                "OBX|8|NM|TEMP||25.0|C|||||F|||20151013104429";

        // Parse HL7 Message
        PipeParser parser = new PipeParser();
        ORU_R01 oruMessage = (ORU_R01) parser.parse(hl7Message);

        // Create an instance of HL7MLLPListener (or the relevant class)
        HL7MessageProcessor messageProcessor = new HL7MessageProcessor();

        // Call the method under test
        messageProcessor.extractMessageData(oruMessage);

        // Assertions for key fields
        // MSH Assertions
        assertEquals("|", oruMessage.getMSH().getFieldSeparator().getValue());
        assertEquals("LABOLINK", oruMessage.getMSH().getSendingApplication().getNamespaceID().getValue());
        assertEquals("HIS", oruMessage.getMSH().getReceivingApplication().getNamespaceID().getValue());
        assertEquals("20151013104430", oruMessage.getMSH().getDateTimeOfMessage().encode());
        assertEquals("ORU^R01", oruMessage.getMSH().getMessageType().encode());

        // PID Assertions
        assertEquals("000", oruMessage.getRESPONSE().getPATIENT().getPID().getPatientIDInternalID(0).getID().getValue());

        // OBX Assertions
        assertEquals("SYSTOLIC", oruMessage.getRESPONSE().getORDER_OBSERVATION().getOBSERVATION(0).getOBX().getObservationIdentifier().getIdentifier().getValue());
        assertEquals("100", oruMessage.getRESPONSE().getORDER_OBSERVATION().getOBSERVATION(0).getOBX().getObservationValue(0).getData().encode());
        assertEquals("mmHg", oruMessage.getRESPONSE().getORDER_OBSERVATION().getOBSERVATION(0).getOBX().getUnits().encode());

        assertEquals("HEIGHT", oruMessage.getRESPONSE().getORDER_OBSERVATION().getOBSERVATION(3).getOBX().getObservationIdentifier().getIdentifier().getValue());
        assertEquals("150.0", oruMessage.getRESPONSE().getORDER_OBSERVATION().getOBSERVATION(3).getOBX().getObservationValue(0).getData().encode());
        assertEquals("cm", oruMessage.getRESPONSE().getORDER_OBSERVATION().getOBSERVATION(3).getOBX().getUnits().encode());

        // OBX Date Assertions
        assertEquals("20151013104427", oruMessage.getRESPONSE().getORDER_OBSERVATION().getOBSERVATION(0).getOBX().getDateTimeOfTheObservation().encode());
    }
}