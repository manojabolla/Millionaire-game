/*
 Project            :   The Millionaire Game
 File Name          :   MoneyBox.java

Author: Alexander Popov
Date  : Saturday, July 20, 2002
Homepage: http://www.geocities.com/emu8086/vb/
*/

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;       // for Rectangle2D.


/**
* This class represents a label with some extended properties
*/
public class MoneyBox extends JComponent {

    // font used to draw text:
    private Font _font = new Font("Arial", Font.BOLD, 12); // font to use.

    private Color _dColor; // current back color of a RoundRect.
    private Color _fColor; // current color of a frame of RoundRect.
    private Color _tColor; // current text color.

    // used to update the size of the component:
    private JPanel _parentPanel;
    // keep current size of component to prevent
    //    unnecessary UI updates:
    int cur_w = 0;      // width.
    int cur_h = 0;      // height.

    // sizes of borders around the text:
    int _borW = 30;     // width.
    int _borH = 5;      // height.

    String _text;       // text to display.

    // true when this money box is "1,000" or "32,000",
    //        it is drawn with another color and font:
    private boolean _isStopStation = false;


    /**
    * Constructor.
    *
    * @param txt text that is shown in the middle of the component.
    * @param parentPanel parent panel, used to update size.
    */
    public MoneyBox(String txt, JPanel parentPanel) {
        _parentPanel = parentPanel;     // set parent panel.
        _text = txt;                    // set text.
        _dColor = Color.black;          // set back color of a RoundRect.
        _fColor = Color.white;          // set color of a frame of RoundRect.
        _tColor = _fColor;              // set text color (the same with frame).
    }


    /**
    * Pait method for this component.
    *
    * @param g current graphics context for this component.
    */
    public void paint(Graphics g) {

        // set font:
        g.setFont(_font);

        // calculate the prefered size for this component
        //      (to fit into the layout manager):
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D r2d = fm.getStringBounds(_text,g);
        int text_w = (int)r2d.getWidth();
        int text_h = (int)r2d.getHeight();

        // calculate the prefered size of the component:
        int w, h;
        w = text_w + _borW;
        h = text_h + _borH;

        // update only when size changes:
        if ((w != cur_w) || (h != cur_h)) {
            setPreferredSize(new Dimension(w,h));
            _parentPanel.updateUI();    // required, to force redimention.
            cur_w = w;
            cur_h = h;
            //debug// System.out.println(w + " === " + h);
        }


        // draw default blue background:
        g.setColor(Color.blue);
        g.fillRect(0,0, this.getWidth(),this.getHeight());

        // draw RoundRect frame:
        g.setColor(_fColor);
        // get coordinates according to current component size:
        g.fillRoundRect(5,0, getWidth()-5*2, getHeight()-1, 7, 7);

        // draw inner RoundRect:
        //    (the number of pixel we add will form a border)
        g.setColor(_dColor);
        g.fillRoundRect(6,1, getWidth()-6*2, getHeight()-3, 7, 7);

        // draw text in the middle of the component:
        g.setColor(_tColor);
        g.drawString(_text,
            this.getWidth()/2-text_w/2, (this.getHeight()-1)/2+fm.getAscent()/2);
    }


    /**
    * Sets this money box to be selected by changing its colors.
    *
    * @param b true to select, false to deselect.
    */
    public void setSelected(boolean b) {
        if (b) {
            _dColor = Color.orange;     // set back color of a RoundRect.

            if (_isStopStation)         // is stop station?
                _fColor = Color.red;    // set color of a frame of RoundRect.
            else
                _fColor = Color.black;  // set color of a frame of RoundRect.

            _tColor = _fColor;          // set the same color for text as for frame.
        }
        else {
            _dColor = Color.black;      // set back color of a RoundRect.

            if (_isStopStation)         // is stop station?
                _fColor = Color.yellow; // set color of a frame of RoundRect.
            else
                _fColor = Color.white;  // set color of a frame of RoundRect.

            _tColor = _fColor;          // set the same color for text as for frame.
        }

        repaint();
    }


    /**
    * Sets this money box to be a stop station,
    * (generally "1,000" and "32,000").
    * Method to set it back not implemented because
    * it's not useful for the game.
    */
    public void setStopStation() {
        _isStopStation = true;
        _font = new Font("Arial", Font.BOLD, 15);
        setSelected(false); // does the repaint().
    }


} // end of class.