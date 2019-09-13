package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.*;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final ArrayList<String> REMOTE_PORTS = new ArrayList<String>() {
        {
            add("11108");
            add("11112");
            add("11116");
            add("11120");
            add("11124");
        }
    };
    static final int SERVER_PORT = 10000;
    private static int proposedPriority = 1;
    static Boolean anyFailure = false;
    static String failedNode = null;
    static int counterForMessageID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        /*
         * Calculate the port number that this AVD listens on.
         * It is just a hack that I came up with to get around the networking limitations of AVDs.
         * The explanation is provided in the PA1 spec.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            //serverSocket.setReuseAddress(true);
            //serverSocket.bind(new InetSocketAddress(SERVER_PORT));
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket" + e.getMessage());
            return;
        }

        /*
         * Retrieve a pointer to the input box (EditText) defined in the layout
         * XML file (res/layout/main.xml).
         *
         * This is another example of R class variables. R.id.edit_text refers to the EditText UI
         * element declared in res/layout/main.xml. The id of "edit_text" is given in that file by
         * the use of "android:id="@+id/edit_text""
         */
        final EditText editText = (EditText) findViewById(R.id.editText1);

        final Button sendButton = (Button) findViewById(R.id.button4);

        /*
         * Register an OnKeyListener for the input box. OnKeyListener is an event handler that
         * processes each key event. The purpose of the following code is to detect an enter key
         * press event, and create a client thread so that the client thread can send the string
         * in the input box over the network.
         */
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "Null message entered. Ignoring it.");
                String msg = editText.getText().toString();

                if(msg != null && msg != "" && msg.length() > 0) {
                    editText.setText(""); // This is one way to reset the input box.
                    TextView localTextView = (TextView) findViewById(R.id.textView1);
                    localTextView.append("\t" + msg); // This is one way to display a string.
                    TextView remoteTextView = (TextView) findViewById(R.id.textView1);
                    remoteTextView.append("\n");

                    /*
                     * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                     * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
                     * the difference, please take a look at
                     * http://developer.android.com/reference/android/os/AsyncTask.html
                     */

                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                }
                else
                {
                    Log.e(TAG, "Null message entered. Ignoring it.");
                }

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    protected synchronized double GetProposalForPriority(int port){

        double result;
        int priority = ProposedPriorityWrapper(port,false);
        ProposedPriorityWrapper(priority + 1,true);

        switch (port) {
            case 11108:
                result = priority + 0.1;
                break;
            case 11112:
                result = priority + 0.2;
                break;
            case 11116:
                result = priority + 0.3;
                break;
            case 11120:
                result = priority + 0.4;
                break;
            case 11124:
                result = priority + 0.5;
                break;
            default:
                result = priority + 0.1;
                break;
        }

        return  result;
    }

    protected synchronized int ProposedPriorityWrapper(int newProposedPriority, boolean update){
        if(update){
            proposedPriority = newProposedPriority;
            return newProposedPriority;
        }
        return proposedPriority;
    }

    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     *
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     *
     */

    public class Message{
        public  String messageID;
        public String text;
        public double priority;
        public boolean isPriorityFinalized;
        public int sender;
    }

    class MessageComparator implements Comparator<Message> {

        public int compare(Message m1, Message m2) {
            if (m1.priority > m2.priority)
                return 1;
            else if (m1.priority < m2.priority)
                return -1;
            return 0;
        }
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        private final ContentResolver mContentResolver = getContentResolver();
        private final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
        int count = 0;
        PriorityQueue<Message> messagesQueue = new PriorityQueue<Message>(11, new MessageComparator());
        HashMap<String, Message> messagesHashMap = new HashMap<String, Message>();
        private int failedSender = 0;
        private boolean amINotified = false;

        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            ServerSocket serverSocket = sockets[0];
            Context context = getApplicationContext();
            TelephonyManager tel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
            int myPort = Integer.parseInt(String.valueOf((Integer.parseInt(portStr) * 2)));

            try{
                while(true){
                    Socket socket = serverSocket.accept();
                    socket.setSoTimeout(500);
                    try{

                        PrintWriter outputPrintWriter = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader inputBufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String messageFromClient = "";

                        while ((messageFromClient = inputBufferedReader.readLine()) != null) {

                            Log.e(TAG, "SERVER doInBackground.  Message received from Client. Message: " + messageFromClient);

                            if (messageFromClient.contains("ProposedPriority=")){
                                double priorityProposal = GetProposalForPriority(myPort);
                                outputPrintWriter.println("ProposedPriority=" + priorityProposal);
                                publishProgress(messageFromClient+priorityProposal);
                                break;
                            }
                            else if(messageFromClient.contains("FinalPriority=")){

                                int currentPriority = ProposedPriorityWrapper(0, false);
                                String[] arrOfStr = messageFromClient.split("&",0);
                                double newPriority = Double.parseDouble(arrOfStr[3].substring(14));
                                if ((int) newPriority > currentPriority) {
                                    ProposedPriorityWrapper((int) newPriority, true);
                                }
                                Log.e(TAG, "SERVER doInBackground.  Entered in FinalPriority . Message: " + messageFromClient);
                                publishProgress(messageFromClient);
                                outputPrintWriter.println("MESSAGE-RECEIVED");
                                break;
                            }else if(messageFromClient.contains("FAILED-NODE=")){
                                Log.e(TAG, "Server: Failed node notification: " + messageFromClient);
                                if(!amINotified){
                                    publishProgress(messageFromClient);
                                    amINotified = true;
                                }
                                outputPrintWriter.println("MESSAGE-RECEIVED");
                                break;
                            }else if(messageFromClient.contains("AreYouAlive")){
                                Log.e(TAG, "Server: AreYouAlive: " + messageFromClient);
                                outputPrintWriter.println("Yes");
                                break;
                            }else {
                                publishProgress(messageFromClient);
                                break;
                            }
                        }
                        inputBufferedReader.close();
                        outputPrintWriter.close();
                        socket.close();

                    }catch (SocketTimeoutException e) {
                        Log.e(TAG, "ServerTask Socket timeout: ");
                        //publishProgress("FAILED-NODE=");
                    }catch (IOException e) {
                        Log.e(TAG, "ServerTask IOException : " + e.getMessage() + "  " + e.getStackTrace());
                    }catch (Exception ex){
                        Log.e(TAG, "ServerTask Exception: " + ex.getMessage());
                    }finally {
                        socket.close();
                    }

                }

                /*
                References for writing lines 252-318:
                https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html
                https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html

                Usage was also referred from the following tutorial:
                http://www.javacjava.com/ServerSocketOne.html
                https://www.geeksforgeeks.org/priority-queue-class-in-java-2/

                 */

            } catch (Exception ex){
                Log.e(TAG, "ServerTask Exception: " + ex.getMessage());
            }
            finally{
                try{
                    serverSocket.close();
                } catch (Exception ex){
                    Log.e(TAG, "ServerTask Exception: " + ex.getMessage());
                }
            }

            return null;
        }

        protected int GetSender(int port){

            int result;

            switch (port) {
                case 11108:
                    result = 1;
                    break;
                case 11112:
                    result = 2;
                    break;
                case 11116:
                    result = 3;
                    break;
                case 11120:
                    result = 4;
                    break;
                case 11124:
                    result = 5;
                    break;
                default:
                    result = 1;
                    break;
            }

            return  result;
        }


        protected synchronized void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();

            String[] arrOfStr = strReceived.split("&",0);

            Log.e(TAG, "onProgressUpdate Message received::  " +  strReceived);

            if(strReceived.contains("ProposedPriority=")){
                Message message = new Message();
                message.text = arrOfStr[0].substring(8);
                message.messageID = arrOfStr[1].substring(3);
                message.sender = GetSender(Integer.parseInt(arrOfStr[2].substring(11)));
                message.priority = Double.parseDouble(arrOfStr[3].substring(17));
                message.isPriorityFinalized = false;
                AddMessageInPriorityQueue(message);
            }else if (strReceived.contains("FAILED-NODE=")){
                String node = strReceived.substring(12);
                failedSender = GetSender(Integer.parseInt(node));
                //RemoveFailedNodeMessagesFromQueue(senderID);

            }else if (strReceived.contains("FinalPriority=")){

                String messageID = arrOfStr[1].substring(3);
                double priority = Double.parseDouble(arrOfStr[3].substring(14));

                Log.e(TAG, "onProgressUpdate Entered for Updating priority:  ID:" +  messageID + "  priority: " + priority);

                UpdateMessagePriority(messageID,priority);
            }

            CheckQueueAndDeliverMessage();



             /*
                References for writing lines 230-241:
                Usage has been referred from OnPTestClickListener.java file of this PA.

             */

            return;
        }

        // Custom methods

        private Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }

        private synchronized boolean IsNodeAlive(String port){

            boolean result = false;

            try {

                String msgToSend = "AreYouAlive";

                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(port));

                PrintWriter outputPrintWriter = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader inputBufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                outputPrintWriter.println(msgToSend);
                String messageFromServer = "";
                while ((messageFromServer = inputBufferedReader.readLine()) != null) {
                    if (messageFromServer.contains("YES")) {
                        result = true;
                        break;
                    }
                }
                inputBufferedReader.close();
                outputPrintWriter.close();
                socket.close();

            } catch (UnknownHostException e) {
                Log.e(TAG, "NotifyAboutFailure UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "NotifyAboutFailure socket IOException 0: " + e.getMessage() + "  " + e.getStackTrace());
            } catch (Exception ex) {
                Log.e(TAG, "NotifyAboutFailure Exception: " + ex.getMessage());
            }

            return result;

        }

        private synchronized boolean AddMessageInPriorityQueue(Message message){
            messagesHashMap.put(message.messageID,message);
            return messagesQueue.add(message);
        }

        private synchronized boolean RemoveMessageFromPriorityQueue(Message message){

            boolean result = false;
            try {
                if(message!= null && messagesHashMap.containsKey(message.messageID)){
                    messagesHashMap.remove(message.messageID);
                    result = messagesQueue.remove(message);
                }

            }catch (Exception e){
                Log.e(TAG, "Exception in RemoveMessageFromPriorityQueue:  " + e.toString() );
            }
            return result;
        }

        private synchronized void UpdateMessagePriority(String messageID, double newPriority){
            int count = 0;
            try {

                Log.e(TAG, "UpdateMessagePriority Entered for Updating priority:  ID:" +  messageID + "  priority: " + newPriority + "  Is in the table?: " + messagesHashMap.containsKey(messageID));

                 if(messagesHashMap.containsKey(messageID)) {
                    Message message = messagesHashMap.get(messageID);
                     Log.e(TAG, "UpdateMessagePriority Start: Queue count: " + messagesQueue.size() + "   Message: " + message.messageID  + "  Text:" + message.text + " Priority: " + message.isPriorityFinalized + " Sender: " + message.sender + " Failed Sender:" + failedSender);

                     count = 1;
                    boolean result = RemoveMessageFromPriorityQueue(message);
                    count = 2;
                    //int currentPriority = ProposedPriorityWrapper(0, false);
                    count = 3;
                    if (result) {
                        //if ((int) newPriority > currentPriority) {
                        //    ProposedPriorityWrapper((int) newPriority, true);
                        //}
                        message.priority = newPriority;
                        message.isPriorityFinalized = true;
                        if (!AddMessageInPriorityQueue(message)) {
                            Log.e(TAG, "UpdateMessagePriority: Can not add message in the queue.");
                        }

                        CheckQueueAndDeliverMessage();

                    } else {
                        Log.e(TAG, "UpdateMessagePriority: Can not delete message from the queue.");
                    }
                }
            }catch (Exception e){
                Log.e(TAG, "UpdateMessagePriority:  " + "Message: " + messageID );
                Log.e(TAG, "UpdateMessagePriority:  " + e.toString() + " Count: " + count);
            }
        }

        private synchronized void CheckQueueAndDeliverMessage(){


            Message message = messagesQueue.peek();
            if(message != null)
                Log.e(TAG, "Queue count: " + messagesQueue.size() + "   Message: " + message.messageID  + "  Text:" + message.text + " Priority: " + message.isPriorityFinalized + " Sender: " + message.sender + " Failed Sender:" + failedSender);
            if(message!= null && message.sender == failedSender){
                RemoveMessageFromPriorityQueue(messagesQueue.peek());
                CheckQueueAndDeliverMessage();
            }else if(message!= null && message.isPriorityFinalized){
                Message messageToSend = messagesQueue.poll();
                Message nextMessage = messagesQueue.peek();

                Log.e(TAG, "CheckQueueAndDeliverMessage : Delivered Message: " + messageToSend.messageID  + "  Text:" + messageToSend.text + " Priority: " + messageToSend.priority + " Sender: " + messageToSend.sender +  " Finalized?: " + messageToSend.isPriorityFinalized + " Failed Sender:" + failedSender);

                if(nextMessage != null){
                    Log.e(TAG, "CheckQueueAndDeliverMessage : Next Message: " + nextMessage.messageID  + "  Text:" + nextMessage.text + " Priority: " + nextMessage.priority + " Sender: " + nextMessage.sender +  " Finalized?: " + nextMessage.isPriorityFinalized + " Failed Sender:" + failedSender);

                }
                else {
                    Log.e(TAG, "CheckQueueAndDeliverMessage : Next Message is empty. " );

                }
                if(messageToSend!= null && messagesHashMap.containsKey(messageToSend.messageID)) {
                    messagesHashMap.remove(messageToSend.messageID);
                }
                if( nextMessage == null ||( nextMessage != null && !nextMessage.isPriorityFinalized) || (nextMessage != null  && nextMessage.isPriorityFinalized && messageToSend.priority < nextMessage.priority))
                    SendMessageAndInsertInContentProvider(messageToSend);
                else
                    AddMessageInPriorityQueue(messageToSend);
                CheckQueueAndDeliverMessage();
            }

            return;
        }

        private synchronized void SendMessageAndInsertInContentProvider(Message message){

            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(message.text + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append("\n");

            ContentValues cv = new ContentValues();
            cv.put("key", count);
            cv.put("value", message.text);

            try {
                mContentResolver.insert(mUri, cv);
                count++;
                if(messagesHashMap.containsKey(message.messageID))
                    messagesHashMap.remove(message.messageID);

            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    /***
     *
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {


        private synchronized String GetMessageID(int port,boolean finalized){

            String result;
            if(!finalized){
                counterForMessageID++;
            }

            switch (port) {
                case 11108:
                    result = "Client1_Message"  + counterForMessageID;
                    break;
                case 11112:
                    result = "Client2_Message"  + counterForMessageID;
                    break;
                case 11116:
                    result = "Client3_Message"  + counterForMessageID;
                    break;
                case 11120:
                    result = "Client4_Message"  + counterForMessageID;
                    break;
                case 11124:
                    result = "Client5_Message"  + counterForMessageID;
                    break;
                default:
                    result = "Client1_Message"  + counterForMessageID;
                    break;
            }

            return  result;
        }

        private synchronized String GetFormatedMessage(int port, String message,double priority, boolean finalized){
            String result;

            String messageID = GetMessageID(port,finalized);

            if(!finalized){
                result = "Message=" + message.replace("\n", "") + "&Id=" + messageID + "&SenderPort="+port+"&ProposedPriority=";
            }else {
                result = "Message=" + message.replace("\n", "") + "&Id=" + messageID + "&SenderPort="+port+"&FinalPriority="+priority;
            }

            return result;
        }

        private synchronized boolean IsNodeAlive(String port){

            boolean result = false;
            Socket socket = new Socket();
            int timeout = 2000;

            try {

                Log.e(TAG, "IsNodeAlive : AreyouAlive " + port );

                String msgToSend = "AreYouAlive";
                SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(port));
                socket.connect(socketAddress, timeout);

                PrintWriter outputPrintWriter = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader inputBufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                outputPrintWriter.println(msgToSend);
                String messageFromServer = "";
                while ((messageFromServer = inputBufferedReader.readLine()) != null) {
                    Log.e(TAG, "IsNodeAlive messageFromServer: " + messageFromServer);
                    if (messageFromServer.contains("Yes")) {
                        result = true;
                        break;
                    }
                }
                inputBufferedReader.close();
                outputPrintWriter.close();
                socket.close();

            } catch (UnknownHostException e) {
                Log.e(TAG, "IsNodeAlive UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "IsNodeAlive socket IOException 0: " + e.getMessage() + "  " + e.getStackTrace());
            } catch (Exception ex) {
                Log.e(TAG, "IsNodeAlive Exception: " + ex);
            }
            Log.e(TAG, "IsNodeAlive : AreyouAlive  " + result );
            return result;

        }

        private void NotifyAboutFailure(String node, String myPort){

            for(String port : REMOTE_PORTS) {


                try {

                    if(port == node){
                        continue;
                    }

                    Log.e(TAG, "NotifyAboutFailure: " + node + "  myPort:" + port);

                    String msgToSend = "FAILED-NODE=" + node;

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(port));

                    PrintWriter outputPrintWriter = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader inputBufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    outputPrintWriter.println(msgToSend);
                    String messageFromServer = "";
                    while ((messageFromServer = inputBufferedReader.readLine()) != null) {
                        if (messageFromServer.contains("MESSAGE-RECEIVED")) {
                            break;
                        }
                    }
                    inputBufferedReader.close();
                    outputPrintWriter.close();
                    socket.close();


                } catch (UnknownHostException e) {
                    Log.e(TAG, "NotifyAboutFailure UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "NotifyAboutFailure socket IOException 0: " + e.getMessage() + "  " + e.getStackTrace());
                } catch (Exception ex) {
                    Log.e(TAG, "NotifyAboutFailure Exception: " + ex.getMessage());
                }
            }

        }

        @Override
        protected Void doInBackground(String... msgs) {
            String msg = msgs[0];
            String myPort = msgs[1];
            int timeout = 2000;
            ArrayList<Double> priorityList = new ArrayList<Double>();

            String msgToSend = GetFormatedMessage(Integer.parseInt(myPort), msg, 0, false);

            for(String port : REMOTE_PORTS) {
                Socket socket = new Socket();
                try{

                    if(anyFailure && failedNode != null && port == failedNode){
                        continue;
                    }

                    SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(port));

                    socket.connect(socketAddress, timeout);

                    Log.e(TAG, "doInBackground : sending message to " + port + " Message:" + msgToSend);


                    //Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    //       Integer.parseInt(port));

                    //socket.setSoTimeout(500);

                    PrintWriter outputPrintWriter = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader inputBufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    outputPrintWriter.println(msgToSend);
                    String messageFromServer = "";
                    while ((messageFromServer = inputBufferedReader.readLine()) != null) {
                        Log.e(TAG, "doInBackground : message received from server " + port + " Message:" + messageFromServer);

                        if (messageFromServer.contains("ProposedPriority=")) {
                            outputPrintWriter.println("Bye");
                            String[] messageArray = messageFromServer.split("ProposedPriority=",0);
                            double priority = Double.parseDouble(messageArray[1]);
                            priorityList.add(priority);
                            break;
                        }
                    }
                    inputBufferedReader.close();
                    outputPrintWriter.close();
                    socket.close();

                }catch (SocketTimeoutException e) {
                    Log.e(TAG, "ClientTask Socket timeout: " + port);
                    anyFailure = true;
                    failedNode = port;
                    NotifyAboutFailure(failedNode,myPort );
                }catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException: " + port);
                }catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException 1: " + e.getMessage() + "  " + e.getStackTrace() + " Port:  " + port);
                    anyFailure = true;
                    failedNode = port;
                    NotifyAboutFailure(failedNode, myPort);
                }catch (Exception ex){
                    Log.e(TAG, "ClientTask Exception: " + ex.getMessage() + ": Port: " + port);
                }finally {
                    try {
                        socket.close();
                    }catch (Exception ex){
                        Log.e(TAG, "ClientTask Exception: " + ex.getMessage() + ": Port: " + port);
                    }
                }

            }



            double maxPriority = Collections.max(priorityList);
            String msgToSendFinal = GetFormatedMessage(Integer.parseInt(myPort), msg, maxPriority, true);

            for(String port2 : REMOTE_PORTS) {
                Socket socket2 = new Socket();
                try{

                    if(anyFailure && failedNode != null && port2 == failedNode){
                        Log.e(TAG, "Debug: ClientTask: " + failedNode);
                            continue;
                    }

                    SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(port2));

                    socket2.connect(socketAddress, timeout);

                    Log.e(TAG, "doInBackground : sending final message to " + port2 + " Message:" + msgToSendFinal);


                    //Socket socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    //        Integer.parseInt(port2));
                    //socket2.setSoTimeout(500);


                    PrintWriter outputPrintWriter2 = new PrintWriter(socket2.getOutputStream(), true);
                    BufferedReader inputBufferedReader2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));

                    outputPrintWriter2.println(msgToSendFinal);
                    String messageFromServer = "";
                    while ((messageFromServer = inputBufferedReader2.readLine()) != null) {

                        Log.e(TAG, "doInBackground : message received from server " + port2 + " Message:" + messageFromServer);

                        if (messageFromServer.contains("MESSAGE-RECEIVED")) {
                            outputPrintWriter2.println("Bye");
                            break;
                        }
                    }
                    inputBufferedReader2.close();
                    outputPrintWriter2.close();


                }catch (SocketTimeoutException e) {
                    Log.e(TAG, "ClientTask Socket timeout: " + port2);
                    anyFailure = true;
                    failedNode = port2;
                    NotifyAboutFailure(failedNode, myPort);
                    continue;
                }catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException: " + port2);
                    continue;
                }catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException 2 " + e.getMessage() + "  " + e.getStackTrace() + " Port: " +port2);
                    anyFailure = true;
                    failedNode = port2;
                    NotifyAboutFailure(failedNode, myPort);
                    continue;
                }catch (Exception ex){
                    Log.e(TAG, "ClientTask Exception: " + ex.getMessage() + ": Port: " + port2);
                    continue;
                }
                finally {
                    try {
                        socket2.close();
                    }catch (Exception ex){
                        Log.e(TAG, "ClientTask Exception: " + ex.getMessage() + ": Port: " + port2);
                    }
                }

            }

            if(!anyFailure){

                for(String checkPort : REMOTE_PORTS) {
                    if(!IsNodeAlive(checkPort)){
                        anyFailure = true;
                        failedNode = checkPort;
                        NotifyAboutFailure(failedNode, checkPort);
                    }
                }

            }

            /*
            References for writing lines 731-870:
            https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html
            https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html

            Usage was also referred from the following tutorial:
            http://www.javacjava.com/ServerSocketOne.html
            https://examples.javacodegeeks.com/core-java/net/socket/create-client-socket-with-timeout/

             */

            return null;
        }
    }
}
