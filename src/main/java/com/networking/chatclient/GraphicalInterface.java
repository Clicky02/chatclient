package com.networking.chatclient;

import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.*;

import com.networking.chatclient.ChatClient.Group;
import com.networking.chatclient.ChatClient.Message;
import com.networking.chatclient.ChatClient.ReceiveMessageLabelEventPayload;

import java.awt.*;

/*
 * The code for the graphical user interface
 * 
 * Uses Java's swing library to create the UI
 */
class GraphicalInterface extends UserInterface {

    public GraphicalInterface(ChatClient client) {
        super(client);
    }

    @Override
    public void run() {
        try {
            connectAndJoin();
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }

        new ChatFrame(client);
    }

    private void connectAndJoin() throws InterruptedException {
        final String CONNECTION_PANEL = "Connection Panel";
        final String JOIN_PANEL = "Join Panel";

        Object lock = new Object();

        synchronized (lock) {

            JFrame connectionFrame = new JFrame("Server Connection");
            connectionFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            connectionFrame.setSize(400, 120);
            connectionFrame.setResizable(false);

            JPanel titlePanel = new JPanel();
            JLabel title = new JLabel("Enter the host and port of the server.");
            titlePanel.add(title);

            connectionFrame.getContentPane().add(titlePanel, BorderLayout.NORTH);

            // Connection Panel

            JPanel connectionPanel = new JPanel();

            JTextField host = new JTextField("localhost", 10);
            connectionPanel.add(host);

            connectionPanel.add(new JLabel(":"));

            SpinnerModel model = new SpinnerNumberModel(2434, // default
                    0, // min
                    100000, // max
                    1);
            JSpinner port = new JSpinner(model);
            connectionPanel.add(port);

            JButton connectButton = new JButton("Connect");
            connectionPanel.add(connectButton);

            connectButton.addActionListener((evt) -> {
                synchronized (lock) {
                    try {
                        client.connect(host.getText(), (int) port.getValue()); // try to connect
                        lock.notify(); // notify that the connection was successful
                    } catch (Exception e) {
                        title.setText("Unable to connect.");
                    }
                }
            });

            // Join Panel

            JPanel joinPanel = new JPanel();

            JLabel usernameLabel = new JLabel("Username");
            joinPanel.add(usernameLabel);

            JTextField username = new JTextField(10);
            joinPanel.add(username);

            JButton joinButton = new JButton("Join");
            joinPanel.add(joinButton);

            joinButton.addActionListener((evt) -> {
                synchronized (lock) {
                    if (client.join(username.getText())) {
                        lock.notify(); // notify that the join was successful
                    } else {
                        title.setText("Unable to join with that username.");
                    }
                }
            });

            // Cards for the panels

            JPanel cards = new JPanel(new CardLayout());
            cards.add(connectionPanel, CONNECTION_PANEL);
            cards.add(joinPanel, JOIN_PANEL);

            CardLayout cl = (CardLayout) (cards.getLayout());
            cl.show(cards, CONNECTION_PANEL);

            connectionFrame.getContentPane().add(cards, BorderLayout.CENTER);
            connectionFrame.setVisible(true);

            // Wait for a successful connection
            lock.wait();

            title.setText("Enter your username.");
            cl.show(cards, JOIN_PANEL);

            // Wait for join to occur
            lock.wait();

            connectionFrame.setVisible(false);
            connectionFrame.dispose();
        }

    }

}

/*
 * The main window (or frame) for the chat application
 */
class ChatFrame extends JFrame {

    enum ChatFrameState {
        SHOWING_MESSAGE_LIST,
        CREATING_MESSAGE,
        READING_MESSAGE,
        UNINITIALIZED
    }

    ChatClient client;
    ChatFrameState state = ChatFrameState.UNINITIALIZED;
    Group selectedGroup = null;

    MessageListPanel messageList;

    /*
     * Bottom Control Panel Components
     */

    JPanel controlPanel;
    JButton readMessageButton;
    JButton createMessageButton;
    JButton returnButton;

    /*
     * Message Creation Components
     */

    JPanel messageCreationPanel;
    JTextField subjectField;
    JTextArea contentField;
    JLabel messageCreationFeeback;

    /*
     * Message Read Components
     */

    JPanel messageReadPanel;
    JTextField groupDisplayField;
    JTextField idDisplayField;
    JTextField userDisplayField;
    JTextField subjectDisplayField;
    JTextArea contentDisplayField;
    JLabel messageReadFeeback;

    JButton sendMessageButton;

    /*
     * Group Sidebar Components
     */

    JSplitPane topGroupPane;
    JSplitPane bottomGroupPane;

    JPanel allGroupsPanel;
    JList<Group> allGroupsList;
    DefaultListModel<Group> allGroupsListModel;
    JButton groupJoinButton;

    JPanel userGroupsPanel;
    JList<Group> userGroupsList;
    DefaultListModel<Group> userGroupsListModel;
    JButton groupLeaveButton;

    JPanel usersPanel;
    JList<String> usersList;
    DefaultListModel<String> usersListModel;

    public ChatFrame(ChatClient client) {

        // Create the window/frame
        super("Chat Frame");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setMinimumSize(new Dimension(500, 500));

        this.client = client;

        // Create and add the control panel
        createControlPanel();
        getContentPane().add(controlPanel, BorderLayout.SOUTH);

        // Create group panel
        createGroupPanel();
        topGroupPane.setPreferredSize(new Dimension(200, 200));
        getContentPane().add(topGroupPane, BorderLayout.EAST);

        // Create the message creation panel
        createMessageCreationPanel();

        // Create the message read panel
        createMessageReadPanel();

        // Create and add the message list
        messageList = new MessageListPanel();
        client.receiveMessageLabelEvent.onEvent((payload) -> {
            messageList.tryAddMessageLabel(payload.labelMessage);
        });

        // Set initial state
        setSelectedGroup(client.getGroups()[0]);
        setState(ChatFrameState.SHOWING_MESSAGE_LIST);

        // Make window visible
        setVisible(true);

    }

    public void createControlPanel() {
        controlPanel = new JPanel();

        // Read Message Button
        readMessageButton = new JButton("Read Message");
        readMessageButton.addActionListener((evt) -> {
            if (messageList != null && messageList.hasSelectedRow()) {
                readMessage(messageList.getSelectedMessage());
            }
        });
        controlPanel.add(readMessageButton);

        // Create Message Button
        createMessageButton = new JButton("Create Message");
        createMessageButton.addActionListener((evt) -> {
            setState(ChatFrameState.CREATING_MESSAGE);
        });
        controlPanel.add(createMessageButton);

        // Return Button
        returnButton = new JButton("Return");
        returnButton.addActionListener((evt) -> {
            setState(ChatFrameState.SHOWING_MESSAGE_LIST);
        });
        controlPanel.add(returnButton);
    }

    public void createGroupPanel() {

        // Retrieve Groups
        client.retrieveGroups();

        // Create all groups holder panel
        allGroupsPanel = new JPanel();
        allGroupsPanel.setBackground(new Color(255, 255, 255));
        allGroupsPanel.setLayout(new BorderLayout());
        allGroupsPanel.setBorder(BorderFactory.createTitledBorder("All Groups"));

        // Create the model for the all groups list
        allGroupsListModel = new DefaultListModel<Group>();
        allGroupsListModel.addAll(Arrays.asList(client.getGroups())); // add all possible groups

        // Create the all groups list
        allGroupsList = new JList<Group>(allGroupsListModel);
        allGroupsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        allGroupsList.addListSelectionListener((evt) -> {
            Group g = allGroupsList.getSelectedValue();
            groupJoinButton.setEnabled(g != null && !client.userIsInGroup(g.id));
        });
        JScrollPane scroll = new JScrollPane(allGroupsList);
        scroll.setMinimumSize(new Dimension(100, 100));
        allGroupsPanel.add(scroll, BorderLayout.CENTER);

        // Create the join group button
        groupJoinButton = new JButton("Join");
        groupJoinButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        groupJoinButton.setEnabled(false);
        groupJoinButton.addActionListener((evt) -> {
            Group g = allGroupsList.getSelectedValue();

            if (g != null && !client.userIsInGroup(g.id)) {
                if (client.joinGroup(g.id)) {
                    userGroupsListModel.addElement(g);
                    groupJoinButton.setEnabled(false);
                }
            }
        });
        allGroupsPanel.add(groupJoinButton, BorderLayout.SOUTH);

        // Create user groups holder panel
        userGroupsPanel = new JPanel();
        userGroupsPanel.setBackground(new Color(255, 255, 255));
        userGroupsPanel.setLayout(new BorderLayout());
        userGroupsPanel.setBorder(BorderFactory.createTitledBorder("User Groups"));

        // Create the model for the user groups list
        userGroupsListModel = new DefaultListModel<Group>();
        for (int id : client.getUserGroups()) { // Add every group the client is already a part of
            userGroupsListModel.addElement(client.getGroup(id));
        }

        // Create the user groups list
        userGroupsList = new JList<Group>(userGroupsListModel);
        userGroupsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userGroupsList.addListSelectionListener((evt) -> {
            Group g = userGroupsList.getSelectedValue();
            groupLeaveButton.setEnabled(g != null && g.id != 0); // Do not let them leave the global group
            setSelectedGroup(g);

            if (g == null) {
                userGroupsList.setSelectedIndex(0);
            }

        });
        scroll = new JScrollPane(userGroupsList);
        scroll.setMinimumSize(new Dimension(100, 100));
        userGroupsPanel.add(scroll, BorderLayout.CENTER);

        // Create the leave group button
        groupLeaveButton = new JButton("Leave");
        groupLeaveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        groupLeaveButton.setEnabled(false);
        groupLeaveButton.addActionListener((evt) -> {
            Group g = userGroupsList.getSelectedValue();

            if (g != null && g.id != 0) { // Do not let them leave the global group
                if (client.leaveGroup(g.id)) {
                    userGroupsListModel.removeElement(g);
                }
            }
        });
        userGroupsPanel.add(groupLeaveButton, BorderLayout.SOUTH);

        // Create users panel
        usersPanel = new JPanel();
        usersPanel.setBackground(new Color(255, 255, 255));
        usersPanel.setLayout(new BorderLayout());
        usersPanel.setBorder(BorderFactory.createTitledBorder("Users"));

        // Create the model for the users list
        usersListModel = new DefaultListModel<String>();
        usersListModel.addAll(client.getGroup(0).users);

        // Create the all groups list
        usersList = new JList<String>(usersListModel);
        usersList.setSelectionModel(new DefaultListSelectionModel() { // Do not allow selections
            @Override
            public void setSelectionInterval(int index0, int index1) {
                super.setSelectionInterval(-1, -1);
            }
        });
        scroll = new JScrollPane(usersList);
        scroll.setMinimumSize(new Dimension(100, 100));
        usersPanel.add(scroll, BorderLayout.CENTER);

        // Listen for player join and add to them to the list
        client.userJoinEvent.onEvent((payload) -> {
            if (payload.group != null && payload.group.id == selectedGroup.id) {
                usersListModel.addElement(payload.username);
            }
        });

        // Create Panes and add panels
        topGroupPane = new JSplitPane();
        topGroupPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        bottomGroupPane = new JSplitPane();
        bottomGroupPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

        topGroupPane.setTopComponent(allGroupsPanel);
        topGroupPane.setBottomComponent(bottomGroupPane);
        bottomGroupPane.setTopComponent(userGroupsPanel);
        bottomGroupPane.setBottomComponent(usersPanel);

    }

    private void createMessageCreationPanel() {

        SpringLayout layout = new SpringLayout();
        messageCreationPanel = new JPanel(layout);

        messageCreationPanel.setBackground(new Color(200, 200, 200));

        // Subject Label & Field

        JLabel subjectLabel = new JLabel("Subject");
        layout.putConstraint(SpringLayout.WEST, subjectLabel,
                10,
                SpringLayout.WEST, messageCreationPanel);
        layout.putConstraint(SpringLayout.NORTH, subjectLabel,
                10,
                SpringLayout.NORTH, messageCreationPanel);
        messageCreationPanel.add(subjectLabel);

        subjectField = new JTextField(20);
        layout.putConstraint(SpringLayout.WEST, subjectField,
                10,
                SpringLayout.EAST, subjectLabel);
        layout.putConstraint(SpringLayout.NORTH, subjectField,
                0,
                SpringLayout.NORTH, subjectLabel);
        messageCreationPanel.add(subjectField);

        // Content Label

        JLabel contentLabel = new JLabel("Content");
        layout.putConstraint(SpringLayout.WEST, contentLabel,
                10,
                SpringLayout.WEST, messageCreationPanel);
        layout.putConstraint(SpringLayout.NORTH, contentLabel,
                10,
                SpringLayout.SOUTH, subjectLabel);
        messageCreationPanel.add(contentLabel);

        // Send Message Button

        sendMessageButton = new JButton("Send");
        sendMessageButton.addActionListener((evt) -> {
            String subject = subjectField.getText().trim();
            String content = contentField.getText().trim();

            if (subject.equals("") || content.equals("")) {
                messageCreationFeeback.setText("You must have a subject and content");
                return;
            }

            int id = selectedGroup.id;

            if (client.postMessage(id, subject, content)) {
                sendMessageButton.setEnabled(false);
                messageCreationFeeback.setText("Posting message...");

                ReceiveMessageLabelEventPayload payload = null;

                while (payload == null || !payload.labelMessage.username.equals(client.getUsername())
                        || !payload.labelMessage.subject.equals(subject)
                        || payload.labelMessage.groupId != id) {
                    payload = client.receiveMessageLabelEvent.waitForEvent();
                }

                readMessage(payload.labelMessage);

            } else {
                messageCreationFeeback.setText("Unable to post message");
            }

        });
        layout.putConstraint(SpringLayout.WEST, sendMessageButton,
                10,
                SpringLayout.EAST, contentLabel);
        layout.putConstraint(SpringLayout.SOUTH, sendMessageButton,
                -10,
                SpringLayout.SOUTH, messageCreationPanel);
        messageCreationPanel.add(sendMessageButton);

        // Feedback Label

        messageCreationFeeback = new JLabel("");
        messageCreationFeeback.setFont(new Font("Sans", Font.ITALIC, 10));
        layout.putConstraint(SpringLayout.WEST, messageCreationFeeback,
                10,
                SpringLayout.EAST, sendMessageButton);
        layout.putConstraint(SpringLayout.NORTH, messageCreationFeeback,
                0,
                SpringLayout.NORTH, sendMessageButton);
        messageCreationPanel.add(messageCreationFeeback);

        // Content Field

        contentField = new JTextArea();
        layout.putConstraint(SpringLayout.WEST, contentField,
                10,
                SpringLayout.EAST, contentLabel);
        layout.putConstraint(SpringLayout.NORTH, contentField,
                0,
                SpringLayout.NORTH, contentLabel);
        layout.putConstraint(SpringLayout.EAST, contentField,
                -30,
                SpringLayout.EAST, messageCreationPanel);
        layout.putConstraint(SpringLayout.SOUTH, contentField,
                -10,
                SpringLayout.NORTH, sendMessageButton);
        messageCreationPanel.add(contentField);

    }

    private void clearMessageCreationPanel() {
        subjectField.setText("");
        contentField.setText("");
        messageCreationFeeback.setText("");
    }

    private void createMessageReadPanel() {

        SpringLayout layout = new SpringLayout();
        messageReadPanel = new JPanel(layout);
        messageReadPanel.setBackground(new Color(200, 200, 200));

        // Group Display

        JLabel groupLabel = new JLabel("Group");
        layout.putConstraint(SpringLayout.WEST, groupLabel,
                10,
                SpringLayout.WEST, messageReadPanel);
        layout.putConstraint(SpringLayout.NORTH, groupLabel,
                10,
                SpringLayout.NORTH, messageReadPanel);
        messageReadPanel.add(groupLabel);

        groupDisplayField = new JTextField(15);
        layout.putConstraint(SpringLayout.WEST, groupDisplayField,
                10,
                SpringLayout.EAST, groupLabel);
        layout.putConstraint(SpringLayout.NORTH, groupDisplayField,
                0,
                SpringLayout.NORTH, groupLabel);
        groupDisplayField.setEditable(false);
        messageReadPanel.add(groupDisplayField);

        // ID Display

        JLabel idLabel = new JLabel("Message Id");
        layout.putConstraint(SpringLayout.WEST, idLabel,
                10,
                SpringLayout.WEST, messageCreationPanel);
        layout.putConstraint(SpringLayout.NORTH, idLabel,
                10,
                SpringLayout.SOUTH, groupLabel);
        messageReadPanel.add(idLabel);

        idDisplayField = new JTextField(15);
        layout.putConstraint(SpringLayout.WEST, idDisplayField,
                10,
                SpringLayout.EAST, idLabel);
        layout.putConstraint(SpringLayout.NORTH, idDisplayField,
                0,
                SpringLayout.NORTH, idLabel);
        idDisplayField.setEditable(false);
        messageReadPanel.add(idDisplayField);

        // User Display

        JLabel userLabel = new JLabel("Username");
        layout.putConstraint(SpringLayout.WEST, userLabel,
                10,
                SpringLayout.WEST, messageCreationPanel);
        layout.putConstraint(SpringLayout.NORTH, userLabel,
                10,
                SpringLayout.SOUTH, idLabel);
        messageReadPanel.add(userLabel);

        userDisplayField = new JTextField(15);
        layout.putConstraint(SpringLayout.WEST, userDisplayField,
                10,
                SpringLayout.EAST, userLabel);
        layout.putConstraint(SpringLayout.NORTH, userDisplayField,
                0,
                SpringLayout.NORTH, userLabel);
        userDisplayField.setEditable(false);
        messageReadPanel.add(userDisplayField);

        // Subject Display

        JLabel subjectLabel = new JLabel("Subject");
        layout.putConstraint(SpringLayout.WEST, subjectLabel,
                10,
                SpringLayout.WEST, messageCreationPanel);
        layout.putConstraint(SpringLayout.NORTH, subjectLabel,
                10,
                SpringLayout.SOUTH, userLabel);
        messageReadPanel.add(subjectLabel);

        subjectDisplayField = new JTextField(20);
        layout.putConstraint(SpringLayout.WEST, subjectDisplayField,
                10,
                SpringLayout.EAST, subjectLabel);
        layout.putConstraint(SpringLayout.NORTH, subjectDisplayField,
                0,
                SpringLayout.NORTH, subjectLabel);
        subjectDisplayField.setEditable(false);
        messageReadPanel.add(subjectDisplayField);

        // Content Label

        JLabel contentLabel = new JLabel("Content");
        layout.putConstraint(SpringLayout.WEST, contentLabel,
                10,
                SpringLayout.WEST, messageCreationPanel);
        layout.putConstraint(SpringLayout.NORTH, contentLabel,
                10,
                SpringLayout.SOUTH, subjectLabel);
        messageReadPanel.add(contentLabel);

        // Message Feedback Text

        messageReadFeeback = new JLabel("");
        messageReadFeeback.setFont(new Font("Sans", Font.ITALIC, 10));
        layout.putConstraint(SpringLayout.WEST, messageReadFeeback,
                10,
                SpringLayout.EAST, contentLabel);
        layout.putConstraint(SpringLayout.SOUTH, messageReadFeeback,
                -10,
                SpringLayout.SOUTH, messageReadPanel);
        messageReadPanel.add(messageReadFeeback);

        // Content Display Field

        contentDisplayField = new JTextArea(10, 30);
        layout.putConstraint(SpringLayout.WEST, contentDisplayField,
                10,
                SpringLayout.EAST, contentLabel);
        layout.putConstraint(SpringLayout.NORTH, contentDisplayField,
                0,
                SpringLayout.NORTH, contentLabel);
        layout.putConstraint(SpringLayout.EAST, contentDisplayField,
                -30,
                SpringLayout.EAST, messageReadPanel);
        layout.putConstraint(SpringLayout.SOUTH, contentDisplayField,
                -10,
                SpringLayout.NORTH, messageReadFeeback);
        contentDisplayField.setEditable(false);
        messageReadPanel.add(contentDisplayField);

    }

    private void clearMessageReadPanel() {
        groupDisplayField.setText("");
        idDisplayField.setText("");
        userDisplayField.setText("");
        subjectDisplayField.setText("");
        contentDisplayField.setText("");
        messageReadFeeback.setText("");
    }

    private void setState(ChatFrameState newState) {
        if (state == newState)
            return;

        switch (state) {
            case CREATING_MESSAGE:
                getContentPane().remove(messageCreationPanel);
                break;
            case READING_MESSAGE:
                getContentPane().remove(messageReadPanel);
                break;
            case SHOWING_MESSAGE_LIST:
                getContentPane().remove(messageList);
                break;
            default:
                break;
        }

        switch (newState) {
            case CREATING_MESSAGE:
                clearMessageCreationPanel();
                getContentPane().add(messageCreationPanel, BorderLayout.CENTER);
                sendMessageButton.setEnabled(true);
                createMessageButton.setEnabled(false);
                readMessageButton.setEnabled(false);
                returnButton.setEnabled(true);
                break;
            case READING_MESSAGE:
                clearMessageReadPanel();
                getContentPane().add(messageReadPanel, BorderLayout.CENTER);
                createMessageButton.setEnabled(false);
                readMessageButton.setEnabled(false);
                returnButton.setEnabled(true);
                break;
            case SHOWING_MESSAGE_LIST:
                getContentPane().add(messageList, BorderLayout.CENTER);
                messageList.setColumnWidths(0.15, 0.15, 0.7);
                createMessageButton.setEnabled(true);
                readMessageButton.setEnabled(true);
                returnButton.setEnabled(false);
                break;
            default:
                break;
        }

        revalidate();
        repaint();
        state = newState;
    }

    private void readMessage(Message m) {
        setState(ChatFrameState.READING_MESSAGE);

        messageReadFeeback.setText("Loading");

        m = client.retrieveMessage(m.groupId, m.messageId);

        if (m != null) {
            Group g = client.getGroup(m.groupId);

            messageReadFeeback.setText("Message Successfully Loaded");

            if (g != null) {
                groupDisplayField.setText(g.name);
                idDisplayField.setText(String.valueOf(m.messageId));
                userDisplayField.setText(m.username);
                subjectDisplayField.setText(m.subject);
                contentDisplayField.setText(m.getContent());
            } else {
                messageReadFeeback.setText("Something went wrong");
            }
        } else {
            messageReadFeeback.setText("Unable to fetch message");
        }
    }

    private void setSelectedGroup(Group newGroup) {
        selectedGroup = newGroup;
        messageList.setSelectedGroup(newGroup);

        usersListModel.removeAllElements();
        usersListModel.addAll(newGroup.users);
    }
}

/*
 * The central panel that lists all of the messages in a group
 */
class MessageListPanel extends JPanel {

    final String[] tableHeaders = { "Username", "Post Date", "Subject" };

    private Group selectedGroup;

    private JLabel header;

    private JTable messageTable;
    private DefaultTableModel tableModel;
    private ArrayList<Message> messages = new ArrayList<>();

    public MessageListPanel() {
        super(new BorderLayout());

        header = new JLabel("Messages", SwingConstants.CENTER);
        add(header, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(tableHeaders, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        messageTable = new JTable(tableModel);
        messageTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        messageTable.setCellSelectionEnabled(false);
        messageTable.setRowSelectionAllowed(true);
        messageTable.getTableHeader().setReorderingAllowed(false);
        messageTable.getTableHeader().setResizingAllowed(false);

        setColumnWidths(0.15, 0.15, 0.7);

        add(new JScrollPane(messageTable), BorderLayout.CENTER);
    }

    public void setSelectedGroup(Group newGroup) {
        selectedGroup = newGroup;

        // header.setText(selectedGroup.name + " Messages");
        updateTable();
    }

    public Group getSelectedGroup() {
        return selectedGroup;
    }

    public void updateTable() {
        if (selectedGroup != null) {
            String[][] tableData = new String[selectedGroup.messages.size()][3];

            messages = new ArrayList<Message>(selectedGroup.messages.values());

            for (int i = 0; i < messages.size(); i++) {
                Message m = messages.get(i);
                tableData[i][0] = m.username;
                tableData[i][1] = m.postDate;
                tableData[i][2] = m.subject;
            }

            tableModel.setDataVector(tableData, tableHeaders);
        } else {
            tableModel.setDataVector(new String[0][0], tableHeaders);
        }

        setColumnWidths(0.15, 0.15, 0.7);
    }

    public void tryAddMessageLabel(Message m) {
        if (m.groupId == selectedGroup.id) {
            tableModel.addRow(new String[] { m.username, m.postDate, m.subject });
            messages.add(m);
        }
    }

    public boolean hasSelectedRow() {
        return messageTable.getSelectedRow() != -1;
    }

    public Message getSelectedMessage() {
        int row = messageTable.getSelectedRow();
        return messages.get(row);
    }

    public void setColumnWidths(double... widthPercentages) {
        final double factor = 10000;

        TableColumnModel model = messageTable.getColumnModel();
        for (int columnIndex = 0; columnIndex < widthPercentages.length; columnIndex++) {
            TableColumn column = model.getColumn(columnIndex);
            column.setPreferredWidth((int) (widthPercentages[columnIndex] * factor));
        }
    }
}
