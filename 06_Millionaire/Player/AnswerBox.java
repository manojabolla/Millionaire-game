/*
 Project            :   The Millionaire Game
 File Name          :   AnswerBox.java

Author: Alexander Popov
Date  : Saturday, July 20, 2002
Homepage: http://www.geocities.com/emu8086/vb/
*/

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;       // for Rectangle2D.


/**
* This class represents a JCheckBox with some extended properties
*/
public class AnswerBox extends JCheckBox {

    // font used to draw text & caption:
    private Font _fontText = new Font("Arial", Font.BOLD, 12);

    private Color _dColor; // current back color of a 6-point diamond.
    private Color _fColor; // current color of a frame of 6-point diamond.
    private Color _tColor; // current text color.

    // used to update the size of the component:
    private JPanel _parentPanel;
    // keep current size of component to prevent
    //    unnecessary UI updates:
    int cur_w = 0;      // width.
    int cur_h = 0;      // height.

    // sizes of borders around the text:
    int _borW = 40;     // width.
    int _borH = 20;     // height.

    // the caption (the text that is shown in the left corner):
    String _caption;


    /**
    * Constructor.
    *
    * @param caption text that is shown in the left corner.
    * @param parentPanel parent panel, used to update size.
    */
    public AnswerBox(String caption, JPanel parentPanel) {
        _parentPanel = parentPanel; // set parent panel for UI update.
        setText(" ");               // by default text is nothing (will be answer).
        _caption = caption;         // caption will be "A", "B", "C" or "D".
        _dColor = Color.black;      // set back color of a 6-point diamond.
        _fColor = Color.white;      // set color of a frame of 6-point diamond.
        _tColor = Color.white;      // set text color.
    }


    /**
    * Pait method for this component.
    *
    * @param g current graphics context for this component.
    */
    public void paint(Graphics g) {

        // set font:
        g.setFont(_fontText);

        // calculate the prefered size for this component
        //      (to fit into the layout manager)
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D r2d = fm.getStringBounds(getText(),g);
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


        // draw 6-point diamond frame:
        g.setColor(_fColor);
        // get coordinates according to current component size:
        int x[] =
        {10           , 0            , 10, getWidth()-10, getWidth()-1 , getWidth()-10};
        int y[] =
        {getHeight()-1, getHeight()/2, 0 , 0            , getHeight()/2, getHeight()-1};
        Polygon polf = new Polygon(x, y, x.length);
        g.fillPolygon(polf);


        // draw 6-point inner diamond:
        //    (move inside from coordinates of previous polygon)
        //    the number of added pixels will form a border:
        g.setColor(_dColor);
        x[0]+=2;
        y[0]-=2;

        x[1]+=3;    // left middle point.

        x[2]+=2;
        y[2]+=2;

        x[3]-=2;
        y[3]+=2;

        x[4]-=3;    // right middle point.

        x[5]-=2;
        y[5]-=2;

        Polygon poli = new Polygon(x, y, x.length);
        g.fillPolygon(poli);


        // the same color is used for text & caption:
        g.setColor(_tColor);

        // draw caption in in the left corner of the component:
        g.drawString(_caption,
            18, (this.getHeight()-1)/2+fm.getAscent()/2);

        // draw text in the middle of the component:
        g.drawString(getText(),
            this.getWidth()/2-text_w/2, (this.getHeight()-1)/2+fm.getAscent()/2);
    }


    /**
    * Overrides the setSelected() method of JCheckBox,
    * does the same and also sets the colors.
    *
    * @param b true to select, false to deselect.
    */
    public void setSelected(boolean b) {
        if (b) {
            _dColor = Color.orange;     // back color of a 6-point diamond.
            _fColor = Color.black;      // color of a frame of 6-point diamond.
            _tColor = Color.black;      // text color.
        }
        else {
            _dColor = Color.black;      // back color of a 6-point diamond.
            _fColor = Color.white;      // color of a frame of 6-point diamond.
            _tColor = Color.white;      // text color.
        }

        super.setSelected(b);  // makes repaint().
    }



    /**
    * Colors this checkbox in green,
    * by making it to be shown as a correct answer.
    */
    public void setCorrect() {
        _dColor = Color.green;      // back color of a 6-point diamond.
        _fColor = Color.white;      // color of a frame of 6-point diamond.
        _tColor = Color.black;      // text color.
        repaint();
    }


} // end of class.