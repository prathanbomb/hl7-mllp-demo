package global.alyssa.hl7mllpdemo.controller;

import global.alyssa.hl7mllpdemo.service.HL7Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hl7")
public class HL7Controller {

    @Autowired
    private HL7Service hl7Service;

    @PostMapping("/send")
    public String sendHL7Message(@RequestBody String hl7Message) {
        try {
            String response = hl7Service.sendHL7Message(hl7Message, "localhost", 2575);
            return "Message successfully sent. Acknowledgment: " + response;
        } catch (Exception e) {
            return "Failed to send message: " + e.getMessage();
        }
    }
}