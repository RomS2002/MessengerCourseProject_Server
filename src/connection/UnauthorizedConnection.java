package connection;

import messages.MessengerUser;
import util.ConnectionListener;
import util.Main;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;

public class UnauthorizedConnection extends Connection {

    private final Scanner scanner = new Scanner(in);
    private boolean isRunning = true;

    public UnauthorizedConnection(Socket socket) throws IOException {
        super(socket);
    }

    private String hashPassword(String password) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch(NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return Base64.getEncoder().encodeToString(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
    }

    private void login(String username, String password) {

        for(MessengerUser user : Main.users) {
            if(user.getUserName().equals(username)) {
                if(!user.getHashedPassword().equals(hashPassword(password))) {
                    try {
                        out.write("Invalid password\n".getBytes(StandardCharsets.UTF_8));
                        out.flush();
                    } catch(IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                try {
                    out.write("OK\n".getBytes(StandardCharsets.UTF_8));
                    out.flush();

                    System.out.println("Успешно выполнен вход пользователя " + username);
                } catch(IOException e) {
                    throw new RuntimeException(e);
                }

                AuthorizedConnection authorizedConnection;
                try {
                    authorizedConnection = new AuthorizedConnection(socket, in, out, user);
                } catch(IOException e) {
                    throw new RuntimeException(e);
                }
                ConnectionListener.getUnauthorizedConnections().remove(this);
                ConnectionListener.getAuthorizedConnections().add(authorizedConnection);
                authorizedConnection.start();
                isRunning = false;
                return;
            }
        }

        try {
            out.write("Invalid username\n".getBytes(StandardCharsets.UTF_8));
            out.flush();

        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createUser(String username, String password) {
        for(MessengerUser user : Main.users) {
            if(user.getUserName().equals(username)) {
                try {
                    out.write("User already exists\n".getBytes(StandardCharsets.UTF_8));
                    out.flush();
                } catch(IOException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
        }
        try {
            out.write("OK\n".getBytes(StandardCharsets.UTF_8));
            out.flush();

            System.out.println("Успешно создан пользователь " + username);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        int id = Main.getNextUserID();
        String hashedPassword = hashPassword(password);
        MessengerUser newUser = new MessengerUser(id, username, hashedPassword);
        Main.users.add(newUser);

        login(username, password);
    }

    @Override
    public void run() {
        try {
            while(isRunning) {
                String command = scanner.next();
                String username;
                String password;

                switch(command) {
                    case "createUser":
                        username = scanner.next();
                        password = scanner.next();
                        createUser(username, password);
                        break;
                    case "login":
                        username = scanner.next();
                        password = scanner.next();
                        login(username, password);
                        break;
                    case "disconnect":
                        isRunning = false;
                        close();
                        break;
                }
            }
        } catch(RuntimeException e) {
            System.out.println("Соединение с клиентом было разорвано!");
            close();
        }
    }
}
