package com.networking.chatclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.networking.chatclient.ChatClient.Group;
import com.networking.chatclient.ChatClient.Message;

/*
 * The code for the command line user interface
 */
public class CommandLineInterface extends UserInterface {

    /*
     * Class for each user command
     * 
     * Takes in the command, how many arguments it accepts (including the command),
     * and a function that runs when the command is typed in by the user.
     */

    public class Command {
        String name;
        int minimumArgumentCount;
        Function<ArrayList<String>, Boolean> commandFunction;
        String[] argumentDescriptions;
        boolean mustBeJoined = true;

        public Command(String name, int minimumArgumentCount,
                Function<ArrayList<String>, Boolean> commandFunction) {
            this.name = name;
            this.argumentDescriptions = new String[] {};
            this.minimumArgumentCount = minimumArgumentCount;
            this.commandFunction = commandFunction;
        }

        public Command(String name, int minimumArgumentCount,
                Function<ArrayList<String>, Boolean> commandFunction, boolean mustBeJoined) {
            this.name = name;
            this.argumentDescriptions = new String[] {};
            this.minimumArgumentCount = minimumArgumentCount;
            this.commandFunction = commandFunction;
            this.mustBeJoined = mustBeJoined;
        }

        public Command(String name, int minimumArgumentCount, String[] argumentDescriptions,
                Function<ArrayList<String>, Boolean> commandFunction) {
            this.name = name;
            this.argumentDescriptions = argumentDescriptions;
            this.minimumArgumentCount = minimumArgumentCount;
            this.commandFunction = commandFunction;
        }

        public Command(String name, int minimumArgumentCount, String[] argumentDescriptions,
                Function<ArrayList<String>, Boolean> commandFunction, boolean mustBeJoined) {
            this.name = name;
            this.argumentDescriptions = argumentDescriptions;
            this.minimumArgumentCount = minimumArgumentCount;
            this.commandFunction = commandFunction;
            this.mustBeJoined = mustBeJoined;
        }

        public boolean run(ChatClient client, ArrayList<String> args) {
            if (mustBeJoined && !client.isJoined()) {
                System.out.println("You must have connected to and joined a sever to perform this command.");
                System.out.println("Run the command \"connect <host> <port>\" to connect to a server.");
                System.out.println("Run the command \"join <username>\" to join a server.");
                return false;
            }

            if (args.size() < minimumArgumentCount) {
                System.out.println(name + " requires at least " + (minimumArgumentCount - 1) + " arguments.");
                System.out.println("Usage: " + getCommandUsage());
                return false;
            }

            return commandFunction.apply(args);
        }

        public String getCommandUsage() {
            StringBuilder usage = new StringBuilder(name);

            for (int i = 0; i < argumentDescriptions.length; i++) {
                usage.append(" <" + argumentDescriptions[i] + ">");
            }

            return usage.toString();
        }
    }

    /*
     * The list of all commands that can be used.
     */
    Command[] commands = {
            new Command("help", 1, (args) -> {
                System.out.print("Available Commands: \n");
                for (int i = 0; i < this.commands.length; i++) {
                    Command c = this.commands[i];
                    System.out.print("\t" + c.getCommandUsage() + "\n");
                }
                return true;
            }, false),

            new Command("connect", 3, new String[] { "host", "port" }, (args) -> {
                if (client.socket != null && client.socket.isConnected()) {
                    System.out.println("You are already connected to a server.");
                    return false;
                }

                try {
                    String host = args.get(1);
                    int port = Integer.parseInt(args.get(2));
                    client.connect(host, port);
                    return true;
                } catch (NumberFormatException e) {
                    System.out.println("The port must be an integer.");
                    return false;
                } catch (Exception e) {
                    System.out.println("Error Connecting.");
                    return false;
                }
            }, false),

            new Command("join", 2, new String[] { "username" }, (args) -> {
                if (client.socket == null || !client.socket.isConnected()) {
                    System.out.println("You must first connect to a server.");
                    System.out.println("Run the command \"connect <host> <port>\" to connect to a server.");
                    return false;
                }

                String username = args.get(1);
                if (client.join(username)) {
                    System.out.println("Joined server.");
                } else {
                    System.out.println("Invalid username.");
                }

                return true;
            }, false),

            new Command("post", 3, new String[] { "subject", "content" }, (args) -> {
                String subject = args.get(1);
                String content = args.get(2);

                if (!client.postMessage(0, subject, content)) {
                    System.out.println("Unable to post message.");
                    return false;
                }

                System.out.println("Message posted.");
                return true;
            }),

            new Command("users", 1, (args) -> {
                Group g = client.retrieveGroupUsers(0);

                if (g == null) {
                    System.out.println("Unable to retrieve users.");
                    return false;
                }

                System.out.print("Users for global group: ");
                for (int i = 0; i < g.users.size(); i++) {
                    System.out.print(g.users.get(i));
                    if (i != g.users.size() - 1) {
                        System.out.print(", ");
                    }
                }

                System.out.print("\n");

                return true;
            }),

            new Command("leave", 1, (args) -> {
                client.logOut();
                System.out.println("Logged out of server.");
                return true;
            }),

            new Command("message", 2, new String[] { "messageId" }, (args) -> {
                int messageId = getMessageIdFromArgument(0, args.get(1));
                if (messageId == -1) {
                    System.out.println("Invalid message Id.");
                    return false;
                }

                Message m = client.retrieveMessage(0, messageId);

                if (m == null) {
                    System.out.println("Could not retrieve message " + messageId + " from the server.");
                    return false;
                }

                System.out.println("Message " + m.messageId + ": " + m.getContent());
                return true;

            }),

            new Command("exit", 1, (args) -> {
                try {
                    System.out.println("Disconnecting...");
                    client.disconnect();

                    System.exit(0);
                    return true;
                } catch (IOException e) {
                    System.out.println("Something went wrong.");
                    return false;
                }
            }, false),

            new Command("groups", 1, (args) -> {
                ArrayList<Group> groups = client.retrieveGroups();

                System.out.print("Groups: ");
                for (int i = 0; i < groups.size(); i++) {
                    Group group = groups.get(i);

                    System.out.print(group.name + " (" + group.id + ")");
                    if (i != groups.size() - 1) {
                        System.out.print(", ");
                    }
                }

                System.out.print("\n");

                return true;
            }),

            new Command("groupjoin", 2, new String[] { "group id/name" }, (args) -> {
                // client.retrieveGroups();

                int groupId = getGroupIdFromArgument(args.get(1));
                if (groupId == -1) {
                    System.out.println("Invalid Group.");
                    return false;
                }

                if (client.userIsInGroup(groupId)) {
                    System.out.println("Already in group.");
                    return false;
                }

                if (!client.joinGroup(groupId)) {
                    System.out.println("Unable to join group.");
                    return false;
                }

                System.out.println("Joined group.");

                return true;
            }),

            new Command("grouppost", 4, new String[] { "group id/name", "subject", "content" }, (args) -> {
                // client.retrieveGroups();

                int groupId = getGroupIdFromArgument(args.get(1));
                if (groupId == -1) {
                    System.out.println("Invalid Group.");
                    return false;
                }

                if (!client.userIsInGroup(groupId)) {
                    System.out.println("Not in group.");
                    return false;
                }

                String subject = args.get(2);
                String content = args.get(3);

                if (!client.postMessage(groupId, subject, content)) {
                    System.out.println("Unable to post message.");
                    return false;
                }

                System.out.println("Message posted.");
                return true;
            }),

            new Command("groupusers", 2, new String[] { "group id/name" }, (args) -> {
                // client.retrieveGroups();

                int groupId = getGroupIdFromArgument(args.get(1));
                if (groupId == -1) {
                    System.out.println("Invalid Group.");
                    return false;
                }

                Group g = client.retrieveGroupUsers(groupId);

                if (g == null) {
                    System.out.println("Unable to retrieve users.");
                    return false;
                }

                System.out.print("Users for " + g.name + ": ");
                for (int i = 0; i < g.users.size(); i++) {
                    System.out.print(g.users.get(i));
                    if (i != g.users.size() - 1) {
                        System.out.print(", ");
                    }
                }

                System.out.print("\n");

                return true;
            }),

            new Command("groupleave", 2, new String[] { "group id/name" }, (args) -> {
                // client.retrieveGroups();

                int groupId = getGroupIdFromArgument(args.get(1));
                if (groupId == -1) {
                    System.out.println("Invalid Group.");
                    return false;
                }

                if (!client.userIsInGroup(groupId)) {
                    System.out.println("Not in group.");
                    return false;
                }

                if (client.leaveGroup(groupId)) {
                    System.out.println("Left group.");
                    return true;
                } else {
                    System.out.println("Unable to leave group.");
                    return false;
                }

            }),

            new Command("groupmessage", 3, new String[] { "group id/name", "message id" }, (args) -> {
                // client.retrieveGroups();

                int groupId = getGroupIdFromArgument(args.get(1));
                if (groupId == -1) {
                    System.out.println("Invalid Group.");
                    return false;
                }

                if (!client.userIsInGroup(groupId)) {
                    System.out.println("Not in group.");
                    return false;
                }

                int messageId = getMessageIdFromArgument(groupId, args.get(2));
                if (messageId == -1) {
                    System.out.println("Invalid message Id.");
                    return false;
                }

                Message m = client.retrieveMessage(groupId, messageId);

                if (m == null) {
                    System.out.println("Could not retrieve message " + messageId + " from the server.");
                    return false;
                }

                System.out.println("Message " + m.messageId + ": " + m.getContent());
                return true;

            }),

    };

    BufferedReader reader; // The buffered reader for System.in

    public CommandLineInterface(ChatClient client) {
        super(client);

        reader = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public void run() {

        client.receiveMessageLabelEvent.onEvent((payload) -> {
            synchronized (this) {
                Message m = payload.labelMessage;
                Group g = client.getGroup(m.groupId);

                System.out.print("\b\b\n");
                System.out.println("------------------------------New Message------------------------------");
                System.out.println("Id: " + m.messageId + "\tGroup: " + g.name);
                System.out.println("User: " + m.username + "\tDate: " + m.postDate);
                System.out.println("Subject: " + m.subject);
                System.out.println("-----------------------------------------------------------------------");
                System.out.print("> ");
            }
        });

        System.out.println("Enter a command or enter help to get a list of commands.");

        while (true) {
            String input = getString("> ");

            ArrayList<String> args = new ArrayList<String>();
            Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(input);
            while (m.find()) {
                args.add(m.group(1).replace("\"", ""));
            }

            if (!args.isEmpty())
                runCommand(args);
        }
    }

    private synchronized void runCommand(ArrayList<String> args) {
        String commandName = args.get(0);
        for (Command command : commands) {
            if (command.name.equalsIgnoreCase(commandName)) {
                command.run(client, args);
                return;
            }
        }

        System.out.println("No command with name " + commandName + ".");
    }

    private int getGroupIdFromArgument(String argument) {
        int groupId = -1;
        try {
            groupId = Integer.parseInt(argument);

            // Make sure groupId actually exists
            if (!client.isValidGroupId(groupId, false, false)) {
                return -1;
            }
        } catch (NumberFormatException e) {
            String groupName = argument;
            for (Group g : client.getGroups()) {
                System.out.println(g.name);
                System.out.println(groupName);
                if (g.name.equals(groupName)) {
                    groupId = g.id;
                }
            }
        }

        return groupId;
    }

    private int getMessageIdFromArgument(int groupId, String argument) {
        int messageId = -1;
        try {
            messageId = Integer.parseInt(argument);
        } catch (NumberFormatException e) {

        }

        return messageId;
    }

    private String getString(String prompt) {
        String result = null;
        while (result == null) {
            try {
                System.out.print(prompt);
                result = reader.readLine();
            } catch (IOException e) {
                System.out.println("IO Exception Occured: " + e.getMessage());
            }
        }

        return result;
    }

}
