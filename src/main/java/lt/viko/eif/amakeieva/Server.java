package lt.viko.eif.amakeieva;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(7070);
        System.out.println("The server is listening on port 7070");

        Scanner console = new Scanner(System.in); // For reading input from console

        while (true) {
            try (Socket socket = serverSocket.accept();
                 BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                String publicKey = input.readLine();
                String message = input.readLine();
                String originalSignature = input.readLine();

                if (publicKey == null || message == null || originalSignature == null) {
                    System.out.println("Received incomplete data, skipping...");
                    continue;
                }

                System.out.println("Received from a client:");
                System.out.println("Public Key: " + publicKey);
                System.out.println("Message: " + message);
                System.out.println("Original Signature: " + originalSignature);

                // Check if the client wants to modify the signature
                if (wantToModifySignature(publicKey)) {
                    String modifiedSignature = modifySignature(originalSignature);
                    if (modifiedSignature != null) {
                        originalSignature = modifiedSignature;
                    }
                }

                // Verify the modified or original signature
                boolean result = verifySignature(message, originalSignature, publicKey);

                // Send the verification result to the client
                sendResultToClient(socket, result);

                // Send modified signature to receiver
                sendModifiedSignatureToReceiver(publicKey, originalSignature, message);

            } catch (IOException e) {
                System.out.println("Error handling client: " + e.getMessage());
            }
        }
    }
    private static String modifySignature(String originalSignature) {
        Scanner console = new Scanner(System.in);
        System.out.println("Enter new signature (as a space-separated series of integers):");
        String modifiedSignature = console.nextLine();
        // Validate the modified signature
        if (!modifiedSignature.matches("^\\d+( \\d+)*$")) {
            System.out.println("Invalid format for signature. Using original signature.");
            return null;
        }
        return modifiedSignature;
    }

    private static void sendResultToClient(Socket socket, boolean result) {
        try (PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) {
            if (result) {
                output.println("Signature verification successful");
            } else {
                output.println("Signature verification failed");
                // Return null values for public key, message, and original signature
                output.println("null");
                output.println("null");
                output.println("null");
            }
        } catch (IOException e) {
            System.out.println("Error sending result to client: " + e.getMessage());
        }
    }

    private static void sendModifiedSignatureToReceiver(String publicKey, String modifiedSignature, String message) {
        try (Socket receiverSocket = new Socket("localhost", 3031);
             PrintWriter receiverOutput = new PrintWriter(receiverSocket.getOutputStream(), true)) {
            receiverOutput.println(publicKey);
            receiverOutput.println(modifiedSignature);
            receiverOutput.println(message);
            System.out.println("Modified signature sent to receiver.");
        } catch (IOException e) {
            System.out.println("Error sending modified signature to receiver: " + e.getMessage());
        }
    }


    private static boolean wantToModifySignature(String publicKey) {
        return !publicKey.equalsIgnoreCase("Signature verification successful");
    }

    private static boolean verifySignature(String message, String signature, String publicKeyStr) {
        try {
            String[] keys = publicKeyStr.split(",");
            BigInteger e = new BigInteger(keys[0]);
            BigInteger n = new BigInteger(keys[1]);
            String[] signatureParts = signature.split(" ");

            StringBuilder decryptedMessageBuilder = new StringBuilder();
            for (String part : signatureParts) {
                BigInteger sigPart = new BigInteger(part);
                BigInteger decryptedChar = sigPart.modPow(e, n);
                decryptedMessageBuilder.append((char) decryptedChar.intValue());
            }
            return decryptedMessageBuilder.toString().equals(message);
        } catch (NumberFormatException ex) {
            System.out.println("Invalid signature format: " + ex.getMessage());
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
