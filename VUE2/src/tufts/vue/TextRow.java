 /*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */


package tufts.vue;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextLayout;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;

public class TextRow
    implements VueConstants
{
    private String text;
    private Graphics2D g2d;
    private TextLayout row;

    private static final BasicStroke BorderStroke = new BasicStroke(0.1f);

    private Rectangle2D.Float bounds;

    float width;
    float height;
    
    public TextRow(String text, Font font, FontRenderContext frc)
    {
        this.text = text;
        this.row = new TextLayout(text, font, frc);
        this.bounds = (Rectangle2D.Float) row.getBounds();
        this.width = bounds.width;
        this.height = bounds.height;
    }

    public TextRow(String text, Graphics g)
    {
        this(text, g.getFont(), ((Graphics2D)g).getFontRenderContext());
        this.g2d = (Graphics2D) g;
    }

    public TextRow(String text, Font font)
    {
        // default FRC: anti-alias & fractional metrics
        this(text, font, new FontRenderContext(null, true, true));
    }
    
    public float getWidth() { return width; }
    public float getHeight() { return height; }

    public void draw(float xoff, float yoff)
    {
        draw(this.g2d, xoff, yoff);
    }
        
    private Rectangle2D.Float cachedBounds;

    public void draw(Graphics2D g2d, float xoff, float yoff)
    {
        // Mac & PC 1.4.1 implementations haved reversed baselines
        // and differ in how descents are factored into bounds offsets

        if (VueUtil.isMacPlatform()) {
            if (cachedBounds == null) {
                // for some reason, getting the bounds is only accurate the FIRST
                // time we ask for it on mac JVM 1.4.2_03
                cachedBounds = (Rectangle2D.Float) row.getBounds();
            }
            final Rectangle2D.Float tb = (Rectangle2D.Float) cachedBounds.clone();
            //System.out.println("TextRow[" + text + "]@"+tb);
            yoff += tb.height;
            yoff += tb.y;
            xoff += tb.x; // FYI, tb.x always appears to be zero in Mac Java 1.4.1
            row.draw(g2d, xoff, yoff);

            
            if (DEBUG.BOXES||DEBUG.LAYOUT) {
                // draw a red bounding box for testing
                tb.x += xoff;
                // tb.y seems to default at to -1, and if
                // any chars have descent below baseline, tb.y
                // even less --  e.g., "txt" yields -1, "jpg" yields -2
                // that with fractional metrics OFF.  With fractional
                // metrics on, the x/y & size values go from integer
                // aligned to making use of the floating point.
                // The mac doesn't fill in tb.x, and thus when fractional
                // metrics is on, it's a tiny bit off in on the x-axis.
                tb.y = -tb.y;
                tb.y += yoff;
                tb.y -= tb.height;
                g2d.setStroke(BorderStroke);
                g2d.setColor(Color.red);
                g2d.draw(tb);
            }
                
        } else {
            final Rectangle2D.Float tb = (Rectangle2D.Float) row.getBounds();
            // This is cleaner, thus I'm assuming the PC
            // implementation is also cleaner, and worthy of being
            // the default case.
                
            row.draw(g2d, -tb.x + xoff, -tb.y + yoff);
            //baseline = yoff + tb.height;

            if (DEBUG.BOXES||DEBUG.LAYOUT) {
                // draw a red bounding box for testing
                tb.x = xoff;
                tb.y = yoff;
                g2d.setStroke(BorderStroke);
                g2d.setColor(Color.red);
                g2d.draw(tb);
            }
        }
    }
}
