package global.alyssa.hl7mllpdemo.service;

import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.Socket;

@Service
public class HL7Service {

    public String sendHL7Message(String hl7Message, String host, int port) throws Exception {
        // Add MLLP framing characters
        String framedMessage = "\u000B" + hl7Message + "\u001C\r";

        try (Socket socket = new Socket(host, port);
             OutputStream outputStream = socket.getOutputStream();
             var inputStream = socket.getInputStream()) {

            // Send HL7 message
            outputStream.write(framedMessage.getBytes());
            outputStream.flush();

            // Read ACK response
            byte[] buffer = new byte[1024];
            int bytesRead = inputStream.read(buffer);
            return new String(buffer, 0, bytesRead);
        }
    }
}