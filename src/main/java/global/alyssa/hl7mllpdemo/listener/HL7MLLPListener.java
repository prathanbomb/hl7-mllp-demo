package global.alyssa.hl7mllpdemo.listener;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.PipeParser;
import global.alyssa.hl7mllpdemo.processor.HL7MessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

@Component
public class HL7MLLPListener implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(HL7MLLPListener.class);

    private static final char START_BLOCK = 0x0B; // Start of HL7 message
    private static final char END_BLOCK = 0x1C;   // End of HL7 message
    private static final char CARRIAGE_RETURN = 0x0D; // Carriage return

    private final HL7MessageProcessor processor;
    private final int port;

    @Autowired
    public HL7MLLPListener(@Value("${hl7.listener.port}") int port, HL7MessageProcessor processor) {
        this.port = port;
        this.processor = processor;
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
                processor.processMessage(parsedMessage); // Delegate to the processor
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