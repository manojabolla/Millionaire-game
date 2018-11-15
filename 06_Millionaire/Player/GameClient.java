/*
 Project            :   The Millionaire Game
 File Name          :   GameClient.java

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
* This class represents a client side Millionaire Game station.
*/
public class GameClient extends JFrame implements ActionListener, Runnable {

    // used with moneyIndex to get the earned money sum, and
    //    to set money boxes:
    String moneySum[] = {"0", "100", "200", "300", "500", "1,000", "2,000", "4,000",
                         "8,000", "16,000", "32,000", "64,000",
                         "125,000", "250,000", "500,000", "1,000,000"};

    // streams for communication with the server:
    static PrintStream out;         // for sending to server.
    static BufferedReader in;       // for getting data from server.
    // socket for connection with the server:
    static Socket socket = null;


    // label to show status messages:
    JLabel lblStatus = new JLabel("Connected! Waiting for a friend...", JLabel.CENTER);
    // label to show questions:
    JLabel lblQuestion = new JLabel(" ", JLabel.CENTER);
    // label to just to show "wheels" label:
    JLabel lblWheels = new JLabel("  Wheels");
    // label to show how many friend wheels are left:
    JLabel lblFriend = new JLabel("  Friend:   ?");
    // label to show how many 50:50 wheels are left:
    JLabel lbl5050 = new JLabel("  50:50 :   ?");
    // label to show how many wheels of the public are left:
    JLabel lblThePublic = new JLabel("  Public:   ?");
    // answer boxes for possible answers:
    AnswerBox cbAnswers[] = new AnswerBox[4];
    // buttons:
    JButton btnFinal, btnFriend, btn5050, btnThePublic, btnRetire;
    // money boxes:
    MoneyBox cbMoney[] = new MoneyBox[15];
    // response chart for the public:
    Chart publicChart = new Chart();


    int selectedAnswer = -1; // last clicked answer.

    int moneyIndex = 0;    // money index, updated by server.

    // will be true when friend will also be connected:
    public boolean ALL_CONNECTED = false;

    // thread which cares about receiving data from server:
    Thread thServer = null;


    /**
    * Default Constructor.
    */
    public GameClient()
    {
        super ("The Millionaire Game");

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
        lblStatus.updateUI();
        lblQuestion.updateUI();
        lblWheels.updateUI();
        lblFriend.updateUI();
        lbl5050.updateUI();
        lblThePublic.updateUI();

        // processing window events:
        WindowListener L= new WindowAdapter () {
            public void windowClosing(WindowEvent e) {
                closeApplication();
            }
        };
        addWindowListener(L);

        // prepare the layout:
        JPanel  pMain = new JPanel ();
        pMain.setLayout(new BorderLayout());


        JPanel pUpper = new JPanel();           // for labels.
        pUpper.setLayout(new GridLayout(2,1));

        JPanel  pCenter = new JPanel ();        // for answers.
        pCenter.setLayout(new BorderLayout());

        JPanel pAnswers = new JPanel();           // for answers & the public chart.
        pAnswers.setLayout(new GridLayout(2,2, 20,20));

        // for buttons:
        JPanel pButtons = new JPanel(new FlowLayout(FlowLayout.CENTER,10,10));

        JPanel pLeft = new JPanel(new GridLayout(15,1, 10,10));  // for wheels counters.
        JPanel pRight = new JPanel(new GridLayout(15,1, 1,1));   // for money boxes.

        pUpper.add(lblStatus);
        pUpper.add(lblQuestion);

        pLeft.add(lblWheels);
        pLeft.add(lblFriend);
        pLeft.add(lbl5050);
        pLeft.add(lblThePublic);

        // make & add check boxes for answers:
        cbAnswers[0] = new AnswerBox ("A:",pAnswers);   // first possible answer.
        pAnswers.add(cbAnswers[0]);
        cbAnswers[0].addActionListener(this);

        cbAnswers[1] = new AnswerBox ("B:",pAnswers);   // second possible answer.
        pAnswers.add(cbAnswers[1]);
        cbAnswers[1].addActionListener(this);

        cbAnswers[2] = new AnswerBox ("C:",pAnswers);   // third possible answer.
        pAnswers.add(cbAnswers[2]);
        cbAnswers[2].addActionListener(this);

        cbAnswers[3] = new AnswerBox ("D:",pAnswers);   // fourth possible answer.
        pAnswers.add(cbAnswers[3]);
        cbAnswers[3].addActionListener(this);


        // make & add buttons:
        btnFinal = new JButton("Final");        // for confirmation of answer.
        pButtons.add(btnFinal);
        btnFinal.addActionListener(this);

        btnFriend = new JButton("Friend");      // for friend request.
        pButtons.add(btnFriend);
        btnFriend.addActionListener(this);

        btn5050 = new JButton("50:50");         // for 50:50 request.
        pButtons.add(btn5050);
        btn5050.addActionListener(this);

        btnThePublic = new JButton("The Public");   // for request of the public.
        pButtons.add(btnThePublic);
        btnThePublic.addActionListener(this);

        btnRetire = new JButton("Retire");      // for request to retire.
        pButtons.add(btnRetire);
        btnRetire.addActionListener(this);


        enableButtons(false);     // disable all buttons.

        // make money boxes:
        for (int i=14; i>=0; i--) {
            cbMoney[i] = new MoneyBox (moneySum[i+1], pRight);
            pRight.add(cbMoney[i]);
        }
        cbMoney[4].setStopStation();    // "1,000"
        cbMoney[9].setStopStation();    // "32,000"

        pCenter.add (pAnswers, BorderLayout.SOUTH);

        // add response chart for the public:
        publicChart.setVisible(false);  // shown only when required.
        pCenter.add (publicChart, BorderLayout.CENTER);

        // Add sub layouts to main layout:
        pMain.add(pUpper, BorderLayout.NORTH);
        pMain.add(pButtons, BorderLayout.SOUTH);
        pMain.add(pCenter, BorderLayout.CENTER);
        pMain.add(pLeft, BorderLayout.WEST);
        pMain.add(pRight, BorderLayout.EAST);

        // -------- set colors and fonts:
        pButtons.setBackground(Color.blue);
        pAnswers.setBackground(Color.blue);
        pCenter.setBackground(Color.blue);
        pUpper.setBackground(Color.blue);
        pRight.setBackground(Color.blue);
        pLeft.setBackground(Color.blue);
        lblStatus.setForeground(Color.yellow);
        lblQuestion.setForeground(Color.white);
        lblWheels.setForeground(Color.green);
        lblFriend.setForeground(Color.white);
        lbl5050.setForeground(Color.white);
        lblThePublic.setForeground(Color.white);
        Font f1 = new Font("Arial", Font.BOLD, 12);
        Font f2 = new Font("Arial", Font.BOLD, 20);
        lblStatus.setFont(f1);
        lblQuestion.setFont(f2);
        lblWheels.setFont(f1);
        lblFriend.setFont(f1);
        lbl5050.setFont(f1);
        lblThePublic.setFont(f1);

        // show the window:
        setContentPane (pMain);
        setSize(590,450);
        setVisible(true);


        // start the thread that cares about receiving data from server:
        thServer = new Thread (this);
        thServer.start();

    }



    /**
    * Main function, from where GameClient starts.
    */
    public static void main(String args[]) {

        // validate parameters:
        if (args.length != 2) {
            System.err.println("Usage: java GameClient <host> <friend_port>");
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
        System.out.println("Player's local address: " + localHost);

        // create GameClient and show its window:
        new GameClient();
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

        // thread which cares about receiving data from server:
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
              closeApplication(); // force to close.
            }
        }

    } // end of run().



    /**
    * Processes clicks on buttons and answer boxes.
    *
    * @param e the ActionEvent object.
    */
    public void actionPerformed(ActionEvent e){
      Object src=e.getSource();

      if (src == btnFinal) {            // confirmation of answer.
          if (selectedAnswer != -1) {
            enableButtons(false);     // disable all buttons.
            lblStatus.setText("Waiting for server's response...");
            sendToServer("FINAL:" + selectedAnswer);
          }
          else {
             JOptionPane.showMessageDialog(this,
                   "Please select an answer!",
                   "Not selected!", JOptionPane.ERROR_MESSAGE);
          }
      }
      else if  (src == btnFriend) {     // friend request.
          enableButtons(false);     // disable all buttons.
          lblStatus.setText("Waiting for friend's response...");
          sendToServer("FRIEND");
      }
      else if (src == btn5050) {        // 50:50 request.
          enableButtons(false);     // disable all buttons.
          lblStatus.setText("Waiting for server's response...");
          sendToServer("50:50");
      }
      else if (src == btnThePublic) {   // request of the public.
          enableButtons(false);     // disable all buttons.
          publicChart.setChart(0, 0, 0, 0); // reset the chart from any data.
          publicChart.setVisible(true);     // show the chart.
          lblStatus.setText("Waiting for server's response...");
          sendToServer("THE_PUBLIC");
      }
      else if  (src == btnRetire) {     // request to retire.
          enableButtons(false);     // disable all buttons.
          lblStatus.setText("Waiting for server's response...");
          sendToServer("RETIRE");
      }
      else{

          // clicks on check-boxes:
          for (int i=0; i<4; i++)
            if (cbAnswers[i] == src) {  // click on one of the answer boxes.
                selectedAnswer = i+1; // +1 because first is 1.

                  // deselect any other selected check-boxes
                  //      except the last one:
                  for (int k=0; k<4; k++) {
                      if (k != i)       // not equal to selected.
                        cbAnswers[k].setSelected(false);
                      else
                        cbAnswers[k].setSelected(true);
                  }

                return;         // no need to continue.
            }

       }

    }  // end of actionPerformed().



    /**
    * Sets the money index and updates money boxes.
    * Receives value in String format, so does the parsing,
    * in case of error prints out to error stream and does nothing.
    *
    * @param sm value for the bar in String format (should be between "0" and "15").
    */
    private void setMoney(String sm) {
        int k;
        try {
            k = Integer.parseInt(sm);
        }
        catch(NumberFormatException e) {
            System.err.println("setMoney()--> " + e);
            return;
        }

        // validation (should be between 0 and 15):
        if ((k > 15) || (k < 0)) {
            System.err.println("setMoney() out of bounds --> " + sm);
            return;
        }

        moneyIndex = k;  // store in global variable.

        updateEarnedMoney();
    }



    /**
    * Updates the money boxes of earned money according
    *  to moneyIndex.
    */
    private void updateEarnedMoney() {
        // select earned stations:
        for (int i=0; i<moneyIndex; i++)
            cbMoney[i].setSelected(true);

        // clear all the others:
        for (int i=moneyIndex; i<15; i++)
            cbMoney[i].setSelected(false);
    }



    /**
    * This method is triggered when there is message from server.
    * It processes all received data.
    *
    * @param msg what server has to say to player.
    */
    private void gotMessageFromServer(String msg) {
        System.out.println("SERVER: [" + msg + "]");

        if (msg.equals("ALL_CONNECTED")) {   // game started.
            ALL_CONNECTED = true;
            sendToServer("RESTART");        // reset everything.
            sendToServer("QUESTION");       // ask for first question.
        }
        else if (msg.startsWith("Q:")) {    // question.
            prepareForNextQuestion();
            lblQuestion.setText(extractData(msg));
        }
        else if (msg.startsWith("A1:")) {       // first possible answer.
            cbAnswers[0].setText(extractData(msg));
        }
        else if (msg.startsWith("A2:")) {       // second possible answer.
            cbAnswers[1].setText(extractData(msg));
        }
        else if (msg.startsWith("A3:")) {       // third possible answer.
            cbAnswers[2].setText(extractData(msg));
        }
        else if (msg.startsWith("A4:")) {       // fourth possible answer.
            cbAnswers[3].setText(extractData(msg));
            // got last possible answer, so enable buttons:
            enableButtons(true);
            lblStatus.setText(" ");          // clear the status.
        }
        else if (msg.startsWith("MONEY:")) {    // earned money index.
            setMoney(extractData(msg));
        }
        else if (msg.startsWith("FRIEND:")) {  // how many friend tips left.
            lblFriend.setText ("  Friend: " + extractData(msg));
        }
        else if (msg.startsWith("50_50:")) {  // how many 50:50 tips left.
            lbl5050.setText ("  50:50 : " + extractData(msg));
        }
        else if (msg.startsWith("THE_PUBLIC:")) {  // how many tips of the public left.
            lblThePublic.setText ("  Public: " + extractData(msg));
        }
        else if (msg.equals("NOFRIEND!")) {     // error - no more friend tips!
               lblStatus.setText(" ");          // clear the status.
               JOptionPane.showMessageDialog(this,
                     "Server says you have no more friend tips, sorry.",
                   "No more friend tips", JOptionPane.ERROR_MESSAGE);
               enableButtons(true);
        }
        else if (msg.equals("NO50:50!")) {      // error - no more 50:50 tips!
               lblStatus.setText(" ");          // clear the status.
               JOptionPane.showMessageDialog(this,
                     "Server says you have no more 50:50 tips, sorry.",
                   "No more 50:50 tips", JOptionPane.ERROR_MESSAGE);
               enableButtons(true);
        }
        else if (msg.equals("50:50TWICE!")) {   // error - 50:50 done twice already!
               lblStatus.setText(" ");          // clear the status.
               JOptionPane.showMessageDialog(this,
                 "Server says you have done 50:50 twice already for this question!",
                 "Cannot do 50:50 three times for one question", JOptionPane.ERROR_MESSAGE);
               enableButtons(true);
        }
        else if (msg.equals("NO_PUBLIC!")) {    // error - no more tips of the public!
               lblStatus.setText(" ");          // clear the status.
               JOptionPane.showMessageDialog(this,
                     "Server says you have no more tips of the public, sorry.",
                   "No more tips of the public!", JOptionPane.ERROR_MESSAGE);
               enableButtons(true);
               publicChart.setVisible(false);   // hide the chart (it's empty).
        }
        else if (msg.startsWith("GREEN:")) {   // set right answer be green.
            try { // generally, server sends the right data, check anyway
                int k = Integer.parseInt(extractData(msg));
                cbAnswers[k-1].setCorrect();
            } catch (Exception e) {
                System.out.println("GREEN: -> " + e);
            }
        }
        else if (msg.equals("RIGHT!")) {      // last question was answered correctly!
             lblStatus.setText(" ");          // clear the status.
             JOptionPane.showMessageDialog(this,
                   "This is the right answer!",
                   "Congratulations!", JOptionPane.INFORMATION_MESSAGE);
             lblStatus.setText("Waiting for server's response...");
             sendToServer("QUESTION");       // ask for next question.
        }
        else if (msg.equals("MILLION!")) {    // correct, and you won a million!
             lblStatus.setText(" ");          // clear the status.
             JOptionPane.showMessageDialog(this,
                   "You won a MILLION!",
                   "Congratulations! - Game over.", JOptionPane.INFORMATION_MESSAGE);
             askToRestart();
        }
        else if (msg.startsWith("WRONG!:")) { // last question was answered WRONG!
             lblStatus.setText(" ");          // clear the status.
             JOptionPane.showMessageDialog(this,
                            "Your answer is wrong!\n Correct answer is " +
                                indexToLetter( extractData(msg) ) +
                            "\nYou won $" + moneySum[moneyIndex],
                   "Game over!", JOptionPane.ERROR_MESSAGE);
             askToRestart();
        }
        else if (msg.startsWith("RETIRED:")) { // retire request confirmed.
             lblStatus.setText(" ");           // clear the status.
             JOptionPane.showMessageDialog(this,
                   "You won $" + moneySum[moneyIndex] + ",\n" +
                   "and the right answer for the last question is " +
                            indexToLetter ( extractData(msg) ),
                   "Retired!", JOptionPane.INFORMATION_MESSAGE);
             askToRestart();
        }
        else if (msg.startsWith("FRIEND_ANSWER:")) {    // getting friend answer.
            lblStatus.setText("Your friend thinks answer is: " +
                                indexToLetter( extractData(msg) ) );
            // got answer from friend, so enable the buttons:
            enableButtons(true);
        }
        else if (msg.equals("FRIEND_DONTKNOW")) {       // friend doesn't know!
            lblStatus.setText("Your friend doesn't know the answer...");
            // friend says he doesn't know, so enable the buttons:
            enableButtons(true);
        }
        else if (msg.equals("FRIEND_TIMEUP")) {         // friend's time is up!
            lblStatus.setText("Your friend failed to give answer in 30 seconds...");
            // time is up, so enable the buttons:
            enableButtons(true);
        }
        else if (msg.startsWith("TIMER:")) {            // timer state for friend.
            lblStatus.setText("Your friend has: " + extractData(msg) + " seconds.");
        }
        else if (msg.startsWith("Message from Server:")) {
            lblStatus.setText(msg);
        }
        else if (msg.equals("FRIEND_DISCONECTED")) {    // error - friend disconected.
            // though the game can be continued without the friend,
            // we decided to end the game to avoid processing the state
            // when friend is required and it's not there....
            lblStatus.setText(" ");          // clear the status.
            JOptionPane.showMessageDialog(this,
                    "Your Friend has disconnected!\n" +
                    "Server terminated the game.",
                    "Friend disconnected!",
                    JOptionPane.ERROR_MESSAGE);
            closeApplication();     // force to close.
        }
        else if (msg.equals("WATING_PREV_FRIEND")) {    // error - server not ready!
            lblStatus.setText(" ");          // clear the status.
            JOptionPane.showMessageDialog(this,
                    "Server still waits for a connection of Friend" +
                    " for previous game that has not connected yet!\n" +
                    "Try again later...",
                    "Server not ready!",
                    JOptionPane.ERROR_MESSAGE);
            closeApplication();     // force to close.
        }
        else if (msg.equals("TERMINATED")) {        // server terminates our game!
            lblStatus.setText(" ");          // clear the status.
            JOptionPane.showMessageDialog(this,
                    "Server terminated your game for no obvious reason!",
                    "Game terminated!",
                    JOptionPane.ERROR_MESSAGE);
            closeApplication();     // force to close.
        }
        else if (msg.startsWith("CANCEL1:")) {   // 50:50 first canceled answer
            try {
                String index = extractData(msg);
                cbAnswers[Integer.parseInt(index)-1].setText(" ");  // clear.
            } catch (Exception e) {
                System.err.println("CANCEL1: -> " + e);
            }
        }
        else if (msg.startsWith("CANCEL2:")) {   // 50:50 another canceled answer
            try {
                String index = extractData(msg);
                cbAnswers[Integer.parseInt(index)-1].setText(" ");  // clear.
            } catch (Exception e) {
                System.err.println("CANCEL2: -> " + e);
            }
            // clear the status & enable the buttons:
            lblStatus.setText(" ");
            enableButtons(true);
        }
        else if (msg.startsWith("BAR1:")) {  // help of the public (% for "A").
            publicChart.setChart(extractData(msg), 1);
        }
        else if (msg.startsWith("BAR2:")) {  // help of the public (% for "B").
            publicChart.setChart(extractData(msg), 2);
        }
        else if (msg.startsWith("BAR3:")) {  // help of the public (% for "C").
            publicChart.setChart(extractData(msg), 3);
        }
        else if (msg.startsWith("BAR4:")) {  // help of the public (% for "D").
            publicChart.setChart(extractData(msg), 4);
            // assumed that "BAR4:?" is received as the last bar, so
            //   clear the status & enable the buttons:
            lblStatus.setText(" ");
            enableButtons(true);
        }
    }



    /**
    * Sends message to the server.
    *
    * @param msg message to send to the server.
    */
    public void sendToServer(String msg) {
        if (thServer == null)   // shuting down?
            return;

        /* PrintStream never throws an IOException;
          instead, exceptional situations merely set an internal
          flag that can be tested via the checkError method.
        */
        out.println(msg);

        // Flush the stream and check its error state:
        if (out.checkError())
            System.err.println("Cannot send -> " + msg);
        else
            System.out.println("SEND: " + msg);
    }



    /**
    * Asks user if he/she want's to restart the game.
    * If the answer is "yes", starts new game.
    */
    private void askToRestart() {
            int result;
            result = JOptionPane.showConfirmDialog(this,
                       "Would you like to play again?",
                       "Play again?",
                       JOptionPane.YES_NO_OPTION,
                       JOptionPane.QUESTION_MESSAGE);

            if (result == 0) {  // yes, restart.
                lblStatus.setText("Waiting for server's response...");
                sendToServer("RESTART");        // reset everything.
                sendToServer("QUESTION");       // ask for a question.
            }
            else {              // no, close connection.
                closeApplication(); // close connection, and exit.
            }
    }



    /**
    * Closes this Game Client station.
    */
    private void closeApplication() {
        // inform the server:
        sendToServer("CLOSE");

        thServer = null;  // stop the thread.

        try {
              // close server streams:
              out.close();      // close stream that sends data to server.
              in.close();       // close stream that gets data from server.
              // close socket connected to server:
              if (socket != null)
                 socket.close();
        } catch (IOException e) {}

        // close everything:
        System.exit(0);
    }



    /**
    * Enables (or disables) all buttons.
    *
    * @param b when true enables, otherwise disables.
    */
    private void enableButtons (boolean b) {
        btnFinal.setEnabled(b);
        btnFriend.setEnabled(b);
        btn5050.setEnabled(b);
        btnThePublic.setEnabled(b);
        btnRetire.setEnabled(b);
    }



    /**
    * Prepares the GUI for the next question,
    * hides the chart, resets question field, and possible
    * answers.
    */
    private void prepareForNextQuestion() {
        publicChart.setVisible(false);

        lblQuestion.setText(" ");

        for (int i=0; i<cbAnswers.length; i++) {
            cbAnswers[i].setText("");
            cbAnswers[i].setSelected(false); // reset selected check box.
        }

        selectedAnswer = -1;
    }




    /**
    * Returns the letter representaion of a answer box number.
    * index can be from "1" to "4", and
    * if something goes wrong returns "?".
    *
    * @param index the index of a answer box (in String format).
    * @return the letter "A", "B", "C" or "D".
    */
    private String indexToLetter(String index) {
        String sANSWERS[] = {"\"A\"", "\"B\"", "\"C\"", "\"D\""};
        try {
            return sANSWERS[Integer.parseInt(index)-1];
        } catch (Exception e) {
            System.err.println("indexToLetter -> " + e);
            return "?";
        }
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
