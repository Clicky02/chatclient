package com.networking.chatclient;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import com.networking.chatclient.ClientProtocol.GroupAction;
import com.networking.chatclient.ClientProtocol.MessageAction;
import com.networking.chatclient.ServerProtocol.ServerCommand;

/*
 * Hello world!
 */
public class WebClient {
    /*
     * Structure for containing a message. All properties are immutable except
     * content.
     * Content is not immutable so that it can be loaded at a later time.
     */

    public class Message {
        public final int groupId;
        public final int messageId;
        public final String username;
        public final String postDate;
        public final String subject;
        private String content = null;
        private boolean loaded = false;

        public Message(int groupId, int messageId, String username, String postDate, String subject) {
            this.groupId = groupId;
            this.messageId = messageId;
            this.username = username;
            this.postDate = postDate;
            this.subject = subject;
        }

        public void setContent(String content) {
            this.content = content;
            this.loaded = true;
        }

        public String getContent() {
            return content;
        }

        public boolean isLoaded() {
            return loaded;
        }
    }

    /*
     * Structure for containing a group.
     */

    public class Group {
        public final int id;
        public String name;
        public final ArrayList<String> users = new ArrayList<String>();
        public final HashMap<Integer, Message> messages = new HashMap<Integer, Message>();

        public Group(int id) {
            this.id = id;
        }

        public Group(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /*
     * Events and Payload classes
     * 
     * In our implementation, the UI and the server response handler run in
     * different threads. These events are used to pass information from the server
     * responses to the UI.
     * 
     * I did not need to declare a payload for all events, only the ones that did
     * not have one parameter, but I still did so for uniformity and readability.
     */

    public class UsernameVerifyEventPayload {
        boolean isValid;
    }

    ClientEvent<UsernameVerifyEventPayload> usernameVerifyEvent = new ClientEvent<UsernameVerifyEventPayload>(this);

    public class UserJoinEventPayload {
        Group group;
        String username;
    }

    ClientEvent<UserJoinEventPayload> userJoinEvent = new ClientEvent<UserJoinEventPayload>(this);

    public class UserLeaveEventPayload {
        Group group;
        String username;
    }

    ClientEvent<UserLeaveEventPayload> userLeaveEvent = new ClientEvent<UserLeaveEventPayload>(this);

    public class ReceiveMessageLabelEventPayload {
        Message labelMessage;
    }

    ClientEvent<ReceiveMessageLabelEventPayload> receiveMessageLabelEvent = new ClientEvent<ReceiveMessageLabelEventPayload>(
            this);

    public class ReceiveUserListEventPayload {
        Group group;
    }

    ClientEvent<ReceiveUserListEventPayload> receiveUserListEvent = new ClientEvent<ReceiveUserListEventPayload>(this);

    public class ReceiveGroupListEventPayload {
    }

    ClientEvent<ReceiveGroupListEventPayload> receiveGroupListEvent = new ClientEvent<ReceiveGroupListEventPayload>(
            this);

    public class ReceiveMessageContentEventPayload {
        int groupId;
        int messageId;
        Message message; // Will be null if validId is false or if it could not find the message label
        boolean validId;
    }

    ClientEvent<ReceiveMessageContentEventPayload> receiveMessageContentEvent = new ClientEvent<ReceiveMessageContentEventPayload>(
            this);

    /*
     * State variables
     */

    Socket socket;
    DataOutputStream outputStream;

    Thread responseHandlerThread; // The thread that will listen to the server
    Thread interfaceThread; // The thread that will run the interface and send messages to the server

    boolean joined; // Whether the user has joined the server
    String username; // The user's username
    ArrayList<Integer> userGroups = new ArrayList<Integer>(); // A list of the ids that the user is in

    HashMap<Integer, Group> groups = new HashMap<Integer, Group>(); // A hashmap of all the information known about each
                                                                    // group.

    /*
     * The main line of execution.
     */
    public static void main(String[] args) {
        WebClient client = new WebClient();
        client.start();
    }

    /*
     * Starts the client.
     */
    public void start() {
        UserInterface interf = new CommandLineInterface(this);
        interfaceThread = new Thread(interf);
        interfaceThread.start();
    }

    /*
     * Connects to the server, begins the response handler thread.
     */
    public void connect(String host, int port) throws UnknownHostException, IOException {
        socket = new Socket(host, port);
        outputStream = new DataOutputStream(socket.getOutputStream());

        responseHandlerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream is = socket.getInputStream();
                    Scanner scanner = new Scanner(is);
                    scanner.useDelimiter("\0");

                    while (scanner.hasNext()) {
                        String message = scanner.next();

                        if (message.equalsIgnoreCase(""))
                            continue;

                        final ProtocolPacket packet = ServerProtocol.parseResponse(message);

                        (new Thread(() -> {
                            handleResponse(packet);
                        })).start();
                    }

                    scanner.close();
                } catch (IOException e) {
                    System.out.println("Socket Reading Failed");
                }

            }
        });

        responseHandlerThread.start();
    }

    /*
     * Handles a response packet.
     */
    public synchronized void handleResponse(ProtocolPacket packet) {

        ServerCommand command = ServerProtocol.getServerCommand(packet);

        if (command == null) {
            System.out.println("Invalid Server Packet Received : Invalid Command");
            System.out.println("Command: " + packet.command);
            return;
        } else if (packet.parameters.size() < command.minParameters) {
            System.out.println("Invalid Server Packet Received : Too few parameter");
            System.out.println("Command: " + packet.command);
            System.out.println(
                    "Expected " + command.minParameters + " parameters, received " + packet.parameters.size() + ".");
            return;
        }

        switch (command) {
            case BAD_MESSAGE:
                System.out.println("Something went wrong");
                return;
            case SEND_GROUPS_LIST: {
                String[] names = packet.parameters.get(0).split(",");
                String[] ids = packet.parameters.get(1).split(",");

                for (int i = 0; i < ids.length; i++) {
                    int id = Integer.parseInt(ids[i]);
                    if (!groups.containsKey(id)) {
                        groups.put(id, new Group(Integer.parseInt(ids[i]), names[i]));
                    } else {

                    }
                }

                receiveGroupListEvent.invoke(new ReceiveGroupListEventPayload());
                return;
            }
            case SEND_MESSAGE_CONTENT: {
                int groupId = Integer.parseInt(packet.parameters.get(0));
                int messageId = Integer.parseInt(packet.parameters.get(1));
                String content = packet.parameters.get(2);
                boolean validId = (packet.parameters.get(3).equals("1"));

                Message m = null;

                if (validId) {
                    m = getSavedMessage(groupId, messageId);

                    if (m != null)
                        m.setContent(content);
                }

                ReceiveMessageContentEventPayload payload = new ReceiveMessageContentEventPayload();
                payload.groupId = groupId;
                payload.messageId = messageId;
                payload.message = m;
                payload.validId = validId;

                receiveMessageContentEvent.invoke(payload);
                break;
            }
            case SEND_MESSAGE_LABEL: {
                int groupId = Integer.parseInt(packet.parameters.get(0));
                int messageId = Integer.parseInt(packet.parameters.get(1));
                String username = packet.parameters.get(2);
                String postDate = packet.parameters.get(3);
                String subject = packet.parameters.get(4);

                if (!groups.containsKey(groupId))
                    return; // Ignore messages from groups that we are not a part of

                Message m = new Message(groupId, messageId, username, postDate, subject);
                groups.get(groupId).messages.put(messageId, m);

                ReceiveMessageLabelEventPayload payload = new ReceiveMessageLabelEventPayload();
                payload.labelMessage = m;

                receiveMessageLabelEvent.invoke(payload);
                return;
            }
            case SEND_USER_LIST: {
                int groupId = Integer.parseInt(packet.parameters.get(0));

                String[] usernames = packet.parameters.size() > 1 ? packet.parameters.get(1).split(",")
                        : new String[] {}; // If there is not a second paramter, there are no users, so create an empty
                                           // list

                if (!groups.containsKey(groupId))
                    retrieveGroups(); // Ignore messages from groups that we are not a part of

                Group g = groups.get(groupId);

                g.users.clear();

                for (String name : usernames) {
                    g.users.add(name);
                }

                ReceiveUserListEventPayload payload = new ReceiveUserListEventPayload();
                payload.group = g;

                receiveUserListEvent.invoke(payload);
                break;
            }
            case USER_JOIN_NOTIF: {
                int groupId = Integer.parseInt(packet.parameters.get(0));
                String username = packet.parameters.get(1);

                if (!groups.containsKey(groupId))
                    retrieveGroups();

                Group g = groups.get(groupId);

                g.users.add(username);

                UserJoinEventPayload payload = new UserJoinEventPayload();
                payload.group = g;
                payload.username = username;

                userJoinEvent.invoke(payload);
                return;
            }
            case USER_LEAVE_NOTIF: {
                int groupId = Integer.parseInt(packet.parameters.get(0));
                String username = packet.parameters.get(1);

                if (!groups.containsKey(groupId))
                    retrieveGroups();

                Group g = groups.get(groupId);

                g.users.remove(username);

                UserLeaveEventPayload payload = new UserLeaveEventPayload();
                payload.group = g;
                payload.username = username;

                userLeaveEvent.invoke(payload);
                break;
            }
            case VERIFY_USERNAME: {
                boolean success = (packet.parameters.get(0).equals("1"));

                if (success) {
                    joined = true;
                }

                UsernameVerifyEventPayload payload = new UsernameVerifyEventPayload();
                payload.isValid = success;

                usernameVerifyEvent.invoke(payload);
                return;
            }
            default:
                break;

        }

    }

    public synchronized boolean join(String username) {
        if (!joined) {
            this.username = username;
            ClientProtocol.createJoinPacket(username).send(outputStream);

            UsernameVerifyEventPayload payload = usernameVerifyEvent.waitForEvent();

            if (payload.isValid) { // Join default group on join
                groups.put(0, new Group(0, "Global"));
                userGroups.add(0);
            }

            return payload.isValid;
        }
        return false;

    }

    public void postMessage(int groupId, String subject, String content) {
        if (joined) {
            ClientProtocol.createMessagePacket(MessageAction.POST, groupId, -1, subject, content).send(outputStream);
        }
    }

    public synchronized Message retrieveMessage(int groupId, int messageId) {
        if (joined) {
            requestMessage(groupId, messageId);

            ReceiveMessageContentEventPayload messagePayload = null;
            while (messagePayload == null || messagePayload.groupId != groupId
                    || messagePayload.messageId != messageId) {
                messagePayload = receiveMessageContentEvent.waitForEvent();
            }

            return messagePayload.message;
        }

        return null;

    }

    public void requestMessage(int groupId, int messageId) {
        ClientProtocol.createMessagePacket(MessageAction.RETRIEVE, groupId, messageId, "", "")
                .send(outputStream);
    }

    public void joinGroup(int groupId) {
        if (joined) {
            userGroups.add(groupId);
            ClientProtocol.createGroupPacket(GroupAction.JOIN, groupId).send(outputStream);
        }
    }

    public void leaveGroup(int groupId) {
        if (joined) {
            userGroups.remove(groupId);
            ClientProtocol.createGroupPacket(GroupAction.LEAVE, groupId).send(outputStream);
        }
    }

    public void logOut() {
        if (joined) {
            ClientProtocol.createLeavePacket().send(outputStream);

            // Reset server info
            joined = false;
            username = null;
            userGroups.clear();
            groups.clear();
        }
    }

    public synchronized Group retrieveGroupUsers(int groupId) {

        if (joined) {
            requestGroupUsers(groupId);

            Group retrievedGroup = null;

            while (retrievedGroup == null || retrievedGroup.id != groupId) {
                retrievedGroup = receiveUserListEvent.waitForEvent().group;
            }

            return retrievedGroup;
        }

        return null;

    }

    public void requestGroupUsers(int groupId) {
        ClientProtocol.createGroupPacket(GroupAction.USERS, groupId).send(outputStream);
    }

    public synchronized ArrayList<Group> retrieveGroups() {
        if (joined) {
            requestGroups();
            receiveGroupListEvent.waitForEvent();
            return new ArrayList<Group>(groups.values());
        }

        return null;
    }

    public void requestGroups() {
        if (joined) {
            ClientProtocol.createGroupPacket(GroupAction.LIST, -1).send(outputStream);
        }
    }

    public void disconnect() throws IOException {
        final int TIMEOUT = 5000;

        ClientProtocol.createDisconnectPacket().send(outputStream);

        joined = false;

        // Wait for the server to close the socket
        try {
            int timeElapsed = 0;
            while (!socket.isClosed() && timeElapsed < TIMEOUT) {
                Thread.sleep(100);
                timeElapsed += 100;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (socket != null && !socket.isClosed())
            socket.close();
    }

    final public Message getSavedMessage(int groupId, int messageId) {
        if (!groups.containsKey(groupId))
            return null;

        Group g = groups.get(groupId);

        if (!g.messages.containsKey(messageId))
            return null;

        return g.messages.get(messageId);
    }

}
