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

import tufts.Util;

import java.awt.Color;


/**
 * Special map portal.
 *
 * We need this for now to subclass LWNode just to support the shape property.
 * If we were to move the shape key into LWComponent, we could do away with
 * this class, and just use an LWComponent with dynamically disabled properies
 * as we see fit...
 *
 * @version $Revision: 1.3 $ / $Date: 2007-05-11 21:37:11 $ / $Author: sfraize $ 
 */

public class LWPortal extends LWNode
{
    private static final Color DarkFill = new Color(0,0,0,64);
    private static final Color LightFill = new Color(255,255,255,64);
    
    public LWPortal() {
        disablePropertyTypes(KeyType.STYLE);
        enableProperty(LWKey.Shape);
        //disableProperty(LWKey.Label);
    }

    public static LWPortal create() {
        final LWPortal p = new LWPortal();
        p.setStrokeWidth(0);
        p.setSize(LWSlide.SlideWidth / 4, LWSlide.SlideHeight / 4);
        //setAspect(LWSlide.SlideAspect);
        p.setLabel("Presentation Portal");
        return p;
    }


    /* override to do nothing so we aren't constrainted by LWNode's minimum size*/
    //@Override protected void layoutImpl(Object triggerKey) {}
    //@Override protected void layout(Object triggerKey, Size curSize, Size request) {} // overkill: shrinks to nothing?
    /** override to so we aren't constrainted by LWNode's minimum size */
    @Override protected Size getTextSize() { return Size.None; }

    @Override protected void userSetSize(float width, float height, MapMouseEvent e)
    {
        if (e.isShiftDown()) {
            // Allow constraining to slide aspect:
            Size newSize = ConstrainToAspect(LWSlide.SlideAspect, width, height);
            super.setSize(newSize.width, newSize.height);
        } else {
            super.setSize(width, height);
        }
    }

    

    @Override public boolean supportsUserLabel() {
        return false;
    }
    
    @Override protected void drawImpl(DrawContext dc) {

        if (dc.focal != this && dc.isInteractive()) {
            dc.g.setColor(getRenderFillColor());
            dc.g.fill(getShape());
        } else {
            if (false) {
                dc.g.setColor(Color.gray);
                dc.g.setStroke(VueConstants.STROKE_TWO);
                dc.g.draw(getShape());
            }
        }
    }

    private boolean wasVisible = true;
    @Override public boolean isVisible() {
        // TODO: handle this in LWPathway and actually set a hidden bit...
        boolean visible;
        final java.util.Collection pathways = getPathways();
        if (pathways.size() > 0) {
            visible = false;
            for (LWPathway p : getPathways())
                if (p.isVisible())
                    visible = true;
        } else
            visible = true;

        if (wasVisible != visible) {
            wasVisible = visible;
            notify(LWKey.Hidden);
        }
        return visible;
    }
    

    @Override public boolean hasLabel() { // so LWNode won't draw it
        return false;
    }
    @Override public Color getRenderFillColor() {
        return getMap().mFillColor.brightness() > 0.5 ? DarkFill : LightFill;
        //return Color.black.equals(getMap().getFillColor()) ? LightFill : DarkFill;
    }

    @Override public boolean supportsChildren() {
        return false;
    }
    @Override public boolean supportsReparenting() {
        return false;
    }

    @Override protected boolean iconShowing() {
        return false;
    }

}
    
