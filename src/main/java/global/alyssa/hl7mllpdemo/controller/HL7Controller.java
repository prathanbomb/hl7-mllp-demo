package global.alyssa.hl7mllpdemo.controller;

import global.alyssa.hl7mllpdemo.service.HL7Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/hl7")
public class HL7Controller {

    @Autowired
    private HL7Service hl7Service;

    @PostMapping("/process")
    public ResponseEntity<?> processHL7Message(@RequestBody String hl7Message) {
        try {
            // Process the HL7 message
            String jsonResponse = hl7Service.processHL7Message(hl7Message);

            // Return the JSON response with HTTP 200 (OK)
            return ResponseEntity.ok(jsonResponse);
        } catch (Exception e) {
            // Return an error message with HTTP 400 (Bad Request)
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to process message",
                    "details", e.getMessage()
            ));
        }
    }
}