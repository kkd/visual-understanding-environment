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

import tufts.vue.shape.*;

import java.lang.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import tufts.vue.beans.VueBeanState;

/**
 * VueTool for creating LWNodes.  Methods for creating default new nodes based on
 * tool states, and for handling the drag-create of new nodes.
 */

public class NodeTool extends VueTool
    implements VueConstants, LWEditor
{
    private static NodeTool singleton = null;
    
    /** the contextual tool panel **/
    private static NodeToolPanel sNodeToolPanel;

    
    /** this constructed called via VueResources.properties init */
    public NodeTool()
    {
        super();
        if (singleton != null) 
            new Throwable("Warning: mulitple instances of " + this).printStackTrace();
        singleton = this;
    }

    final public Object getPropertyKey() { return LWKey.Shape; }
    
    public Object produceValue() {
        // this not actually used right now.
        return getActiveSubTool().getShapeInstance();
    }
    
    /** LWPropertyProducer impl: load the currently selected tool to the one with given shape */
    public void displayValue(Object shape) {
        // Find the sub-tool with the matching shape, then load it's button icon images
        // into the displayed selection icon
        if (shape == null)
            return;
        Enumeration e = getSubToolIDs().elements();
        while (e.hasMoreElements()) {
            String id = (String) e.nextElement();
            SubTool subtool = (SubTool) getSubTool(id);
            if (subtool.isShape((RectangularShape) shape)) {
                ((PaletteButton)mLinkedButton).setPropertiesFromItem(subtool.mLinkedButton);
                // call super.setSelectedSubTool to avoid firing the shape setters
                // as we're only LOADING the value here.
                super.setSelectedSubTool(subtool);
                break;
            }
        }
    }


    /** return the singleton instance of this class */
    public static NodeTool getTool()
    {
        if (singleton == null) {
            new Throwable("Warning: NodeTool.getTool: class not initialized by VUE").printStackTrace();
            //throw new IllegalStateException("NodeTool.getTool: class not initialized by VUE");
            new NodeTool();
        }
        return singleton;
    }
    
    private static final Object LOCK = new Object();
    // todo: promote a generic static to VueTool that handles the lock,
    // and calls subclass factory for creating tool panel
    static NodeToolPanel getNodeToolPanel()
    {
        synchronized (LOCK) {
            if (sNodeToolPanel == null) {
                sNodeToolPanel = new NodeToolPanel();
            }
        }
        return sNodeToolPanel;
    }
    
    public void setSelectedSubTool(VueTool tool) {
        super.setSelectedSubTool(tool);
        if (VUE.getSelection().size() > 0) {
            SubTool shapeTool = (SubTool) tool;
            shapeTool.getShapeSetterAction().fire(this);
        }
    }
    
    public JPanel getContextualPanel() {
        return getNodeToolPanel();
    }

    public static NodeTool.SubTool getActiveSubTool()
    {
        return (SubTool) getTool().getSelectedSubTool();
    }

    public Class getSelectionType() { return LWNode.class; }
    
    public boolean supportsSelection() { return true; }
    
    //private java.awt.geom.RectangularShape currentShape = new tufts.vue.shape.RectangularPoly2D(4);
    private java.awt.geom.RectangularShape currentShape;
    // todo: if we had a DrawContext here instead of just the graphics,
    // we could query it for zoom factor (passed in from mapViewer) so the stroke width would
    // look right.
    public void drawSelector(java.awt.Graphics2D g, java.awt.Rectangle r)
    {
        //g.setXORMode(java.awt.Color.blue);

        g.draw(r);
        currentShape = getActiveSubTool().getShape();
        currentShape.setFrame(r);
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,  java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        //g.setColor(COLOR_NODE_DEFAULT);
        //g.fill(currentShape);
        //g.setColor(COLOR_BORDER);
        //g.setStroke(STROKE_ONE); // todo: scale based on the scale in the GC affine transform
        //g.setStroke(new BasicStroke(2f * (float) g.getTransform().getScaleX())); // GC not scaled while drawing selector...
        g.setColor(COLOR_SELECTION);
        g.draw(currentShape);
        /*
        if (VueUtil.isMacPlatform()) // Mac 1.4.1 handles XOR differently than PC
            g.draw(currentShape);
        else
            g.fill(currentShape);
        */
    }
    
    public boolean handleSelectorRelease(MapMouseEvent e)
    {
    	LWNode node = createNode(VueResources.getString("newnode.html"), true);
        node.setAutoSized(false);
        node.setFrame(e.getMapSelectorBox());
        MapViewer viewer = e.getViewer();
        viewer.getFocal().addChild(node);
        VUE.getUndoManager().mark("New Node");
        VUE.getSelection().setTo(node);
        viewer.activateLabelEdit(node);
        return true;
    }
    /*
    public void handleSelectorRelease(java.awt.geom.Rectangle2D mapRect)
    {
        LWNode node = createNode();
        node.setAutoSized(false);
        node.setFrame(mapRect);
        VUE.getActiveMap().addNode(node);
        VUE.getSelection().setTo(node);
        VUE.getActiveViewer().activateLabelEdit(node);
    }
    */

    /**
     * Create a new node with the current default properties
     * @param name the name for the new node, can be null
     * @return the newly constructed node
     */
    public static LWNode createNode(String name) {
        return createNode(name, false);
    }

    
    /** @return a new default node with no label */
    public static LWNode createNode() {
        return createNode(null);
    }
    /** @return a new default node with the default new node label */
    public static LWNode createNewNode() {
        return createNode(VueResources.getString("newnode.html"));
    }
        
    
    public static LWNode initAsTextNode(LWNode node)
    {
        node.setIsTextNode(true);
        node.setAutoSized(true);
        node.setShape(new java.awt.geom.Rectangle2D.Float());
        node.setStrokeWidth(0f);
        //node.setFillColor(COLOR_TRANSPARENT);
        node.setFont(LWNode.DEFAULT_TEXT_FONT);
        
        return node;
    }

    public static LWNode buildTextNode(String text) {
        LWNode node = new LWNode();
        initAsTextNode(node);
        node.setLabel(text);
        return node;
    }

    
    
    /**
     * Create a new node with the current default properties.
     * @param name the name for the new node, can be null
     * @param useToolShape if true, shape of node is shape of node tool, otherwise, shape in contextual toolbar
     * @return the newly constructed node
     */
    public static LWNode createNode(String name, boolean useToolShape)
    {
        LWNode node = new LWNode(name, getActiveSubTool().getShapeInstance());
        /*
        VueBeanState state = getNodeToolPanel().getCurrentState();
        if (state != null) {
            if (useToolShape) {
                // clear out shape if there is one as node already had it's
                // shape set based on state of the node tool
                state.removeProperty(LWKey.Shape.name);
            }
            state.applyState(node);
        }
        node.setAutoSized(true);
        */
        return node;
    }
    
    /** For creating text nodes through the tools and on the map: will adjust text size for current zoom level */
    public static LWNode createTextNode(String text)
    {
        LWNode node = buildTextNode(text);
        /*
        VueBeanState state = TextTool.getTextToolPanel().getCreationStyle();
        if (state != null)
            state.applyState(node);
        */
        if (VUE.getActiveViewer() != null) {
            // Okay, for now this completely overrides the font size from the text toolbar...
            final Font font = node.getFont();
            final float curZoom = (float) VUE.getActiveViewer().getZoomFactor();
            final int minSize = LWNode.DEFAULT_TEXT_FONT.getSize();
            //if (curZoom * font.getSize() < minSize)
                node.setFont(font.deriveFont(minSize / curZoom));
        }
            
        return node;
    }
    
    /** @return an array of actions, with icon set, that will set the shape of selected
     * LWNodes */
    public Action[] getShapeSetterActions() {
        Action[] actions = new Action[getSubToolIDs().size()];
        Enumeration e = getSubToolIDs().elements();
        int i = 0;
        while (e.hasMoreElements()) {
            String id = (String) e.nextElement();
            NodeTool.SubTool nt = (NodeTool.SubTool) getSubTool(id);
            actions[i++] = nt.getShapeSetterAction();
        }
        return actions;
    }
    
    /** @return an array of standard supported shapes for nodes */
    public Object[] getAllShapeValues() {
        Object[] values = new Object[getSubToolIDs().size()];
        Enumeration e = getSubToolIDs().elements();
        int i = 0;
        while (e.hasMoreElements()) {
            String id = (String) e.nextElement();
            NodeTool.SubTool nt = (NodeTool.SubTool) getSubTool(id);
            values[i++] = nt.getShape();
        }
        return values;
    }

    public Shape getNamedShape(String name)
    {
        if (mSubToolMap.isEmpty())
            throw new Error("uninitialized sub-tools");
        
        for (VueTool t : mSubToolMap.values()) {
            //out("SUBTOOL " + t + " cssName=" + t.getAttribute("cssName"));
            if (name.equalsIgnoreCase(t.getAttribute("cssName"))) {
                return ((SubTool)t).getShape();
                //final SubTool shapeTool = (SubTool) t;
                //out("GOT SHAPE " + shapeTool.getShape());
                //return shapeTool.getShape();
            }
        }
        return null;
    }

    /**
     * VueTool class for each of the specifc node shapes.  Knows how to generate
     * an action for shape setting, and creates a dynamic icon based on the node shape.
     */
    public static class SubTool extends VueSimpleTool
    {
        private Class shapeClass = null;
        private RectangularShape cachedShape = null;
        private VueAction shapeSetterAction = null;
            
        public SubTool() {}

	public void setID(String pID) {
            super.setID(pID);
            //System.out.println(this + " ID set");
            //getShape(); // cache it for fast response first time
            setGeneratedIcons(new ShapeIcon(getShapeInstance()));
        }
        
        /** @return an action, with icon set, that will set the shape of selected
         * LWNodes to the current shape for this SubTool */
        public VueAction getShapeSetterAction() {
            if (shapeSetterAction == null) {
                shapeSetterAction = new Actions.LWCAction(getToolName(), new ShapeIcon(getShapeInstance())) {
                        void act(LWNode n) { n.setShape(getShapeInstance()); }
                    };
                //shapeSetterAction.putValue("property.key", LWKey.Shape);
                shapeSetterAction.putValue("property.value", getShape()); // this may be handy
                // key is from: MenuButton.ValueKey
            }
            return shapeSetterAction;
        }

        public RectangularShape getShapeInstance()
        {
            if (shapeClass == null) {
                String shapeClassName = getAttribute("shapeClass");
                //System.out.println(this + " got shapeClass " + shapeClassName);
                try {
                    this.shapeClass = getClass().getClassLoader().loadClass(shapeClassName);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            RectangularShape rectShape = null;
            try {
                rectShape = (RectangularShape) shapeClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return rectShape;
        }

        /** @return true if given shape is of same type as us */
        public boolean isShape(RectangularShape shape) {
            return shape != null && getShape().getClass().equals(shape.getClass());
        }
        
        public RectangularShape getShape()
        {
            if (cachedShape == null)
                cachedShape = getShapeInstance();
            return cachedShape;
        }

        static final int nearestEven(double d)
        {
            if (Math.floor(d) == d && d % 2 == 1) // if exact odd integer, just increment
                return (int) d+1;
            if (Math.floor(d) % 2 == 0)
                return (int) Math.floor(d);
            else
                return (int) Math.ceil(d);
        }
        static final int nearestOdd(double d)
        {
            if (Math.floor(d) == d && d % 2 == 0) // if exact even integer, just increment
                return (int) d+1;
            if (Math.floor(d) % 2 == 1)
                return (int) Math.floor(d);
            else
                return (int) Math.ceil(d);
        }
        
        //private static final Color sShapeColor = new Color(165,178,208); // Melanie's steel blue
        private static final Color sShapeColor = new Color(93,98,162); // Melanie's icon blue/purple
        private static final Color sShapeColorLight = VueUtil.factorColor(sShapeColor, 1.3);
        //private static final boolean sPaintBorder = false;
        private static GradientPaint sShapeGradient;
        private static int sWidth;
        private static int sHeight;
        
        static {

            // Select a width/height that will perfectly center within
            // the parent button icon.  If parent width is even, our
            // width should be even, if odd, we should be odd.  This
            // is independent of the 50% size of the parent button
            // we're using as a baseline (before the pixel tweak).
            // This also means if somebody goes to center us in the
            // parent (ToolIcon), that computation will always have an
            // even integer result, thus perfectly pixel aligned.
            // NOTE: if you paint a 1 pix border on the shape, when
            // anti-aliased this generally adds a total of 1 pixel to
            // the height & width.  If painting a border on the
            // dyanmic shape, you need to account for that for perfect
            // centering.
            
            if (ToolIcon.Width % 2 == 0)
                sWidth = nearestOdd(ToolIcon.Width / 2);
            else
                sWidth = nearestEven(ToolIcon.Width / 2);
            if (ToolIcon.Height % 2 == 0)
                sHeight = nearestOdd(ToolIcon.Height / 2);
            else
                sHeight = nearestEven(ToolIcon.Height / 2);

            sShapeGradient = new GradientPaint(sWidth/2,0,sShapeColorLight, sWidth/2,sHeight/2,sShapeColor,true); // horizontal dark center
            //sShapeGradient = new GradientPaint(sWidth/2,0,sShapeColor, sWidth/2,sHeight/2,sShapeColorLight,true); // horizontal light center
            //sShapeGradient = new GradientPaint(0,sHeight/2,sShapeColor.brighter(), sWidth/2,sHeight/2,sShapeColor,true); // vertical
            //sShapeGradient = new GradientPaint(0,0,sShapeColor.brighter(), sWidth/2,sHeight/2,sShapeColor,true); // diagonal
            
        }
        public static class ShapeIcon implements Icon
        {
            public int getIconWidth() { return sWidth; }
            public int getIconHeight() { return sHeight; }

            private RectangularShape mShape;
            
            public ShapeIcon(RectangularShape pShape)
            {
                mShape = pShape;
                if (mShape instanceof RoundRectangle2D) {
                    // hack to deal with arcs being too small on a tiny icon
                    ((RoundRectangle2D)mShape).setRoundRect(0, 0, sWidth,sHeight, 8,8);
                } else
                    mShape.setFrame(0,0, sWidth,sHeight);
            }
            
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x,y);
                if (sShapeGradient != null)
                    g2.setPaint(sShapeGradient);
                else
                    g2.setColor(sShapeColor);
                g2.fill(mShape);
                g2.setColor(Color.black);
                g2.setStroke(STROKE_HALF);
                g2.draw(mShape);
                g2.translate(-x,-y);
            }

            /** @return true if representing the same *class* of shape.
              *
              * Currently shape's are differentiated
              * by class only, which is why we have tufts.vue.shape.RoundRect2D with fixed arc widths as a separate
              * class.  (Additnal architecture will be needed if we want to get around this self-imposed limitation)
              */
            public boolean equals(Object o) {
                return o == this ||
                    o != null && o instanceof ShapeIcon && ((ShapeIcon)o).mShape.getClass().equals(getClass());
                // note: above will consider awt RoundRect2D's with different arcs as same shape... use tufts.vue.shape.*
                // with fixed params to avoid this.
            }

            public String toString() {
                return "ShapeIcon[" + sWidth + "x" + sHeight + " " + mShape + "]";
            }
        }

        
    }
}
