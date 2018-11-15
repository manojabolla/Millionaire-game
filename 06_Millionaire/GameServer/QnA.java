/*
 Project            :   The Millionaire Game
 File Name          :   QnA.java

Author: Alexander Popov
Date  : Saturday, July 20, 2002
Homepage: http://www.geocities.com/emu8086/vb/
*/

import java.util.*;
import java.io.*;

/**
* This class represents Questions & Answers for all games.
*    Based on Exercise #3!
* It loads questions their possible and right answers.
* The file name is given as a parameter for constructor.
*
* File format (by line numbers) - each q&a takes 6 lines:
*           1.        Question.
*           2.        First possible answer (1 or "A").
*           3.        Second possible answer (2 or "B").
*           4.        Third possible answer (3 or "C").
*           5.        Fourth possible answer (4 or "D").
*           6.        The index for the right answer ("1..4")
*      and so on for each question.
*
*  empty lines are ignored (and not counted)!
*/
public class QnA {
    // index of correct answer when calling getAnswer() method:
    public static final int CORRECT = 5;

    private Vector _q = new Vector  ();         // contains the questions.
    private Vector _a1 = new Vector  ();        // first possible answer.
    private Vector _a2 = new Vector  ();        // second possible answer.
    private Vector _a3 = new Vector  ();        // third possible answer.
    private Vector _a4 = new Vector  ();        // fourth possible answer.
    private Vector _right = new Vector  ();     // correct answer (index).

    /**
    * Constructor. Constructs a Q&A from a file as decribed above.
    * In case of errors prints out to System.err, and exits.
    *
    * @param sourceDic the name of the file that contains data (described above).
    */
    public QnA ( String sourceDic ) {
        String s;
        /* flag to determine what is to be read next:
                0 - question
                1 - first answer
                2 - second answer
                3 - third answer
                4 - fourth answer
                5 - correct answer!
        */
        int readNow = 0; // first get the question.

        try {
            FileReader frSource = new FileReader( sourceDic );
            BufferedReader brSource = new BufferedReader( frSource );

            while( (s=brSource.readLine()) != null )    {
                s = s.trim() ;      // remove spaces from both ends of a string.
                if (s.length() != 0)            // ignore empty lines.
                    if (readNow == 0) {         // line #1  - question.
                        _q.addElement(s);
                        readNow++;
                    }
                    else if (readNow == 1) {    // line #2  - possible answer.
                        _a1.addElement(s);
                        readNow++;
                    }
                    else if (readNow == 2) {    // line #3  - possible answer.
                        _a2.addElement(s);
                        readNow++;
                    }
                    else if (readNow == 3) {    // line #4  - possible answer.
                        _a3.addElement(s);
                        readNow++;
                    }
                    else if (readNow == 4) {    // line #5  - possible answer.
                        _a4.addElement(s);
                        readNow++;
                    }
                    else if (readNow == 5) {    // line #6  - CORRECT answer.
                        _right.addElement(s);
                        readNow=0;  // set to question.
                    }
            }

            // close streams:
            brSource.close();
            frSource.close();


            // after reading last question, flag should be set back to zero,
            //   otherwise the question file is corrupted:
            if ( readNow != 0 ) {
                System.err.println("QnA file: " + sourceDic +
                        ", last question is incomplete!");
                System.exit(1);
            }
        }
        catch ( Exception e ) {
            System.err.println ( "Error loading questions: " + e );
            System.exit(1);
        }

        // insure that we have at least one question:
        if (getNumberOfQuestions() < 1) {
            System.err.println ( "No questions loaded!" );
            System.exit(1);
        }
    }

    /**
    * Returns the number of questions in Q&A.
    *
    * @return the number of questions.
    */
    public int getNumberOfQuestions() {
        return _q.size();   // all vectors have the same size.
    }

    /**
    * Returns the question.
    *
    * @param index number of question to return.
    * @return the question with this index.
    */
    public String getQuestion(int index) {
        try {
            return (String) _q.get(index);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            return "NO QUESTION";
        }
    }

    /*
        5 - is for right answer!
    */
    /**
    * Returns the possible answer (qNumber = 1 .. 4),
    *       or correct answer (qNumber = 5) - can use final CORRECT.
    *
    * @param index number of question to return.
    * @param qNumber number of answer to return (1 to 4 for possible, 5 for correct).
    * @return the possible or correct answer.
    */
    public String getAnswer(int index, int qNumber) {
        try {
            switch (qNumber) {
                case 1:
                    return (String) _a1.get(index);     // first possible answer.
                case 2:
                    return (String) _a2.get(index);     // second possible answer.
                case 3:
                    return (String) _a3.get(index);     // third possible answer.
                case 4:
                    return (String) _a4.get(index);     // fouth possible answer.
                case 5:
                    return (String) _right.get(index);  // correct answer.
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {}
        // gets here if exception, or qNumber <1 or >5 :
        return "NO ANSWER";
    }

}   // end of class.
