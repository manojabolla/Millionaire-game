/*
 Project            :   The Millionaire Game
 File Name          :   Chart.java

Author: Alexander Popov
Date  : Saturday, July 20, 2002
Homepage: http://www.geocities.com/emu8086/vb/
*/

import javax.swing.*;
import java.awt.*;


/**
* This class represents a component that provides the graphical
* representation of some values (a chart).
* This component does not calculate it's preferred size,
* so it should be placed into GridLayout() or something.
*/
public class Chart extends JComponent {

    // font used to draw captions:
    private Font _font = new Font("Arial", Font.BOLD, 14);

    // values for the charts (assumed that values are from 0 to 100):
    private int charts[] = {10, 20, 30, 40};

    private int iFrame;        // size of blue frame around the component.
    private int iCap;          // space for text labels above and below.


    /**
    * Default Constructor.
    */
    public Chart() {
        iFrame = 30;           // set size of blue frame around the component.
        iCap = 30;             // set space for text labels above and below.
    }


    /**
    * Pait method for this component.
    *
    * @param g current graphics context for this component.
    */
    public void paint(Graphics g) {

        // upper captions:
        String letters[] = {"A", "B", "C", "D"};

        // set font:
        g.setFont(_font);

        // make blue frame around the chart:
        g.setColor(Color.blue);
        g.fillRect(0,0, this.getWidth(),this.getHeight());

        // draw black chart background:
        g.setColor(Color.black);
        g.fillRect(iFrame, iFrame, getWidth()-iFrame*2-1, getHeight()-iFrame*2-1);

        // calculate the bar size:
        int innerChartWidth = getWidth()-iFrame*2-1;
        int barWidth = innerChartWidth / 9;      // 4 bars and 5 spaces between.
        int innerChartHeight = getHeight()-iFrame*2-1;
        int fullBarHeight = innerChartHeight - iCap*2;

        // calculate the drawing start point:
        int drawFromX = iFrame + barWidth;
        int drawFromY = iFrame + iCap;

        // draw four bars:
        for (int i=0; i<4; i++) {
            // draw labels:
            g.setColor(Color.white);
            g.drawString(letters[i], drawFromX+barWidth/2-5, drawFromY-10);
            g.drawString
                (charts[i] + "%", drawFromX+barWidth/2-10, drawFromY+fullBarHeight+20);

            // draw main bar (full bar):
            g.setColor(Color.red);
            g.fillRect(drawFromX, drawFromY, barWidth, fullBarHeight);

            // draw inverted bar (to actually show required data):
            g.setColor(Color.green);
            float invertedPercent = 100 - charts[i];
            float pixelsInOnePercent = ((float)fullBarHeight) / 100;
            int barHeight = (int) (pixelsInOnePercent * invertedPercent);
            //System.out.println("fullBarHeight: " + fullBarHeight);
            //System.out.println("pixelsInOnePercent: " + pixelsInOnePercent);
            //System.out.println("barHeight: " + barHeight);
            g.fillRect(drawFromX, drawFromY, barWidth, barHeight);

            // move for next bar:
            drawFromX += barWidth*2;
        }
    } // end of paint().


    /**
    * Sets values for all bars in once.
    * In case values are not between 0 and 100 fixes it
    * by setting to minimum or maximum.
    *
    * @param i1 value for the first bar.
    * @param i2 value for the second bar.
    * @param i3 value for the third bar.
    * @param i4 value for the fourth bar.
    */
    public void setChart(int i1, int i2, int i3, int i4) {
        charts[0] = i1;
        charts[1] = i2;
        charts[2] = i3;
        charts[3] = i4;

        // validate and fix - values should be between 0% and 100% :
        for (int i=0; i<4; i++) {
            if (charts[i] < 0)
                charts[i] = 0;
            else if (charts[i] > 100)
                charts[i] = 100;
        }

        repaint();
    }



    /**
    * Sets value for specified bars.
    * Receives value in String format, so does the parsing,
    * in case of error prints out to error stream.
    * In case value is not between 0 and 100 fixes it
    * by setting to minimum or maximum.
    *
    * @param c value for the bar in String format.
    * @param index number of bar should be from 1 to 4.
    */
    public void setChart(String c, int index) {
        try {
            charts[index-1] = Integer.parseInt(c);

            // validate and fix - value should be between 0% and 100% :
            if (charts[index-1] < 0)
                charts[index-1] = 0;
            else if (charts[index-1] > 100)
                charts[index-1] = 100;
        }
        catch (Exception e) {
            System.err.println("setChart -> " + e);
        }

        repaint();
    }


} // end of class.