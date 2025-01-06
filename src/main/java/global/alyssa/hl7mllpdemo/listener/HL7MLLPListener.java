package global.alyssa.hl7mllpdemo.listener;

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

    private static final char START_BLOCK = 0x0B;
    private static final char END_BLOCK = 0x1C;
    private static final char CARRIAGE_RETURN = 0x0D;

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
                handleClientConnection(serverSocket.accept());
            }
        } catch (IOException e) {
            log.error("Failed to start MLLP Listener on port {}", port, e);
        }
    }

    private void handleClientConnection(Socket clientSocket) {
        try (clientSocket) {
            log.info("Connection established with {}", clientSocket.getRemoteSocketAddress());
            processClientConnection(clientSocket);
        } catch (Exception e) {
            log.error("Error handling client connection", e);
        }
    }

    private void processClientConnection(Socket clientSocket) throws IOException {
        try (InputStream inputStream = clientSocket.getInputStream();
             OutputStream outputStream = clientSocket.getOutputStream()) {

            String hl7Message = readHL7Message(inputStream);
            log.debug("Received raw HL7 message: {}", hl7Message);

            if (hl7Message == null || hl7Message.isBlank()) {
                log.warn("Empty or invalid HL7 message received. No acknowledgment sent.");
                return;
            }

            hl7Message = processor.normalizeHL7Message(hl7Message);
            String ackMessage = processor.processMessage(hl7Message);
            sendAcknowledgment(outputStream, ackMessage, clientSocket);
        } catch (Exception e) {
            log.error("Error processing client connection", e);
        }
    }

    private String readHL7Message(InputStream inputStream) throws IOException {
        StringBuilder messageBuilder = new StringBuilder();
        int character;
        while ((character = inputStream.read()) != -1) {
            if (character == END_BLOCK) break;
            messageBuilder.append((char) character);
        }
        return sanitizeHL7Message(messageBuilder.toString());
    }

    private String sanitizeHL7Message(String hl7Message) {
        if (hl7Message == null || hl7Message.isBlank()) return null;
        hl7Message = hl7Message.strip();
        if (hl7Message.charAt(0) == START_BLOCK) {
            hl7Message = hl7Message.substring(1);
        }
        if (hl7Message.endsWith(String.valueOf(END_BLOCK) + CARRIAGE_RETURN)) {
            hl7Message = hl7Message.substring(0, hl7Message.length() - 2);
        }
        return hl7Message;
    }

    private void sendAcknowledgment(OutputStream outputStream, String ackMessage, Socket clientSocket) throws IOException {
        log.info("Sending ACK to {}", clientSocket.getRemoteSocketAddress());
        outputStream.write(ackMessage.getBytes());
        outputStream.flush();
        log.debug("ACK sent successfully.");
    }
}