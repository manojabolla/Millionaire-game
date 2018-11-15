/*
 Project            :   The Millionaire Game
 File Name          :   TheGame.java

Author: Alexander Popov
Date  : Saturday, July 20, 2002
Homepage: http://www.geocities.com/emu8086/vb/
*/

import java.io.*;
import java.net.*;
import java.util.*;

/**
* This class represents a single server side Millionaire Game.
* This class cares about comminication with the player & his friend,
* sending questions and checking right answers.
*/
public class TheGame extends Thread {

    // used with moneyIndex to get the earned money sum:
    String moneySum[] = {"0", "100", "200", "300", "500", "1,000", "2,000", "4,000",
                         "8,000", "16,000", "32,000", "64,000",
                         "125,000", "250,000", "500,000", "1,000,000"};

    // sockets for player & friend connections:
    Socket playerConnection=null, friendConnection=null;
    // streams for communication:
    BufferedReader inPlayer=null, inFriend=null;
    PrintStream outPlayer=null, outFriend=null;

    // thread for getting data from player:
    Thread thPlayer = null;
    // thread for getting data from friend:
    Thread thFriend = null;

    private QnA _qna;          // pointer to questions and answers.

    private Server _ourServer; // pointer to owner server class (used to remove game).

    int moneyIndex = 0;      // $0.
    int friendNumber = 0;    // number of friend tips.
    int fifty50Number = 0;   // number of 50:50 tips.
    int thePublicNumber = 0; // number of tips of the public.
    // canceled answers when 50:50 is used:
    // (can be used several times in one game, so 3 possible answers can be
    //  canceled)
    int wrong1 = 0;     // first random wrong answer.
    int wrong2 = 0;     // second random wrong answer.
    int wrong3 = 0;     // third "random" wrong answer.

    // this will be the timer for a friend's answer,
    // thTimer is started when timing is required, and stoped after that:
    Thread thTimer = null;
    // string for keeping the response of a friend before send,
    //   to make it synhronized it is sent out of the same timer thread:
    String sFriendResponse = "";
    // keeps timer value (0 to 30 seconds):
    int iTimerValue;

    // used to generate random numbers (for questions, 50:50, The Public):
    private Random rand = new Random();

    // index of currect question of the game:
    int currentQuestion=-1;

    // stores indexes of asked questions (in string format):
    String askedQuestions = "";



    /**
    * Constructor.
    *
    * @param qna pointer to Q&A object with loaded questions.
    * @param ourServer pointer to owner Server class.
    */
    public TheGame(QnA qna, Server ourServer) {
        _qna = qna;
        _ourServer = ourServer;
    }



    /**
    * Connects player to this game.
    *
    * @param playerConnection socket of player to be connected.
    */
    public void connectPlayer(Socket playerConnection) {
        this.playerConnection = playerConnection;

        // open streams for communication:
        try {
            inPlayer =
                new BufferedReader(new InputStreamReader(playerConnection.getInputStream()));
            outPlayer = new PrintStream(playerConnection.getOutputStream());
        } catch (IOException e) {
            System.err.println("connectPlayer -> " + e);
        }

        // thread to receive messages from player:
        thPlayer  = new Thread (this);
        thPlayer.start();
    }



    /**
    * Connects friend to this game and starts the game process.
    * Should be called only after connectPlayer() is done.
    *
    * @param friendConnection the socket of friend to be connected.
    */
    public void connectFriend(Socket friendConnection) {
        this.friendConnection = friendConnection;

        // open streams for communication:
        try {
            inFriend =
                new BufferedReader(new InputStreamReader(friendConnection.getInputStream()));
            outFriend = new PrintStream(friendConnection.getOutputStream());
        } catch (IOException e) {
            System.err.println("connectFriend -> " + e);
        }

        // thread to receive messages from friend:
        thFriend  = new Thread (this);
        thFriend.start();


        // assumed that player already connected, so start the game:

        System.out.println("Starting game...");

        // inform both sides that connection is successful:
        sendToFriend("ALL_CONNECTED");
        sendToPlayer("ALL_CONNECTED");
    }



    /**
    * The run method of that is used by a thread.
    */
    public void run() {

        Thread thisThread = Thread.currentThread();


        // thread to receive messages from player:
        while (thPlayer == thisThread) {
          String inputLine=null;
          try {
                inputLine = inPlayer.readLine();
          } catch (IOException ioe) {
              if (thPlayer != null) { // game not over yet?
                  System.err.println("inPlayer.readLine() -> " + ioe);
                  // inform friend that player has disconected:
                  sendToFriend("PLAYER_DISCONECTED");
                  // remove current game from Server's game list:
                  _ourServer.removeGame(this);
              }
          }
          if (inputLine != null)
            playerTalks(inputLine);      // trigger "event".
        }


        // thread to receive messages from friend:
        while (thFriend == thisThread) {
          String inputLine=null;
          try {
              inputLine = inFriend.readLine();
          } catch (IOException ioe) {
              if (thFriend != null) { // game not over yet?
                  System.err.println("inFriend.readLine() -> " + ioe);
                  // inform player that friend has disconected:
                  sendToPlayer("FRIEND_DISCONECTED");
                  // remove current game from Server's game list:
                  _ourServer.removeGame(this);
              }
          }
          if (inputLine != null)
            friendTalks(inputLine);      // trigger "event".
        }


        // thead for the timing of friend's response:
        while (thTimer == thisThread) {
            if (iTimerValue < 0) {
                thTimer = null;     // stop the timer.
                sendToPlayer("FRIEND_TIMEUP"); // time is up.
                sendToFriend("TIMEUP");
            }
            else {
                if (sFriendResponse.equals("")) {  // no response yet?
                    // send the same timer value to player and friend:
                    sendToPlayer("TIMER:" + iTimerValue);
                    sendToFriend("TIMER:" + iTimerValue);
                }
                else {
                    thTimer = null;   // stop the timer.
                    sendToPlayer(sFriendResponse); // send reply to player.
                }
            }
            pause(1000);    // 1 second delay.
            iTimerValue--;
        }

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
    * Sends current question to player.
    */
    private void sendQuestionToPlayer() {
        resetCanceled5050();      // clear canceled answers flags.
        sendToPlayer("Q:" + _qna.getQuestion(currentQuestion));
        sendToPlayer("A1:" + _qna.getAnswer(currentQuestion, 1));
        sendToPlayer("A2:" + _qna.getAnswer(currentQuestion, 2));
        sendToPlayer("A3:" + _qna.getAnswer(currentQuestion, 3));
        sendToPlayer("A4:" + _qna.getAnswer(currentQuestion, 4));
    }



    /**
    * Sends current question to friend.
    */
    private void sendQuestionToFriend() {
        sendToFriend("Q:" + _qna.getQuestion(currentQuestion));
        sendToFriend("A1:" + _qna.getAnswer(currentQuestion, 1));
        sendToFriend("A2:" + _qna.getAnswer(currentQuestion, 2));
        sendToFriend("A3:" + _qna.getAnswer(currentQuestion, 3));
        sendToFriend("A4:" + _qna.getAnswer(currentQuestion, 4));
    }



    /**
    * Sends current game state to player.
    */
    private void sendState() {
        sendToPlayer("MONEY:" + moneyIndex);
        sendToPlayer("FRIEND:" + friendNumber);         // friend tips left.
        sendToPlayer("50_50:" + fifty50Number);         // 50:50 tips left.
        sendToPlayer("THE_PUBLIC:" + thePublicNumber);  // tips of the public left.
    }



    /**
    * This method is triggered by thread "thPlayer" when
    * there is a message from player.
    * Processes the message and makes an appropriate reaction to it.
    *
    * @param msg what player has to say.
    */
    public void playerTalks(String msg) {

        if (thPlayer == null) {  // game over?
            System.out.println("PLAYER (ignored - game over): " + msg);
            return; // no need to process.
        }

        System.out.println("PLAYER: " + msg);

        // process the message:

        if (msg.equals("QUESTION")) {       // question request.

            // select random question:
            currentQuestion = rand.nextInt(_qna.getNumberOfQuestions());

            /* make sure it wasn't already asked,
                probability of 1000 gives good results with nice
                coefficient of efficiency - 1 question can be repeated on every
                1000 questions, this also avoids the problem of selecting next
                question when there are less then 15 questions: */
            for (int i=0; (i<1000) &&
                  (askedQuestions.indexOf("[" + currentQuestion + "]") != -1);
                    i++) {
                currentQuestion = rand.nextInt(_qna.getNumberOfQuestions());
            }

            // store asked question:
            askedQuestions = askedQuestions + "[" + currentQuestion + "]";

            sendQuestionToPlayer();
        }
        else if (msg.startsWith("FINAL:")) {    // answer for a question.
            if (extractData(msg).equals(_qna.getAnswer(currentQuestion, QnA.CORRECT))) {
                // mark right question by green:
                sendToPlayer("GREEN:" + _qna.getAnswer(currentQuestion, QnA.CORRECT));

                if (moneyIndex < 14) {      // will be under 1000000.
                    moneyIndex++;
                    sendState();
                    sendToPlayer("RIGHT!"); // correct!
                }
                else if (moneyIndex == 14) { // will be 1000000 !!!!.
                    moneyIndex++;
                    sendState();
                    sendToPlayer("MILLION!");   // won a million!
                }
            }
            else {  // not correct:
                if (moneyIndex >= 10)        // won 32000.
                    moneyIndex = 10;
                else if (moneyIndex >= 5)   // won 1000.
                    moneyIndex = 5;
                else
                    moneyIndex = 0;        // won nothing.

                // mark right question by green:
                sendToPlayer("GREEN:" + _qna.getAnswer(currentQuestion, QnA.CORRECT));

                sendState();   // update MONEY & WHEELS.

                sendToPlayer("WRONG!:" + _qna.getAnswer(currentQuestion, QnA.CORRECT));
            }
        }
        else if (msg.equals("FRIEND")) {        // friend request.
            if (friendNumber>0) {
                friendNumber--;
                sendState();
                sendQuestionToFriend();
                // set timer to 30 seconds:
                  iTimerValue = 30;
                  sFriendResponse = "";  // set "no response".
                  thTimer = new Thread (this);
                  thTimer.start();
            }
            else {
                sendToPlayer("NOFRIEND!");      // no more such wheel.
            }

        }
        else if (msg.equals("50:50")) {   // make 50:50
            if (fifty50Number > 0) {
                if (wrong3 != 0) { // 50:50 done twice on current question already?
                    sendToPlayer("50:50TWICE!");
                }
                else {
                    fifty50Number--;
                    sendState();
                    sendFiftyFifty();
                }
            }
            else {
                sendToPlayer("NO50:50!");      // no more such wheel.
            }
        }
        else if (msg.equals("THE_PUBLIC")) {    // request for help of the public.
            if (thePublicNumber > 0) {
                thePublicNumber--;
                sendState();
                sendThePublicResponse();
            }
            else {
                sendToPlayer("NO_PUBLIC!");      // no more such wheel.
            }
        }
        else if (msg.equals("RETIRE")) {        // asked to retire.
                // mark right question by green:
                sendToPlayer("GREEN:" + _qna.getAnswer(currentQuestion, QnA.CORRECT));
                sendState();   // update MONEY & WHEELS (just in case).
                sendToPlayer("RETIRED:" + _qna.getAnswer(currentQuestion, QnA.CORRECT));
        }
        else if (msg.equals("RESTART")) {       // start & restart.
                moneyIndex = 0;   // $0.
                friendNumber = 3;
                fifty50Number = 4;
                thePublicNumber = 5;
                askedQuestions = "";   // allow to ask all questions.
                sendState();
        }
        else if (msg.equals("CLOSE")) {         // player closed the window.
                // inform friend that player has disconected:
                sendToFriend("PLAYER_DISCONECTED");
                // remove current game from Server's game list:
                _ourServer.removeGame(this);
        }
    }



    /**
    * This method is triggered by thread "thFriend" when
    * there is a message from friend.
    * Processes the message and makes an appropriate reaction to it.
    *
    * @param msg what friend has to say.
    */
    public void friendTalks(String msg) {

        if (thFriend == null) {  // game over?
            System.out.println("FRIEND (ignored - game over): " + msg);
            return;
        }

        System.out.println("FRIEND: " + msg);

        // process the message:

        if (msg.startsWith("FRIEND_ANSWER:") ||
                   msg.equals("FRIEND_DONTKNOW")) {     // friend's help for player.
            // set answer to player:
            sFriendResponse = msg;
        }
        else if (msg.equals("CLOSE")) {                 // friend closed the window.
            // inform player that friend has disconected:
            sendToPlayer("FRIEND_DISCONECTED");
            // remove current game from Server's game list:
            _ourServer.removeGame(this);
        }
    }



    /**
    * Sends message to player.
    *
    * @param msg the message to be sent.
    */
    public void sendToPlayer(String msg) {
        outPlayer.println(msg);
        // Flush the stream and check its error state:
        if (outPlayer.checkError())
            System.err.println("Cannot send -> " + msg);
        else
            System.out.println("SENT to PLAYER: " + msg);
    }



    /**
    * Sends message to friend.
    *
    * @param msg the message to be sent.
    */
    public void sendToFriend(String msg) {
        if (outFriend == null) return;      // in case friend not connected yet.

        outFriend.println(msg);
        // Flush the stream and check its error state:
        if (outFriend.checkError())
            System.err.println("Cannot send -> " + msg);
        else
            System.out.println("SENT to FRIEND: " + msg);
    }



    /**
    * Returns the information about the game.
    */
    public String getInfo() {

     if (friendConnection == null)
        return "Waiting for a friend...";
     else
        return  "Player Address: " + playerConnection.getInetAddress() + "\n" +
                "Player Port: " + playerConnection.getPort() + "\n\n" +

                "Friend Address: " + friendConnection.getInetAddress() + "\n" +
                "Friend Port: " + friendConnection.getPort() + "\n\n" +

                "Current Question: " + _qna.getQuestion(currentQuestion)+ "\n" +
                "Current Right Answer: " +
                  indexToLetter( _qna.getAnswer(currentQuestion, QnA.CORRECT) ) +

                "        Current Money: " + moneySum[moneyIndex] + "\n" +

                "Wheels -  Friend: " + friendNumber + ",  " +
                "Fifty Fifty: " + fifty50Number +  ",  " +
                "The Public: " + thePublicNumber +  "\n" +

                "Asked Questions: " + askedQuestions;
    }


    /**
    * Prepares this game for termination.
    * Closes all sockets, streams, and stops threads.
    */
    public void closeEverything() {
          try {
              // stop all game threads:
              thTimer = null;
              thPlayer = null;
              thFriend = null;

              // close sockets and streams:
              //  streams closed after sockets, otherwise causes problems.

              playerConnection.close();
              inPlayer.close();
              outPlayer.close();

              if (friendConnection != null) {  // friend may not be connected yet.
                  friendConnection.close();
                  inFriend.close();
                  outFriend.close();
              }

          } catch (Exception e) {
              System.err.println("On closeEverything() -> " + e);
          }
    }



    /**
    * Returns true when friend is not connected to game yet,
    * otherwise false.
    *
    * @return true if friend not connected, false otherwise.
    */
    public boolean isWaitingForFriend() {
        if (friendConnection == null)
            return true;                // is waiting.
        else
            return false;               // not waiting any more.
    }



    /**
    * Sends 50:50 wheel to a player,
    * by removing 50% of possible wrong answers.
    */
    private void sendFiftyFifty() {
        int correct = 1; // current correct answer.

        try {
            correct = Integer.parseInt( _qna.getAnswer(currentQuestion, QnA.CORRECT) );
        } catch (Exception e) {
            System.err.println("sendFiftyFifty -> " + e);
        }

        // already done 50:50 once on current question?
        //   random not required here, but instead of making too many "if",
        //   it's better to use a loop:
        if (wrong1 != 0) {
            wrong3 = rand.nextInt(4) + 1;  // generate random from 1 to 4.
            // generate again if it's equal to correct answer:
            while ( (wrong3 == correct) || (wrong3 == wrong2) || (wrong3 == wrong1))
                wrong3 = rand.nextInt(4) + 1;  // generate random from 1 to 4.
            sendToPlayer("CANCEL2:" + wrong3); // sends once only (so "CANCEL2:").
            return;   // stop here.
        }


        // gets here when 50:50 is done for the first time for current question:

        wrong1 = rand.nextInt(4) + 1;  // generate random from 1 to 4.
        // generate again if it's equal to correct answer:
        while (wrong1 == correct)
            wrong1 = rand.nextInt(4) + 1;  // generate random from 1 to 4.
        sendToPlayer("CANCEL1:" + wrong1);

        wrong2 = rand.nextInt(4) + 1;  // generate random from 1 to 4.
        // generate again if it's equal to correct answer, or already sent wrong answer:
        while ((wrong2 == correct) || (wrong2 == wrong1))
            wrong2 = rand.nextInt(4) + 1;  // generate random from 1 to 4.
        sendToPlayer("CANCEL2:" + wrong2);
    }



    /**
    * Resets the flags of removed possible answers by 50:50.
    * it's required to reset before asking each question.
    */
    private void resetCanceled5050() {
        wrong1 = 0;
        wrong2 = 0;
        wrong3 = 0;
    }



    /**
    * Sends public response to a player.
    */
    private void sendThePublicResponse() {
        if (wrong1 == 0)  // 50:50 was not used?
            sendThePublicResponse_4answers();
        else if (wrong3 != 0) // 50:50 was used 2 times? (single answer left).
            sendThePublicResponse_1answer();
        else    // 50:50 was used once.
            sendThePublicResponse_2answers(); // two answers left.
    }



    /**
    * Sends public response to a player.
    * Used by sendThePublicResponse() to send response when there
    * are no removed answers.
    */
    private void sendThePublicResponse_4answers() {

        int all = 100;   // 100%
        int charts[] = {0,0,0,0};  // temporary data.

        // generate random data, for first bar make probability
        // of 1:5 to get value of more then 50% (later this value
        // will be swapped with the correct answer), all other bars
        // will get the rest (also random with equal probability).

        // generate number from 29% to 89%
        //    (probability: 20:40 -> 1:2 that it will be over 50%,
        //        it's enough since average % for each column is 25%)
        charts[0] = rand.nextInt(60) + 29;
        all -= charts[0];   // subtract this value from 100%.

        // generate some random values for another 2 bars:
        for (int i=1; i<3; i++) {
            charts[i] = rand.nextInt(all);
            all -= charts[i];
        }

        // last bar gets the rest:
        charts[3] = all;


        // get the right answer:

        int correct = 1; // current correct answer.
        try {
            correct = Integer.parseInt( _qna.getAnswer(currentQuestion, QnA.CORRECT) );
            // validation:
            if ((correct > 5) || (correct < 1))
                correct = 1;
        } catch (Exception e) {
            System.err.println("sendThePublicResponse -> " + e);
        }

        // when right answer is 1 no swap is required:
        if (correct != 1)  {
            // make first bar (correct) with real correct answer:
            // swap first bar with correct bar:
            int temp = charts[correct-1];
            charts[correct-1] = charts[0];
            charts[0] = temp;
        }

        // send to player:
        sendToPlayer("BAR1:" + charts[0]);      // first bar  % - "A".
        sendToPlayer("BAR2:" + charts[1]);      // second bar % - "B".
        sendToPlayer("BAR3:" + charts[2]);      // third bar  % - "C".
        sendToPlayer("BAR4:" + charts[3]);      // fourth bar % - "D".
    }



    /**
    * Sends public response to a player.
    * Used by sendThePublicResponse() to send response when there
    * are 2 removed answers.
    */
    private void sendThePublicResponse_2answers() {

        int all = 100;   // 100%
        int charts[] = {0,0,0,0};  // temporary data.

        // get the right answer:

        int correct = 1; // current correct answer.
        try {
            correct = Integer.parseInt( _qna.getAnswer(currentQuestion, QnA.CORRECT) );
            // validation:
            if ((correct > 5) || (correct < 1))
                correct = 1;
        } catch (Exception e) {
            System.err.println("sendThePublicResponse -> " + e);
        }


        // generate number from 40% to 99%
        //    (probability: 10:50 -> 1:5 that it will be over 50%)
        charts[correct-1] = rand.nextInt(60) + 40;
        all -= charts[correct-1];   // subtract this value from 100%.

        // find second not removed answer and give it the rest:
        int i=1;                                             // just in case check
        while ( ((i == correct) || (i == wrong1) || (i == wrong2))  && (i < 4))
            i++;
        charts[i-1] = all;


        // send to player:
        sendToPlayer("BAR1:" + charts[0]);      // first bar  % - "A".
        sendToPlayer("BAR2:" + charts[1]);      // second bar % - "B".
        sendToPlayer("BAR3:" + charts[2]);      // third bar  % - "C".
        sendToPlayer("BAR4:" + charts[3]);      // fourth bar % - "D".
    }



    /**
    * Sends public response to a player.
    * Used by sendThePublicResponse() to send response when there
    * are 3 removed answers.
    */
    private void sendThePublicResponse_1answer() {
        int charts[] = {0,0,0,0};  // temporary data.

        // get the right answer:

        int correct = 1; // current correct answer.
        try {
            correct = Integer.parseInt( _qna.getAnswer(currentQuestion, QnA.CORRECT) );
            // validation:
            if ((correct > 5) || (correct < 1))
                correct = 1;
        } catch (Exception e) {
            System.err.println("sendThePublicResponse -> " + e);
        }

        charts[correct-1] = 100;    // single bar will get 100%.

        // send to player:
        sendToPlayer("BAR1:" + charts[0]);      // first bar  % - "A".
        sendToPlayer("BAR2:" + charts[1]);      // second bar % - "B".
        sendToPlayer("BAR3:" + charts[2]);      // third bar  % - "C".
        sendToPlayer("BAR4:" + charts[3]);      // fourth bar % - "D".
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
            // exception is expected when "NO ANSWER" is returned by _qna.getAnswer(),
            //  this happens when info is requested until question is generated
            //  and sent to player
            return "NO ANSWER";
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
