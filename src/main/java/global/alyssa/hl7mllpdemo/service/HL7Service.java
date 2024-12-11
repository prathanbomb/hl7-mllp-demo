package global.alyssa.hl7mllpdemo.service;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.PipeParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import global.alyssa.hl7mllpdemo.processor.HL7MessageProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HL7Service {

    @Autowired
    private HL7MessageProcessor processor;
    @Autowired
    private ObjectMapper objectMapper;

    public String processHL7Message(String hl7Message) throws Exception {
        // Normalize HL7 message
        hl7Message = processor.normalizeHL7Message(hl7Message);

        // Parse HL7 message
        PipeParser parser = new PipeParser();
        Message parsedMessage = parser.parse(hl7Message);

        // Use processor to extract details
        var messageData = processor.extractMessageData(parsedMessage);

        // Convert extracted data to JSON
        return objectMapper.writeValueAsString(messageData);
    }
}