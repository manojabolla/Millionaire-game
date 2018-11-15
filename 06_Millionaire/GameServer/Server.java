/*
 Project            :   The Millionaire Game
 File Name          :   Server.java

Author: Alexander Popov
Date  : Saturday, July 20, 2002
Homepage: http://www.geocities.com/emu8086/vb/
*/

import javax.swing.*;
import javax.swing.event.*;   // for ChangeListener of JTabbedPane.
import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
* This class represents a server that runs Millionaire Game(s).
* It gets the connections and starts the game by creating new
* game handlers (TheGame) for each game.
* This class also cares about showing information about each game
* that is running, and allows to terminate games.
*/
public class Server extends JFrame implements ActionListener, ChangeListener, Runnable {

  // a list of the games that are played currently:
  private static Vector games = new Vector();

  // keeps all questions (the same object is used for all games):
  private static QnA qna;

  // number of ports to receive messages from player & friend:
  private static int rec_player_port;
  private static int rec_friend_port;

  // keeps server's address:
  private static InetAddress localHost = null;

  // server sockets for receiving connections or players and friends:
  ServerSocket socketForPlayer = null, socketForFriend = null;

  // pointer to last connected player that waits for a friend:
  Socket playerConnection;
  // pointer to last created game:
  TheGame lastGame;

  // flag, true when server waits for player connection,
  //       false when it waits for friend connection:
  boolean waitForPlayerConnection;

  // visual interface:

  // button for termination of selected game:
  JButton btnTerminate = new JButton("Terminate the game");

  JLabel lblRunning;        // label to show how many games are running.

  JLabel lblWaiting = new JLabel("lblWaiting");     // expected connection status.
  JLabel lblWaitAddress = new JLabel("lblWaitAddress");  // waiting player host.
  JLabel lblWaitPort = new JLabel("lblWaitPort");        // waiting player port.
  JButton btnCancelWait = new JButton("Cancel Waiting"); // button to cancel waiting.


  ImageIcon icon = new ImageIcon("pic.gif");    // picture for tabbedPane.
  JTextArea txtInfo = new JTextArea();          // text area for information.
  JTabbedPane tabbedPane = new JTabbedPane();   // tabs to select games.
  JPanel pInnerTab = new JPanel (new BorderLayout());  // panel inside the tabbedPane.

  // thread that cares about accepting new players:
  Thread thPlayerAccept = null;
  // thread that cares about accepting new friends:
  Thread thFriendAccept = null;
  // thread that cares about updating the game info:
  Thread thUpdateGameInfo = null;


    /**
    * Default Constructor.
    */
    public Server() {

        super ("The Server");       // set window caption.

        // set server sockets:
        try {
            socketForPlayer = new ServerSocket(rec_player_port);
            socketForFriend = new ServerSocket(rec_friend_port);
        } catch (IOException ioe) {
            System.err.println("Cannot open server socket: " + ioe);
            System.exit(0);
        }



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
        btnTerminate.updateUI();
        lblWaiting.updateUI();
        lblWaitAddress.updateUI();
        lblWaitPort.updateUI();
        btnCancelWait.updateUI();
        txtInfo.updateUI();
        tabbedPane.updateUI();
        pInnerTab.updateUI();



        /* The default value is: HIDE_ON_CLOSE,
           we need to ask user "Are you sure?" when there are games running,
           so closing is done manually like JFrame.EXIT_ON_CLOSE */
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // processing window events:
        WindowListener L= new WindowAdapter () {
            public void windowClosing(WindowEvent e) {
                closeApplication();
            }
        };
        addWindowListener(L);

        // prepare the layout:
        JPanel pMain = new JPanel (new BorderLayout());     // main layout.
        JPanel pUpper = new JPanel (new GridLayout(1,2));   // upper layout.
        JPanel pLBL = new JPanel (new GridLayout(5,1));     // for labels.
        JPanel pWaiting = new JPanel (new GridLayout(4,1)); // for waiting info & button.

        lblRunning = new JLabel("Currently running: 0 games.");

        pLBL.add(new JLabel("Server is running on host: " + localHost));
        pLBL.add(new JLabel("Waiting players on port: " + rec_player_port));
        pLBL.add(new JLabel("Waiting friends on port: " + rec_friend_port));
        pLBL.add(new JLabel("Server has " + qna.getNumberOfQuestions() +
                                    " questions to ask."));
        pLBL.add(lblRunning);

        // add labels for waiting player info, and cancel button:
        pWaiting.add(lblWaiting);
        pWaiting.add(lblWaitAddress);
        pWaiting.add(lblWaitPort);
        btnCancelWait.addActionListener(this);
        pWaiting.add(btnCancelWait);

        // upper part conatins server state labels on one side,
        // and current connection state on the other:
        pUpper.add(pLBL);
        pUpper.add(pWaiting);

        JPanel pBtns = new JPanel (new FlowLayout());   // for "Terminate" button.

        btnTerminate.addActionListener(this);
        pBtns.add(btnTerminate);

        // shown inside the tabbedPane:
        pInnerTab.add(txtInfo, BorderLayout.CENTER);
        pInnerTab.add(pBtns, BorderLayout.SOUTH);

        pMain.add(pUpper, BorderLayout.NORTH);
        tabbedPane.addChangeListener(this);
        pMain.add(tabbedPane, BorderLayout.CENTER);
/*
 // debug (just for test of JTabbedPane):
        tabbedPane.addTab("zero!", icon, pInnerTab);
        tabbedPane.setSelectedIndex(0);
        for (int i=1; i<100; i++) {
            tabbedPane.addTab("Hey: " + i, icon, null);
        }
*/

        // set content pane:
        setContentPane (pMain);

        // it doesn't work with our JTabbedPane !!! ---> pack();
        setSize(550, 420);

        // set for waiting for player connection:
        setWaitForPlayer();

        // show the window:
        setVisible(true);

        // start threads:
        thPlayerAccept = new Thread(this);
        thFriendAccept = new Thread(this);
        thUpdateGameInfo = new Thread(this);
        thPlayerAccept.start();             // start accepting new players.
        thFriendAccept.start();             // start accepting new friends.
        thUpdateGameInfo.start();           // start to care about updating the game info.
    }


    /**
    * The run method of that is used by all threads.
    */
    public void run() {

        Thread thisThread = Thread.currentThread();

        // thread that cares about accepting new players:
        while (thPlayerAccept == thisThread) {
            try {
              // wait for player to connect, and then validate connection:
              // use temporary pointer for new connection:
              Socket pc = socketForPlayer.accept();

              if (!waitForPlayerConnection) {
                System.out.println("Player rejected!");
                // send message to player that we are waiting for a friend!:
                terminateSocket(pc, "WATING_PREV_FRIEND");
              }
              else {
                playerConnection = pc;
                System.out.println("Player accepted!");

                  // register new game:
                  addNewTab();
                  lastGame = new TheGame (qna, this);
                  games.addElement(lastGame);
                  lblRunning.setText("Currently running: " + games.size() + " game(s).");

                // connect player to game:
                lastGame.connectPlayer(playerConnection);

                setWaitForFriend();  // now wait for friend (for this game).
              }
            }
            catch (Exception e) {
                 if (thPlayerAccept != null) // not shutting down yet?
                  System.err.println("Accept player -> " + e);
            }
        }


        // thread that cares about accepting new friends:
        while (thFriendAccept == thisThread) {
            try {
              // wait for friend to connect, and then validate connection:
              Socket friendConnection = socketForFriend.accept();

              if (waitForPlayerConnection) {
                  System.out.println("Friend rejected!");
                  // send message to friend that we are waiting for a player:
                  terminateSocket(friendConnection, "WATING_PLAYER_FIRST");
              }
              else {
                  System.out.println("Friend accepted!");

                  // connect friend to game:
                  lastGame.connectFriend(friendConnection);

                  setWaitForPlayer(); // now wait for player (for another game).
              }
            }
            catch (Exception e) {
                if (thFriendAccept != null) // not shutting down yet?
                  System.err.println("Accept friend -> " + e);
            }
        }

        // thread that cares about updating the game info:
        // automatic update of info for selected game:
        while (thUpdateGameInfo == thisThread) {
            if (games.size() > 0)   // no games?
                showGameData();
            thUpdateGameInfo.yield(); // this thread isn't so important, think of others.
            pause(1200);    // update every 1.2 of a second.
        }

    }  // end of run().



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
    * Set server to wait for player connection.
    */
    private void setWaitForPlayer() {
        waitForPlayerConnection = true;
        lblWaiting.setText("Waiting for a PLAYER connection...");
        btnCancelWait.setVisible(false);
        lblWaitAddress.setText("");
        lblWaitPort.setText("");
    }


    /**
    * Set server to wait for friend connection.
    */
    private void setWaitForFriend() {
        waitForPlayerConnection = false;
        lblWaiting.setText("Waiting for a FRIEND connection...");
        btnCancelWait.setVisible(true);
        lblWaitAddress.setText("player host: " + playerConnection.getInetAddress());
        lblWaitPort.setText("player port: " + playerConnection.getPort());
    }


    /**
    * Processes clicks on buttons.
    *
    * @param e the ActionEvent object.
    */
    public void actionPerformed(ActionEvent e){
      Object src=e.getSource();

        if (src == btnTerminate) {      // terminate the game
           try {
                TheGame t;      // temporary pointer.
                t = (TheGame) games.get(tabbedPane.getSelectedIndex());
                t.sendToPlayer("TERMINATED");
                t.sendToFriend("TERMINATED");
                removeGame(t);
           }
           catch (ArrayIndexOutOfBoundsException ae) {
                txtInfo.setText( "No game with index: " + tabbedPane.getSelectedIndex());
           }
        }
        else if (src == btnCancelWait) {    // cancel waiting of a player
            // close waiting player connection:
            lastGame.sendToPlayer("TERMINATED");
            removeGame(lastGame);
            setWaitForPlayer(); // wait for another player.
        }
    }


    /**
    * processes events for the JTabbedPane.
    *
    * @param e the ChangeEvent object.
    */
    public void stateChanged(ChangeEvent e) {
        Object src=e.getSource();

        if (src == tabbedPane) {    // click on a tab
            showGameData();
        }
    }

    /**
    * Adds new tab to tabbedPane,
    * the same component is displayed for all tabs,
    * so if there are not tabs the pInnerTab is added.
    */
    private void addNewTab() {
          try {
              int curTabs = tabbedPane.getTabCount();
              if (curTabs == 0) { // no tabs in tabbedPane?
                tabbedPane.addTab("Game 1", icon, pInnerTab);
                tabbedPane.setSelectedIndex(0);
              }
              else {
                // add empty tab, component from Tab#0 will be used:
                tabbedPane.addTab("Game " + (curTabs+1), icon, null);
                // activate last tab (newly added):
                tabbedPane.setSelectedIndex(curTabs);
              }
          }
          catch (Exception e) {
               System.err.println("addNewTab() -> " + e);
          }
    }


    /**
    * Removes the last tab from tabbedPane,
    * (used when game is terminated).
    */
    private void removeLastTab() {
          try {
              int curTabs = tabbedPane.getTabCount();
              tabbedPane.removeTabAt(curTabs-1);
              // activate first tab (if any):
              if (curTabs > 1)
                tabbedPane.setSelectedIndex(0);
          }
          catch (Exception e) {
               System.err.println("removeLastTab() -> " + e);
          }
    }

    /**
    * Updates the text box that shows the information for
    * currectly selected game (by the tabbedPane).
    */
    private void showGameData() {
           try {
                TheGame t;  // temporary pointer.
                t = (TheGame) games.get(tabbedPane.getSelectedIndex());
                String sInfo = t.getInfo();
                if (!sInfo.equals( txtInfo.getText() )) // update text only when required.
                    txtInfo.setText(sInfo);
           }
           catch (ArrayIndexOutOfBoundsException ae) {
                txtInfo.setText( "No game with index: " + tabbedPane.getSelectedIndex());
           }
    }


    /**
    * Main function, from where Server starts.
    */
    public static void main(String args[]) {

        // validate parameter count:
        if (args.length!=2) {
            System.err.println("Wrong parameters!  Usage:");
            System.err.println("java Server <player_port> <friend_port>");
            System.exit(1);
        }

        // create Q&A and load questions & answers into it:
        qna = new QnA("Questions.txt");

        // process parameters:
        try {
            rec_player_port = Integer.parseInt(args[0]);
            rec_friend_port = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException e) {
            System.err.println("Wrong number for a port -> " + e);
            System.exit(1);
        }

        // get address of the server:
        try {
            localHost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            System.out.println("Unknown host - probably localhost with no IP!");
            // no exit, since can work on "localhost" without internet.
        }

        // print out the info (the same info is also shown on the server's
        //   GUI window).
        System.out.println("Server is running on host: " + localHost);
        System.out.println("Waiting players on port: " + rec_player_port);
        System.out.println("Waiting friends on port: " + rec_friend_port);

        // create & start server GUI engine:
        new Server();

    } // end of main().


    /**
    * Closes the server, cares about closing all sockets,
    * and informing the players and friends that are running
    * currenlty about the shutdown, and terminating games.
    */
    private void closeApplication() {

        // ask user if he/she is sure to shut down the server when
        //      there are games running:
        if (games.size() > 0) {
            int result;
            result = JOptionPane.showConfirmDialog(this,
                   "Are you sure you want to shut down the SERVER?\n" +
                   "All games will be terminated!",
                   "Close anyway?",
                   JOptionPane.YES_NO_OPTION,
                   JOptionPane.WARNING_MESSAGE);
             if (result != 0)   // no, cancel.
                return;
             // otherwise - yes, close.

            // send termination message to all players and friends:
            for (int i=games.size()-1; i>=0; i--) {
                TheGame t;
                t = (TheGame) games.get(i);
                t.sendToPlayer("TERMINATED");
                t.sendToFriend("TERMINATED");
                removeGame(t);
            }
        }


        // if there is a player waiting for a friend connection,
        //        inform it about termination:
        if (!waitForPlayerConnection) { // waiting for player?
            lastGame.sendToPlayer("TERMINATED");
            removeGame(lastGame);
        }

        // stop the server's threads:
        thPlayerAccept = null;
        thFriendAccept = null;

        // close sockets:
        try {
            socketForPlayer.close();
            socketForFriend.close();
        }
        catch (IOException e) {
            System.err.println("On close -> " + e);
        }
        // close everything:
        System.exit(0);
    }


    /**
    * Terminates the game.
    *
    * @param gameToDelete the pointer to game to be deleted.
    */
    public void removeGame(TheGame gameToDelete) {

        if (games.contains(gameToDelete)) {  // check if not removed already.
            if (gameToDelete.isWaitingForFriend())
                setWaitForPlayer(); // terminate waiting.

            // close sockets, streams, stop threads:
            gameToDelete.closeEverything();
            games.remove(gameToDelete);   // remove from vector.
            lblRunning.setText("Currently running: " + games.size() + " game(s).");
            removeLastTab();
        }
    }


    /**
    * Closes the socket, before closing the socket sends message to it.
    * Used to terminate unwanted connections (for example when friend tries
    * to connect before player).
    *
    * @param s the socket to be closed.
    * @param msg message to be sent before closing.
    */
    private void terminateSocket(Socket s, String msg) {
        PrintStream out;
        try {
            out = new PrintStream(s.getOutputStream());
            out.println(msg);
            // Flush the stream and check its error state:
            if (out.checkError())
                System.out.println("terminateSocket -> Cannot send -> " + msg);
            out.close();
        } catch (IOException e) {
            System.err.println("terminateSocket -> " + e);
        }
        finally {
            try {
                s.close();
            } catch (IOException ioe) {}
        }
    }

}   // end of class.
