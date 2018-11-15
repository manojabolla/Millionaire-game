/*
 Project            :   The Millionaire Game
 File Name          :   FriendClient.java

Author: Alexander Popov
Date  : Saturday, July 20, 2002
Homepage: http://www.geocities.com/emu8086/vb/
*/

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
* This class represents a friend client for Millionaire Game.
*/
public class FriendClient extends JFrame implements ActionListener, Runnable {

    // streams for communication with the server:
    static PrintStream out;         // for sending to server.
    static BufferedReader in;       // for getting data from server.
    // socket for connection with the server:
    static Socket socket = null;


    JPanel pMain;               // main panel with checkboxes and question label.
    JPanel pWAITING;            // panel that is shown when friend is inactive.

    // status (center label of pWAITING):
    JLabel lblWaitStatus =
            new JLabel("Waiting for server response...", JLabel.CENTER);

    JLabel lblQuestion;         // label for question.
    JLabel lblTime;             // label to show the time.
    JCheckBox cbAnswers[] = new JCheckBox[4];   // answer boxes.
    JButton btnSend, btnDontknow;   // buttons "Send" & "Don't know".

    int selectedAnswer = -1;    // currently selected answer.

    // will be true when everything is connected:
    public boolean ALL_CONNECTED = false;

    // thread which cares about receiving data from server:
    Thread thServer = null;



    /**
    * Default contructor for the Friend Client.
    */
    public FriendClient()
    {
        super ("Friend");       // set caption.

        // look & feel setup:
        try {
            UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName() );
        } catch (Exception e) {
            System.err.println("Couldn't use the system "
                             + "look and feel: " + e);
        }

        // update look & feel for those components created in
        //     declaration (if required):
        lblWaitStatus.updateUI();


        /* The default value is: HIDE_ON_CLOSE,
           we need to ask user "Are you sure?" so closing is done
           manually like JFrame.EXIT_ON_CLOSE */
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // processing window close event:
        WindowListener L= new WindowAdapter () {
            public void windowClosing(WindowEvent e) {
                    closeApplication(false); // procedure will ask "Are you sure?".
            }
        };
        addWindowListener(L);


        // prepare the layout:
        pMain = new JPanel (new BorderLayout());    // for everything.

        JPanel pButtons = new JPanel(new FlowLayout()); // for buttons.

        lblTime = new JLabel("Time: ??");

        JPanel pLeft = new JPanel ();           // for timer.
        pLeft.setLayout(new FlowLayout());
        pLeft.add(lblTime);

        JPanel pCenter = new JPanel ();         // for question and answers.
        pCenter.setLayout(new GridLayout(3,1));

        lblQuestion = new JLabel("lblQuestion");
        pCenter.add(lblQuestion);

        JPanel pAnswers = new JPanel();      // for possible answers.
        pAnswers.setLayout(new GridLayout(2,2, 50,50));

        // make & add check boxes for answers:
        cbAnswers[0] = new JCheckBox ("A: ");       // first possible answer.
        pAnswers.add(cbAnswers[0]);
        cbAnswers[0].addActionListener(this);

        cbAnswers[1] = new JCheckBox ("B: ");       // second possible answer.
        pAnswers.add(cbAnswers[1]);
        cbAnswers[1].addActionListener(this);

        cbAnswers[2] = new JCheckBox ("C: ");       // third possible answer.
        pAnswers.add(cbAnswers[2]);
        cbAnswers[2].addActionListener(this);

        cbAnswers[3] = new JCheckBox ("D: ");       // fourth possible answer.
        pAnswers.add(cbAnswers[3]);
        cbAnswers[3].addActionListener(this);

        pCenter.add(pAnswers);

        // add inner layouts to main layout:

        pMain.add(pLeft, BorderLayout.WEST);
        pMain.add(pCenter, BorderLayout.CENTER);
        pMain.add(pButtons, BorderLayout.SOUTH);

        btnSend = new JButton("Send");
        pButtons.add(btnSend);
        btnSend.addActionListener(this);

        btnDontknow = new JButton("Don't know");
        pButtons.add(btnDontknow);
        btnDontknow.addActionListener(this);


        // prepare second layout that is shown when friend is inactive:
        pWAITING = new JPanel (new GridLayout(1,1));
        pWAITING.add(lblWaitStatus);
        lblWaitStatus.setFont(new Font("Monospaced", Font.BOLD, 16));

        setSize(400,350);

        // show the second waiting layout:
        setContentPane (pWAITING);
        setVisible(true);

        // start the thread that cares about receiving data from server:
        thServer = new Thread (this);
        thServer.start();
    }



    /**
    * Main function, from where FriendClient starts.
    */
    public static void main(String args[]) {

        // validate parameters:
        if (args.length != 2) {
            System.err.println("Usage: java FriendClient <host> <friend_port>");
            System.exit(1);
        }

        // trying to connect to the server:
        try {
            String serverAddress = args[0];
            int serverPort = Integer.parseInt(args[1]);
            socket = new Socket(serverAddress, serverPort);
        } catch (Exception e) {
            System.err.println("Wrong arguments -> " + e);
            System.exit(1);
        }

        System.out.println(socket);

        // open in and out streams for talking with the server:
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintStream(socket.getOutputStream());
        }
        catch (IOException e) {
            System.err.println("Open streams -> " + e);
            System.exit(1);
        }


        // print the address of player - for verification:
        InetAddress localHost = null;
        try {
            localHost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            System.out.println("Unknown host - probably localhost with no IP!");
            // no exit, since can work on "localhost" without internet.
        }
        System.out.println("Friend's local address: " + localHost);

        // create FriendClient and show its window:
        new FriendClient();
    }



    /**
    * Makes current thread to pause.
    *
    * @param time miliseconds to sleep.
    */
    private void pause(int time) {
        try {
            Thread.sleep(time);
        }
        catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }


    /**
    * The run method of that is used by a thread.
    */
    public void run() {

        Thread thisThread = Thread.currentThread();

        while (thServer == thisThread) {
              String serverInput;

              try {
                if ((serverInput = in.readLine()) != null)
                    gotMessageFromServer(serverInput);  // trigger "event".
              }
              catch (IOException ioe) {
                  JOptionPane.showMessageDialog(this,
                       ioe + "\nNot regular game termination!",
                       "Cannot read from server!",
                       JOptionPane.ERROR_MESSAGE);
                   closeApplication(true); // force to close.
              }
        }

    } // end of run().



    /**
    * Processes clicks on buttons and check-boxes.
    *
    * @param e the ActionEvent object.
    */
    public void actionPerformed(ActionEvent e){
      Object src=e.getSource();

      if (src == btnSend) {                 // "Send" button.
          if (selectedAnswer != -1) {
            setContentPane (pWAITING);
            setVisible(true);               // show "Waiting...".

            sendToServer("FRIEND_ANSWER:" + selectedAnswer);
          }
          else {
             JOptionPane.showMessageDialog(this,
                   "Please select an answer! or click [Don't know].",
                   "Not selected", JOptionPane.ERROR_MESSAGE);
          }
      }
      else if (src == btnDontknow) {        // "Don't know" button.
            setContentPane (pWAITING);
            setVisible(true);               // show "Waiting...".

            sendToServer("FRIEND_DONTKNOW");
      }
      else {                                // clicks on check-boxes:
          for (int i=0; i<4; i++)
            if (cbAnswers[i] == src) {
                selectedAnswer = i+1; // +1 because first is 1.

                  // deselect any other selected check-boxes
                  //      except the one that was clicked last:
                  for (int k=0; k<4; k++) {
                      if (k != i)       // not equal to selected.
                        cbAnswers[k].setSelected(false);
                      else
                        cbAnswers[k].setSelected(true);
                  }

                return;         // no need to continue.
            }

       }
    } // end of actionPerformed().



    /**
    * This method is triggered when there is message from server.
    * It processes all received data.
    *
    * @param msg what server has to say to friend.
    */
    private void gotMessageFromServer(String msg) {
        System.out.println("SERVER: [" + msg + "]");

        if (msg.equals("ALL_CONNECTED")) {      // game started.
            ALL_CONNECTED=true;
            lblWaitStatus.setText("Waiting for player's request...");
        }
        else if (msg.startsWith("TIMER:")) {    // timer updated.
            String t = extractData(msg);
            if (t.length() == 1)
                t = "0" + t;
            lblTime.setText("Time: " + t);
        }
        else if (msg.equals("TIMEUP")) {        // time is over.
             JOptionPane.showMessageDialog(this,
                   "Time is up...",
                   "Server says: Time up", JOptionPane.ERROR_MESSAGE);
            // show "waiting...":
            setContentPane (pWAITING);
            setVisible(true);
        }
        else if (msg.startsWith("Q:")) {        // question.
            lblQuestion.setText(extractData(msg));
            // reset selected check box:
            if (selectedAnswer != -1)
                cbAnswers[selectedAnswer-1].setSelected(false);
            selectedAnswer = -1;
        }
        else if (msg.startsWith("A1:")) {       // first possible answer.
            cbAnswers[0].setText("A: " + extractData(msg));
        }
        else if (msg.startsWith("A2:")) {       // second possible answer.
            cbAnswers[1].setText("B: " + extractData(msg));
        }
        else if (msg.startsWith("A3:")) {       // third possible answer.
            cbAnswers[2].setText("C: " + extractData(msg));
        }
        else if (msg.startsWith("A4:")) {       // fourth possible answer.
            cbAnswers[3].setText("D: " + extractData(msg));
            // got last possible answer, so show the question:
            setContentPane (pMain);
            setVisible(true);
        }
        else if (msg.equals("WATING_PLAYER_FIRST")) { // player not connected yet!
            JOptionPane.showMessageDialog(this,
                    "Player should be connected first!",
                    "Player not connected!",
                    JOptionPane.ERROR_MESSAGE);
            closeApplication(true);     // force to close.
        }
        else if (msg.equals("PLAYER_DISCONECTED")) {  // player connection terminated!
            JOptionPane.showMessageDialog(this,
                    "Player has disconnected!\n" +
                    "Server terminated the game.",
                    "Player disconnected!",
                    JOptionPane.INFORMATION_MESSAGE);
            closeApplication(true);     // force to close.
        }
        else if (msg.equals("TERMINATED")) {        // server terminates our game!
            JOptionPane.showMessageDialog(this,
                    "Server terminated your game for no obvious reason!",
                    "Game terminated!",
                    JOptionPane.ERROR_MESSAGE);
            closeApplication(true);     // force to close.
        }
    }



    /**
    * Sends message to the server.
    *
    * @param msg message to send to the server.
    */
    public void sendToServer(String msg) {
        if (thServer == null)   // if not running any longer then don't send.
             return;

        out.println(msg);
        // Flush the stream and check its error state:
        if (out.checkError())
            System.err.println("Cannot send -> " + msg);
        else
            System.out.println("SEND: " + msg);
    }


    /**
    * Closes this Friend Client station.
    *
    * @param force when true forces to close without dialog box.
    */
    private void closeApplication(boolean force) {

        // if not forced to close, and connected to game -
        //    then ask user:
        if ( (!force) && ALL_CONNECTED ) {
            int result;
            result = JOptionPane.showConfirmDialog(this,
                   "Are you sure you want to close the friend window?\n" +
                   "If you close - server will terminate the game!",
                   "Close anyway?",
                   JOptionPane.YES_NO_OPTION,
                   JOptionPane.WARNING_MESSAGE);
             if (result != 0)   // no, cancel.
                return;
             // otherwise - yes, close.
        }

        // inform server about closure:
        sendToServer("CLOSE");

        thServer = null;  // stop the receiving thread.

        try {
              // close server streams:
              out.close();      // close stream that sends data to server.
              in.close();       // close stream that gets data from server.
              // close socket connected to server:
              if (socket != null)
                 socket.close();
        } catch (Exception e) {}        // ignore all errors on exit.

        // close everything:
        System.exit(0);
    }



    /**
    * Returns data substring after the first ':' char.
    *
    * @param msg the source string.
    * @return the rest of the string after the ':'.
    */
    private static String extractData(String msg) {
            try {
                int i = msg.indexOf(':');   // get index of first ':'.
                return msg.substring(i+1);  // return second token.
            }
            catch(Exception e) {
               return "";               // in case of error return empty string.
            }
    }

}   // end of class.
