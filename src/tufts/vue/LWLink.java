package tufts.vue;

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;

import javax.swing.JTextArea;

/**
 * LWLink.java
 *
 * Draws a view of a Link on a java.awt.Graphics2D context,
 * and offers code for user interaction.
 *
 * Note that links have position (always their mid-point) only so that
 * there's a place to connect for another link and/or a place for
 * the label.  Having a size doesn't actually make much sense, tho
 * we inherit from LWComponent.
 *
 * @author Scott Fraize
 * @version 6/1/03
 */
public class LWLink extends LWComponent
    implements Link,
               LWSelection.ControlListener
{
    public final static Font DEFAULT_FONT = VueResources.getFont("link.font");
    public final static Color DEFAULT_LABEL_COLOR = java.awt.Color.darkGray;
    
    //private static final Color ContrastFillColor = new Color(255,255,255,224);
    //private static final Color ContrastFillColor = new Color(255,255,255);
    // transparency fill is actually just distracting
    
    private LWComponent ep1;
    private LWComponent ep2;
    private Line2D.Float line = new Line2D.Float();
    private QuadCurve2D.Float quadCurve = null;
    private CubicCurve2D.Float cubicCurve = null;
    private Shape curve = null;
    private float mCurveCenterX;
    private float mCurveCenterY;

    private int curveControls = 0; // 0=straight, 1=quad curved, 2=cubic curved
    
    private float centerX;
    private float centerY;
    private float startX;       // todo: either consistently use these or the values in this.line
    private float startY;
    private float endX;
    private float endY;
    private String endPoint1_ID; // used only during restore
    private String endPoint2_ID; // used only during restore
    
    private boolean ordered = false; // not doing anything with this yet
    private int endPoint1Style = 0;
    private int endPoint2Style = 0;
    
    // todo: create set of arrow types
    private final float ArrowBase = 5;
    private RectangularShape ep1Shape = new tufts.vue.shape.Triangle2D(0,0, ArrowBase,ArrowBase*1.3);
    private RectangularShape ep2Shape = new tufts.vue.shape.Triangle2D(0,0, ArrowBase,ArrowBase*1.3);

    private boolean endpointMoved = true; // has an endpoint moved since we last compute shape?

    public static final int ARROW_NONE = 0;
    public static final int ARROW_EP1 = 0x1;
    public static final int ARROW_EP2 = 0x2;
    public static final int ARROW_BOTH = ARROW_EP1+ARROW_EP2;
    
    private int mArrowState = ARROW_NONE;
    
    private transient LWIcon.Block mIconBlock =
        new LWIcon.Block(this,
                         11, 9,
                         Color.darkGray,
                         LWIcon.Block.HORIZONTAL,
                         LWIcon.Block.COORDINATES_MAP);
    
    /**
     * Used only for restore -- must be public
     */
    public LWLink() {}

    /**
     * Create a new link between two LWC's
     */
    public LWLink(LWComponent ep1, LWComponent ep2)
    {
        if (ep1 == null || ep2 == null)
            throw new IllegalArgumentException("LWLink: ep1=" + ep1 + " ep2=" + ep2);
        setDefaults(this);
        setComponent1(ep1);
        setComponent2(ep2);
        computeLinkEndpoints();
    }

    static LWLink setDefaults(LWLink l)
    {
        l.setFont(DEFAULT_FONT);
        l.setTextColor(DEFAULT_LABEL_COLOR);
        l.setStrokeWidth(1f); //todo config: default link width
        return l;
    }

    public boolean supportsUserLabel() {
        return true;
    }
    
    /*
    public void setSelected(boolean selected)
    {
        boolean wasSelected = this.selected;
        super.setSelected(selected);
        if (wasSelected != selected && isCurved) {
            //notify("requestSelectionHandle", getCtrlPoint())
            if (selected)
                VUE.ModelSelection.addSelectionControl(getCtrlPoint(), this);
            else
                VUE.ModelSelection.removeSelectionControl(this);
        }
        }*/

    public boolean handleSingleClick(MapMouseEvent e)
    {
        // returning true will disallow label-edit
        // when single clicking over an icon.
        return mIconBlock.contains(e.getMapX(), e.getMapY());
    }
    
    public boolean handleDoubleClick(MapMouseEvent e)
    {
        return mIconBlock.handleDoubleClick(e);
    }

    private void setStartPoint(Point2D p) {
        setStartPoint((float)p.getX(), (float)p.getY());
    }
    private void setStartPoint(float x, float y) {
        Object old = new Point2D.Float(startX, startY);
        startX = x;
        startY = y;
        endpointMoved = true;
        notify("link.ep1.location", new Undoable(old) { void undo() { setStartPoint((Point2D) old); }} );
    }
    private void setEndPoint(Point2D p) {
        setEndPoint((float)p.getX(), (float)p.getY());
    }
    private void setEndPoint(float x, float y) {
        Object old = new Point2D.Float(endX, endY);
        endX = x;
        endY = y;
        endpointMoved = true;
        notify("link.ep2.location", new Undoable(old) { void undo() { setEndPoint((Point2D) old); }} );
    }

    
    /** interface ControlListener handler */
    public void controlPointPressed(int index, MapMouseEvent e) { }
    
    /** interface ControlListener handler
     * One of our control points (an endpoint or curve control point).
     */
    public void controlPointMoved(int index, MapMouseEvent e)
    {
        //System.out.println("LWLink: control point " + index + " moved");
        
        if (index == 0) {
            // endpoint 1 (start)
            setComponent1(null); // disconnect from node
            setStartPoint(e.getMapPoint());
            LinkTool.setMapIndicationIfOverValidTarget(ep2, this, e);
        } else if (index == 1) {
            // endpoint 2 (end)
            setComponent2(null);  // disconnect from node
            setEndPoint(e.getMapPoint());
            LinkTool.setMapIndicationIfOverValidTarget(ep1, this, e);
        } else if (index == 2) {
            // optional control 0 for curve
            setCtrlPoint0(e.getMapPoint());
        } else if (index == 3) {
            // optional control 1 for curve
            setCtrlPoint1(e.getMapPoint());
        } else
            throw new IllegalArgumentException("LWLink ctrl point > 2");

    }

    /** interface ControlListener handler */
    public void controlPointDropped(int index, MapMouseEvent e)
    {
        LWComponent dropTarget = e.getViewer().getIndication();
        // TODO BUG: above doesn't work if everything is selected
        System.out.println("LWLink: control point " + index + " dropped on " + dropTarget);
        if (dropTarget != null) {
            if (index == 0 && ep1 == null && ep2 != dropTarget)
                setComponent1(dropTarget);
            else if (index == 1 && ep2 == null && ep1 != dropTarget)
                setComponent2(dropTarget);
            // todo: ensure paint sequence same as LinkTool.makeLink
        }
    }


    //private Point2D.Float[] controlPoints = new Point2D.Float[2];
    //public Point2D.Float[] getControlPoints()
    private LWSelection.ControlPoint[] controlPoints = new LWSelection.ControlPoint[2];
    /** interface ControlListener */
    private final Color freeEndpointColor = new Color(128,0,0);
    public LWSelection.ControlPoint[] getControlPoints()
    {
        if (endpointMoved)
            computeLinkEndpoints();
        // todo opt: don't create these new Point2D's all the time --
        // we iterate through this on every paint for each link in selection
        // todo: need to indicate a color for these so we
        // can show a connection as green and a hanging endpoint as red
        //controlPoints[0] = new Point2D.Float(startX, startY);
        //controlPoints[1] = new Point2D.Float(endX, endY);
        controlPoints[0] = new LWSelection.ControlPoint(startX, startY, COLOR_SELECTION);
        controlPoints[1] = new LWSelection.ControlPoint(endX, endY, COLOR_SELECTION);
        controlPoints[0].setColor(null); // no fill (transparent)
        controlPoints[1].setColor(null);
        if (this.ep1 == null) controlPoints[0].setColor(COLOR_SELECTION_HANDLE);
        if (this.ep2 == null) controlPoints[1].setColor(COLOR_SELECTION_HANDLE);
        if (curveControls == 1) {
            //controlPoints[2] = (Point2D.Float) quadCurve.getCtrlPt();
            controlPoints[2] = new LWSelection.ControlPoint(quadCurve.getCtrlPt(), COLOR_SELECTION_CONTROL);
        } else if (curveControls == 2) {
            //controlPoints[2] = (Point2D.Float) cubicCurve.getCtrlP1();
            //controlPoints[3] = (Point2D.Float) cubicCurve.getCtrlP2();
            controlPoints[2] = new LWSelection.ControlPoint(cubicCurve.getCtrlP1(), COLOR_SELECTION_CONTROL);
            controlPoints[3] = new LWSelection.ControlPoint(cubicCurve.getCtrlP2(), COLOR_SELECTION_CONTROL);
        }

        return controlPoints;
    }
    
    /** called by LWComponent.updateConnectedLinks to let
     * us know something we're connected to has moved,
     * and thus we need to recompute our drawn shape.
     */
    void setEndpointMoved(boolean tv)
    {
        this.endpointMoved = tv;
    }

    public boolean isCurved()
    {
        return curveControls > 0;
    }

    /** set this link to be curved or not.  defaults to a Quadradic (1 control point) curve. */
    /*
    private void X_setCurved(boolean curved)
    {
        this.isCurved = curved;
        //System.out.println(this + " SET CURVED " + curved + " cubic="+isCubicCurve);
        if (isCurved) {
            if (isCubicCurve) {
                this.curve = this.cubicCurve = new CubicCurve2D.Float();
                this.controlPoints = new LWSelection.ControlPoint[4];//new Point2D.Float[4];
                this.cubicCurve.ctrlx1 = Float.MIN_VALUE;
            } else {
                this.curve = this.quadCurve = new QuadCurve2D.Float();
                this.controlPoints = new LWSelection.ControlPoint[3];//new Point2D.Float[3];
                this.quadCurve.ctrlx = Float.MIN_VALUE;
            }
        } else {
            this.controlPoints = new LWSelection.ControlPoint[2];//new Point2D.Float[2];
            this.quadCurve = null;
            this.cubicCurve = null;
            this.curve = null;
        }
        endpointMoved = true;
    }
    */

    /** for persistance or setting CubicCurve */
    public void setControlCount(int points)
    {
        //System.out.println(this + " setting CONTROL COUNT " + points);
        if (points > 2)
            throw new IllegalArgumentException("LWLink: max 2 control points " + points);

        if (curveControls == points)
            return;

        // Note: Float.MIN_VALUE is used as a special marker
        // to say that that control point hasn't been initialized
        // yet.

        if (curveControls == 0 && points == 1) {
            this.curve = this.quadCurve = new QuadCurve2D.Float();
            this.quadCurve.ctrlx = Float.MIN_VALUE;
        }
        else if (curveControls == 0 && points == 2) {
            this.curve = this.cubicCurve = new CubicCurve2D.Float();
            this.cubicCurve.ctrlx1 = Float.MIN_VALUE;
            this.cubicCurve.ctrlx2 = Float.MIN_VALUE;
        }
        else if (curveControls == 1 && points == 2) {
            // adding one (up from quadCurve to cubicCurve)
            this.curve = this.cubicCurve = new CubicCurve2D.Float();
            this.cubicCurve.ctrlx1 = quadCurve.ctrlx;
            this.cubicCurve.ctrly1 = quadCurve.ctrly;
            this.cubicCurve.ctrlx2 = Float.MIN_VALUE;
        }
        else if (curveControls == 2 && points == 1) {
            // removing one (drop from cubicCurve to quadCurve)
            this.curve = this.quadCurve = new QuadCurve2D.Float();
            this.quadCurve.ctrlx = cubicCurve.ctrlx1;
            this.quadCurve.ctrly = cubicCurve.ctrly1;
        }
        else {
            this.quadCurve = null;
            this.cubicCurve = null;
            this.curve = null;
        }
        
        curveControls = points;
        this.controlPoints = new LWSelection.ControlPoint[2 + curveControls];

        endpointMoved = true;        
    }

    /** for persistance */
    public int getControlCount()
    {
        return curveControls;
    }

    /** for persistance */
    public Point2D.Float getPoint1()
    {
        return new Point2D.Float(startX, startY);
    }
    /** for persistance */
    public Point2D.Float getPoint2()
    {
        return new Point2D.Float(endX, endY);
    }
    /** for persistance */
    public void setPoint1(Point2D.Float p)
    {
        startX = p.x;
        startY = p.y;
    }
    /** for persistance */
    public void setPoint2(Point2D.Float p)
    {
        endX = p.x;
        endY = p.y;
    }
    
    /** for persistance */
    public Point2D getCtrlPoint0()
    {
        if (curveControls == 0)
            return null;
        else if (curveControls == 2)
            return cubicCurve.getCtrlP1();
        else
            return quadCurve.getCtrlPt();
    }
    /** for persistance */
    public Point2D getCtrlPoint1()
    {
        return (curveControls == 2) ? cubicCurve.getCtrlP2() : null;
    }
    
    /** for persistance and ControlListener */
    public void setCtrlPoint0(Point2D point) {
        setCtrlPoint0((float) point.getX(), (float) point.getY());
    }
    public void setCtrlPoint0(float x, float y)
    {
        if (curveControls == 0) {
            setControlCount(1);
            System.out.println("implied curved link by setting control point 0 " + this);
        }
        Object old;
        if (curveControls == 2) {
            old = new Point2D.Float(cubicCurve.ctrlx1, cubicCurve.ctrly1); 
            cubicCurve.ctrlx1 = x;
            cubicCurve.ctrly1 = y;
        } else {
            old = new Point2D.Float(quadCurve.ctrlx, quadCurve.ctrly);
            quadCurve.ctrlx = x;
            quadCurve.ctrly = y;
        }
        endpointMoved = true;
        notify("link.control.0", new Undoable(old) { void undo() { setCtrlPoint0((Point2D)old); }} );
    }

    /** for persistance and ControlListener */
    public void setCtrlPoint1(Point2D point) {
        setCtrlPoint1((float) point.getX(), (float) point.getY());
    }
    public void setCtrlPoint1(float x, float y)
    {
        if (curveControls < 2) {
            setControlCount(2);
            System.out.println("implied cubic curved link by setting a control point 1 " + this);
        }
        Object old = new Point2D.Float(cubicCurve.ctrlx2, cubicCurve.ctrly2); 
        cubicCurve.ctrlx2 = x;
        cubicCurve.ctrly2 = y;
        endpointMoved = true;
        notify("link.control.1", new Undoable(old) { void undo() { setCtrlPoint1((Point2D)old); }} );
    }

    protected void removeFromModel()
    {
        super.removeFromModel();
        if (ep1 != null) ep1.removeLinkRef(this);
        if (ep2 != null) ep2.removeLinkRef(this);
    }

    /** Is this link between a parent and a child? */
    public boolean isParentChildLink()
    {
        // todo fix: if parent is null this may provide incorrect results
        return (ep1 != null && ep1.getParent() == ep2) || (ep2 != null && ep2.getParent() == ep1);
    }
    
    public Shape getShape()
    {
        if (endpointMoved)
            computeLinkEndpoints();
        if (curveControls > 0)
            return this.curve;
        else
            return this.line;
    }

    public void mouseOver(MapMouseEvent e)
    {
        if (mIconBlock.isShowing())
            mIconBlock.checkAndHandleMouseOver(e);
    }
    
    private final int MaxZoom = 1; //todo: get from Zoom code
    private final float SmallestScaleableStrokeWidth = 1 / MaxZoom;
    public boolean intersects(Rectangle2D rect)
    {
        if (endpointMoved)
            computeLinkEndpoints();
        float w = getStrokeWidth();
        if (true || w <= SmallestScaleableStrokeWidth) {
            //if (isCurved) { System.err.println("curve intersects=" + rect.intersects(curve.getBounds2D())); }
            if (curve != null) {
                if (curve.intersects(rect)) // checks entire INTERIOR (concave region) of the curve
                //if (curve.getBounds2D().intersects(rect)) // todo perf: cache bounds -- why THIS not working???
                    return true;
            } else {
                if (rect.intersectsLine(this.line))
                    return true;
            }
            if (mIconBlock.intersects(rect))
                return true;
            else if (hasLabel())
                return labelBox.intersectsMapRect(rect);
            //return rect.intersects(getLabelX(), getLabelY(),
            //                         labelBox.getWidth(), labelBox.getHeight());
            else
                return false;
        } else {
            // todo: finish 
            Shape s = this.stroke.createStrokedShape(this.line); // todo: cache this
            return s.intersects(rect);
            // todo: ought to compensate for stroke shrinkage
            // due to a link to a child (or remove that feature)
            
            /*
            //private Line2D edge1 = new Line2D.Float();
            //private Line2D edge2 = new Line2D.Float();
            // probably faster to do it this way for vanilla lines,
            // tho also need to compute perpedicular segments from
            // endpoints.
            edge1.setLine(this.line);//move "left"
            if (rect.intersectsLine(edge1))
                return true;
            edge1.setLine(this.line); //move "right"
            if (rect.intersectsLine(edge2))
                return true;
            */
        }
    }

    public boolean contains(float x, float y)
    {
        if (endpointMoved)
            computeLinkEndpoints();
        if (curve != null) {
            // QuadCurve2D actually checks the entire concave region for containment
            // todo perf: would be more accurate to coursely flatten the curve
            // and check the segments using stroke width and distance
            // from each segment as we do below when link is line,
            // tho this would be much slower.  Could cache flattening
            // iterator or it's resultant segments to make faster.
            if (curve.contains(x, y))
                return true;
        } else {
            float maxDist = getStrokeWidth() / 2;
            final int slop = 2; // near miss on line still hits it
            // todo: can make slop bigger if implement a two-pass
            // hit detection process that does absolute on first pass,
            // and slop hits on second pass (otherwise, if this too big,
            // clicking in a node near a link to it that's on top of it
            // will select the link, and not the node).
            if (maxDist < slop)
                maxDist = slop;
            if (line.ptSegDistSq(x, y) <= (maxDist * maxDist) + 1)
                return true;
        }
        if (mIconBlock.contains(x, y))
            return true;
        else if (hasLabel())
            return labelBox.containsMapLocation(x, y); // bit of a hack to do this way
        else
            return false;
    }
    
    /**
     * Does x,y fall within the selection target for this component.
     * For links, we need to get within 20 pixels of the center.
     */
    public boolean targetContains(float x, float y)
    {
        if (endpointMoved)
            computeLinkEndpoints();
        float swath = getStrokeWidth() / 2 + 20; // todo: config/preference
        float sx = this.centerX - swath;
        float sy = this.centerY - swath;
        float ex = this.centerX + swath;
        float ey = this.centerY + swath;
        
        return x >= sx && x <= ex && y >= sy && y <= ey;
    }
    
    /* TODO FIX: not everybody is going to be okay with these returning null... */
    public LWComponent getComponent1() { return ep1; }
    public LWComponent getComponent2() { return ep2; }
    public MapItem getItem1() { return ep1; }
    public MapItem getItem2() { return ep2; }

    void disconnectFrom(LWComponent c)
    {
        boolean changed = false;
        if (ep1 == c)
            setComponent1(null);
        else if (ep2 == c)
            setComponent2(null);
        else
            throw new IllegalArgumentException(this + " cannot disconnect: not connected to " + c);
    }
            
    void setComponent1(LWComponent c)
    {
        if (c == ep1 || (c == null && ep1 == null))
            return;
        if (ep1 == c)
            System.err.println("*** Warning: ep1 already set to that in " + this + " " + c);
        if (ep1 != null)
            ep1.removeLinkRef(this);            
        Object old = this.ep1;
        this.ep1 = c;
        if (c != null)
            c.addLinkRef(this);
        endPoint1_ID = null;
        endpointMoved = true;
        notify("link.ep1.connect", new Undoable(old) { void undo() { setComponent1((LWComponent)old); }} );
    }
    void setComponent2(LWComponent c)
    {
        if (c == ep2 || (c == null && ep2 == null))
            return;
        if (c != null && ep2 == c)
            System.err.println("*** Warning: ep2 already set to that in " + this + " " + c);
        if (ep2 != null)
            ep2.removeLinkRef(this);            
        Object old = this.ep2;
        this.ep2 = c;
        if (c != null)
            c.addLinkRef(this);
        endPoint2_ID = null;
        endpointMoved = true;
        notify("link.ep2.connect", new Undoable(old) { void undo() { setComponent2((LWComponent)old); }} );
    }

    
    // used only during save
    public String getEndPoint1_ID()
    {
        //System.err.println("getEndPoint1_ID called for " + this);
        if (this.ep1 == null)
            return this.endPoint1_ID;
        else
            return this.ep1.getID();
    }
    // used only during save
    public String getEndPoint2_ID()
    {
        //System.err.println("getEndPoint2_ID called for " + this);
        if (this.ep2 == null)
            return this.endPoint2_ID;
        else
            return this.ep2.getID();
    }

    // used only during restore
    public void setEndPoint1_ID(String s)
    {
        this.endPoint1_ID = s;
    }
    // used only during restore
    public void setEndPoint2_ID(String s)
    {
        this.endPoint2_ID = s;
    }

    public boolean isOrdered()
    {
        return this.ordered;
    }
    public void setOrdered(boolean ordered)
    {
        this.ordered = ordered;
    }
    public int getWeight()
    {
        return (int) (getStrokeWidth() + 0.5f);
    }
    public void setWeight(int w)
    {
        setStrokeWidth((float)w);
    }
    public void setStrokeWidth(float w)
    {
        if (w <= 0f)
            w = 0.1f;
        super.setStrokeWidth(w);
    }
    public int incrementWeight()
    {
        //this.weight += 1;
        //return this.weight;
        setStrokeWidth(getStrokeWidth()+1);
        return getWeight();
    }

    public java.util.Iterator getLinkEndpointsIterator()
    {
        java.util.List endpoints = new java.util.ArrayList(2);
        if (this.ep1 != null) endpoints.add(this.ep1);
        if (this.ep2 != null) endpoints.add(this.ep2);
        return new VueUtil.GroupIterator(endpoints,
                                         super.getLinkEndpointsIterator());
        
    }
    
    public java.util.List getAllConnectedComponents()
    {
        java.util.List list = new java.util.ArrayList(getLinkRefs().size() + 2);
        list.addAll(getLinkRefs());
        list.add(getComponent1());
        list.add(getComponent2());
        return list;
    }
    
    /**
     * Any free (unattached) endpoints get translated by
     * how much we're moving, as well as any control points.
     * If both ends of this link are connected and it has
     * no control points (it's straight, not curved) calling
     * setLocation will have absolutely no effect on it.
     */

    public void setLocation(float x, float y)
    {
        float dx = x - getX();
        float dy = y - getY();

        if (ep1 == null)
            setStartPoint(startX + dx, startY + dy);

        if (ep2 == null)
            setEndPoint(endX + dx, endY + dy);

        if (curveControls == 1) {
            setCtrlPoint0(quadCurve.ctrlx + dx,
                          quadCurve.ctrly + dy);
        } else if (curveControls == 2) {
            setCtrlPoint0(cubicCurve.ctrlx1 + dx,
                          cubicCurve.ctrly1 + dy);
            setCtrlPoint1(cubicCurve.ctrlx2 + dx,
                          cubicCurve.ctrly2 + dy);
        }
    }

    
    /*
    public void X_setLocation(float x, float y)
    {
        float dx = getX() - x;
        float dy = getY() - y;
        //System.out.println(this + " ("+x+","+y+") dx="+dx+" dy="+dy);
        // fixme: moving a link tween links sends
        // multiple move events to nodes at their
        // ends, causing them to move nlinks or more times
        // faster than we're dragging.
        // todo fixme: what if both are children? better
        // perhaps to actually have a child move it's parent
        // around here, yet we can't do generally in setLocation
        // or then we couldn't individually drag a parent
        if (!ep1.isChild())
            ep1.setLocation(ep1.getX() - dx, ep1.getY() - dy);
        if (!ep2.isChild())
            ep2.setLocation(ep2.getX() - dx, ep2.getY() - dy);
        super.setLocation(x,y);
    }
    */

    /**
     * Compute the intersection point of two lines, as defined
     * by two given points for each line.
     * This already assumes that we know they intersect somewhere (are not parallel), 
     */
    private static final float[] _intersection = new float[2];
    private static float[] computeLineIntersection(float s1x1, float s1y1, float s1x2, float s1y2,
                                                    float s2x1, float s2y1, float s2x2, float s2y2)
    {
        // We are defining a line here using the formula:
        // y = mx + b  -- m is slope, b is y-intercept (where crosses x-axis)
        
        boolean m1vertical = (s1x1 == s1x2);
        boolean m2vertical = (s2x1 == s2x2);
        float m1 = Float.NaN;
        float m2 = Float.NaN;
        if (!m1vertical)
            m1 = (s1y1 - s1y2) / (s1x1 - s1x2);
        if (!m2vertical)
            m2 = (s2y1 - s2y2) / (s2x1 - s2x2);
        
        // Solve for b using any two points from each line.
        // to solve for b:
        //      y = mx + b
        //      y + -b = mx
        //      -b = mx - y
        //      b = -(mx - y)
        // float b1 = -(m1 * s1x1 - s1y1);
        // float b2 = -(m2 * s2x1 - s2y1);
        // System.out.println("m1=" + m1 + " b1=" + b1);
        // System.out.println("m2=" + m2 + " b2=" + b2);

        // if EITHER line is vertical, the x value of the intersection
        // point will obviously have to be the x value of any point
        // on the vertical line.
        
        float x = 0;
        float y = 0;
        if (m1vertical) {   // first line is vertical
            //System.out.println("setting X to first vertical at " + s1x1);
            float b2 = -(m2 * s2x1 - s2y1);
            x = s1x1; // set x to any x point from the first line
            // using y=mx+b, compute y using second line
            y = m2 * x + b2;
        } else {
            float b1 = -(m1 * s1x1 - s1y1);
            if (m2vertical) { // second line is vertical (has no slope)
                //System.out.println("setting X to second vertical at " + s2x1);
                x = s2x1; // set x to any point from the second line
            } else {
                // second line has a slope (is not veritcal: m is valid)
                float b2 = -(m2 * s2x1 - s2y1);
                x = (b2 - b1) / (m1 - m2);
            }
            // using y=mx+b, compute y using first line
            y = m1 * x + b1;
        }
        //System.out.println("x=" + x + " y=" + y);

        _intersection[0] = x;
        _intersection[1] = y;
        return _intersection;
        //return new float[] { x, y };
    }

    // this for debug
    private static final String[] SegTypes = { "MOVEto", "LINEto", "QUADto", "CUBICto", "CLOSE" };
    
    /*
     * Compute the intersection of an arbitrary shape and a line.
     * If no intersection, returns Float.NaN values for x/y.
     */
    private static final float[] NoIntersection = { Float.NaN, Float.NaN };
    private static float[] computeShapeIntersection(Shape shape,
                                                    float rayX1, float rayY1,
                                                    float rayX2, float rayY2)
    {
        PathIterator i = shape.getPathIterator(null);
        // todo performance: if this shape has no curves (CUBICTO or QUADTO)
        // this flattener is redundant.  Also, it would be faster to
        // actually do the math for arcs and compute the intersection
        // of the arc and the line, tho we can save that for another day.
        i = new java.awt.geom.FlatteningPathIterator(i, 0.5);
        
        float[] seg = new float[6];
        float firstX = 0f;
        float firstY = 0f;
        float lastX = 0f;
        float lastY = 0f;
        int cnt = 0;
        while (!i.isDone()) {
            int segType = i.currentSegment(seg);
            if (cnt == 0) {
                firstX = seg[0];
                firstY = seg[1];
            } else if (segType == PathIterator.SEG_CLOSE) {
                seg[0] = firstX; 
                seg[1] = firstY; 
            }
            float endX, endY;
            //if (segType == PathIterator.SEG_CUBICTO) {
            //    endX = seg[4];
            //    endY = seg[5];
            //} else {
                endX = seg[0];
                endY = seg[1];
            //}
            if (cnt > 0 && Line2D.linesIntersect(rayX1, rayY1, rayX2, rayY2, lastX, lastY, seg[0], seg[1])) {
                //System.out.println("intersection at segment #" + cnt + " " + SegTypes[segType]);
                return computeLineIntersection(rayX1, rayY1, rayX2, rayY2, lastX, lastY, seg[0], seg[1]);
            }
            cnt++;
            lastX = endX;
            lastY = endY;
            i.next();
        }
        return NoIntersection;
    }

        /*
          // a different way of computing link connection
          // points that minimizes over-stroke of
          // our parent (if we have one)
          
        if (ep1.isChild()) {
            //Point2D p = ep1.nearestPoint(endX, endY);
            //startX = (float) p.getX();
            //startY = (float) p.getY();
            // nearest corner
            if (endX > startX)
                startX += ep1.getWidth() / 2;
            else if (endX < startX)
                startX -= ep1.getWidth() / 2;
            if (endY > startY)
                startY += ep1.getHeight() / 2;
            else if (endY < startY)
                startY -= ep1.getHeight() / 2;
        }
        if (ep2.isChild()) {
            //Point2D p = ep2.nearestPoint(startX, startY);
            //endX = (float) p.getX();
            //endY = (float) p.getY();
            // nearest corner
            if (endX > startX)
                endX -= ep2.getWidth() / 2;
            else if (endX < startX)
                endX += ep2.getWidth() / 2;
            if (endY > startY)
                endY -= ep2.getHeight() / 2;
            else if (endY < startY)
                endY += ep2.getHeight() / 2;
        }
        */
    

    /**
     * Compute the endpoints of this link based on the edges
     * of the shapes we're connecting.  To do this we draw
     * a line from the center of one shape to the center of
     * the other, and set the link endpoints to the places where
     * this line crosses the edge of each shape.  If one of
     * the shapes is a straight line, or for some reason
     * a shape doesn't have a facing "edge", or if anything
     * unpredicatable happens, we just leave the connection
     * point as the center of the object.
     */
    void computeLinkEndpoints()
    {
        //if (ep1 == null || ep2 == null) throw new IllegalStateException("LWLink: attempting to compute shape w/out endpoints");
        // we clear this at the top in case another thread
        // (e.g., AWT paint) clears it again while we're
        // in here
        endpointMoved = false;

        if (ep1 != null) {
            startX = ep1.getCenterX();
            startY = ep1.getCenterY();
        }
        if (ep2 != null) {
            endX = ep2.getCenterX();
            endY = ep2.getCenterY();
        }

        
        // TODO: sort out setting cubic control points when
        // we're in here the first time and we haven't even
        // computed the real intersected endpoints yet.
        // (same applies to quadcurves but seems to be working better)

        if (curveControls > 0) {
            //-------------------------------------------------------
            // INTIALIZE CONTROL POINTS
            //-------------------------------------------------------
            this.centerX = startX - (startX - endX) / 2;
            this.centerY = startY - (startY - endY) / 2;

            if (curveControls == 2) {
                    /*
                    // disperse the 2 control points -- todo: get working
                    float offX = Math.abs(startX - centerX) * 0.66f;
                    float offY = Math.abs(startY - centerY) * 0.66f;
                    cubicCurve.ctrlx1 = startX + offX;
                    cubicCurve.ctrly1 = startY + offY;
                    cubicCurve.ctrlx2 = endX - offX;
                    cubicCurve.ctrly2 = endY - offY;
                    */
                if (cubicCurve.ctrlx1 == Float.MIN_VALUE) {
                    cubicCurve.ctrlx1 = centerX;
                    cubicCurve.ctrly1 = centerY;
                }
                if (cubicCurve.ctrlx2 == Float.MIN_VALUE) {
                    cubicCurve.ctrlx2 = centerX;
                    cubicCurve.ctrly2 = centerY;
                }
            } else {
                if (quadCurve.ctrlx == Float.MIN_VALUE) {
                    // unintialized control points
                    quadCurve.ctrlx = centerX;
                    quadCurve.ctrly = centerY;
                }
            }
        }
        

        float srcX, srcY;
        Shape ep1Shape = ep1 == null ? null : ep1.getShape();
        // if either endpoint shape is a straight line, we don't need to
        // bother computing the shape intersection -- it will just
        // be the default connection point -- the center point.
        
        // todo bug: if it's a CURVED LINK we're connect to, a floating
        // connection point works out if the link approaches from
        // the convex side, but from the concave side, it winds
        // up at the center point for a regular straight link.
        
        if (ep1Shape != null && !(ep1Shape instanceof Line2D)) {
            if (curveControls == 1) {
                srcX = quadCurve.ctrlx;
                srcY = quadCurve.ctrly;
            } else if (curveControls == 2) {
                srcX = cubicCurve.ctrlx1;
                srcY = cubicCurve.ctrly1;
            } else {
                srcX = endX;
                srcY = endY;
            }
            float[]intersection = computeShapeIntersection(ep1Shape, startX, startY, srcX, srcY);
            // If intersection fails for any reason, leave endpoint as center
            // of object.
            if (!Float.isNaN(intersection[0])) startX = intersection[0];
            if (!Float.isNaN(intersection[1])) startY = intersection[1];
        }
        Shape ep2Shape = ep2 == null ? null : ep2.getShape();
        if (ep2Shape != null && !(ep2Shape instanceof Line2D)) {
            if (curveControls == 1) {
                srcX = quadCurve.ctrlx;
                srcY = quadCurve.ctrly;
            } else if (curveControls == 2) {
                srcX = cubicCurve.ctrlx2;
                srcY = cubicCurve.ctrly2;
            } else {
                srcX = startX;
                srcY = startY;
            }
            float[]intersection = computeShapeIntersection(ep2Shape, srcX, srcY, endX, endY);
            // If intersection fails for any reason, leave endpoint as center
            // of object.
            if (!Float.isNaN(intersection[0])) endX = intersection[0];
            if (!Float.isNaN(intersection[1])) endY = intersection[1];
        }
        
        this.centerX = startX - (startX - endX) / 2;
        this.centerY = startY - (startY - endY) / 2;
        
        // We only set the size & location here so LWComponent.getBounds
        // can do something reasonable with us for computing/drawing
        // a selection box, and for LWMap.getBounds in computing entire
        // area need to display everything on the map (so we need
        // to include control point so a curve swinging out at the
        // edge is sure to be included in visible area).

        if (curveControls > 0) {
            //-------------------------------------------------------
            // INTIALIZE CONTROL POINTS
            //-------------------------------------------------------
            /*
            if (isCubicCurve) {
                if (false&&cubicCurve.ctrlx1 == Float.MIN_VALUE) {
                    // unintialized control points
                    float offX = Math.abs(startX - centerX) * 0.66f;
                    float offY = Math.abs(startY - centerY) * 0.66f;
                    cubicCurve.ctrlx1 = startX + offX;
                    cubicCurve.ctrly1 = startY + offY;
                    cubicCurve.ctrlx2 = endX - offX;
                    cubicCurve.ctrly2 = endY - offY;
                }
            } else {
                if (false&&quadCurve.ctrlx == Float.MIN_VALUE) {
                    // unintialized control points
                    quadCurve.ctrlx = centerX;
                    quadCurve.ctrly = centerY;
                }
            }
            */

            Rectangle2D.Float bounds = new Rectangle2D.Float();
            bounds.width = Math.abs(startX - endX);
            bounds.height = Math.abs(startY - endY);
            bounds.x = centerX - bounds.width/2;
            bounds.y = centerY - bounds.height/2;
            if (curveControls == 2) {
                bounds.add(cubicCurve.ctrlx1, cubicCurve.ctrly1);
                bounds.add(cubicCurve.ctrlx2, cubicCurve.ctrly2);
            } else {
                bounds.add(quadCurve.ctrlx, quadCurve.ctrly);
            }
            setEventsEnabled(false);
            try {
                // todo check: any problem with events off here?
                setSize(bounds.width, bounds.height);
                setX(bounds.x);
                setY(bounds.y);
            } finally {
                setEventsEnabled(true);
            }

        } else {
            setEventsEnabled(false);
            try {
                // todo check: any problem with events off here?
                setSize(Math.abs(startX - endX), Math.abs(startY - endY));
                setX(this.centerX - getWidth()/2);
                setY(this.centerY - getHeight()/2);
            } finally {
                setEventsEnabled(true);
            }
        }

        //-------------------------------------------------------
        // Set the stroke line
        //-------------------------------------------------------
        this.line.setLine(startX, startY, endX, endY);
        if (curveControls == 1) {
            quadCurve.x1 = startX;
            quadCurve.y1 = startY;
            quadCurve.x2 = endX;
            quadCurve.y2 = endY;

            // compute approximate on-curve "center" for label

            // We compute a line from the center of control line 1 to
            // the center of control line 2: that line segment is a
            // tangent to the curve who's center is on the curve.
            // (See QuadCurve2D.subdivide)
            
            float ctrlx1 = (quadCurve.x1 + quadCurve.ctrlx) / 2;
            float ctrly1 = (quadCurve.y1 + quadCurve.ctrly) / 2;
            float ctrlx2 = (quadCurve.x2 + quadCurve.ctrlx) / 2;
            float ctrly2 = (quadCurve.y2 + quadCurve.ctrly) / 2;
            mCurveCenterX = (ctrlx1 + ctrlx2) / 2;
            mCurveCenterY = (ctrly1 + ctrly2) / 2;
            
        } else if (curveControls == 2) {
            cubicCurve.x1 = startX;
            cubicCurve.y1 = startY;
            cubicCurve.x2 = endX;
            cubicCurve.y2 = endY;

            // compute approximate on-curve "center" for label
            // (See CubicCurve2D.subdivide)
            float centerx = (cubicCurve.ctrlx1 + cubicCurve.ctrlx2) / 2;
            float centery = (cubicCurve.ctrly1 + cubicCurve.ctrly2) / 2;
            float ctrlx1 = (cubicCurve.x1 + cubicCurve.ctrlx1) / 2;
            float ctrly1 = (cubicCurve.y1 + cubicCurve.ctrly1) / 2;
            float ctrlx2 = (cubicCurve.x2 + cubicCurve.ctrlx2) / 2;
            float ctrly2 = (cubicCurve.y2 + cubicCurve.ctrly2) / 2;
            float ctrlx12 = (ctrlx1 + centerx) / 2;
            float ctrly12 = (ctrly1 + centery) / 2;
            float ctrlx21 = (ctrlx2 + centerx) / 2;
            float ctrly21 = (ctrly2 + centery) / 2;
            mCurveCenterX = (ctrlx12 + ctrlx21) / 2;
            mCurveCenterY = (ctrly12 + ctrly21) / 2;
        }
        
        layout();
        // if there are any links connected to this link, make sure they
        // know that this endpoint has moved.
        updateConnectedLinks();
        
    }

    /**
     * Compute the angle of rotation of the line defined by the two given points
     */
    private double computeAngle(double x1, double y1, double x2, double y2)
    {
        double xdiff = x1 - x2;
        double ydiff = y1 - y2;
        double slope = xdiff / ydiff;
        double slopeInv = 1 / slope;
        double r0 = -Math.atan(slope);
        double deg = Math.toDegrees(r0);
        if (xdiff >= 0 && ydiff >= 0)
            deg += 180;
        else if (xdiff <= 0 && ydiff >= 0)
            deg = -90 - (90-deg);

        if (VueUtil.isMacPlatform()) {
            // Mac MRJ 69.1 / Java 1.4.1 java bug: approaching 45/-45 & 225/-135 degrees,
            // rotations seriously fuck up (most shapes are translated to infinity and
            // back, except at EXACTLY 45 degrees, where it works fine).
            final int ew = 10; // error-window: # of degrees around 45 that do broken rotations
            if (deg > 45-ew && deg < 45+ew)
                deg = 45;
            if (deg > -135-ew && deg < -135+ew)
                deg = -135;
        }
        return  Math.toRadians(deg);


        // diagnostics
        /*
        this.label =
            ((float)xdiff) + "/" + ((float)ydiff) + "=" + (float) slope
            + " atan=" + (float) r
            + " deg=[ " + (float) Math.toDegrees(r)
            + " ]";
        getLabelBox().setText(this.label);
        */
        
    }

    public void setArrowState(int arrowState)
    {
        if (mArrowState == arrowState)
            return;
        Object old = new Integer(mArrowState);
        if (arrowState < 0 || arrowState > ARROW_BOTH)
            throw new IllegalArgumentException("arrowState < 0 || > " + ARROW_BOTH + ": " + arrowState);
        mArrowState = arrowState;
        layout();
        notify(LWKey.LinkArrows, old);
    }

    public int getArrowState()
    {
        return mArrowState;
    }

    void rotateArrowState()
    {
        if (++mArrowState > ARROW_BOTH)
            mArrowState = ARROW_NONE;
    }

    private void drawArrows(DrawContext dc)
    {
        //-------------------------------------------------------
        // Draw arrows
        //-------------------------------------------------------

        ////ep1Shape.setFrame(this.line.getP1(), new Dimension(arrowSize, arrowSize));
        ////ep1Shape.setFrame(this.line.getX1() - arrowSize/2, this.line.getY1(), arrowSize, arrowSize*2);
        //ep1Shape.setFrame(0,0, arrowSize, arrowSize*2);

        double rotation1 = 0;
        double rotation2 = 0;

        if (curveControls == 1) {
            rotation1 = computeAngle(startX, startY, quadCurve.ctrlx, quadCurve.ctrly);
            rotation2 = computeAngle(endX, endY, quadCurve.ctrlx, quadCurve.ctrly);
        } else if (curveControls == 2) {
            rotation1 = computeAngle(startX, startY, cubicCurve.ctrlx1, cubicCurve.ctrly1);
            rotation2 = computeAngle(endX, endY, cubicCurve.ctrlx2, cubicCurve.ctrly2);
        } else {
            rotation1 = computeAngle(line.getX1(), line.getY1(), line.getX2(), line.getY2());
            rotation2 = rotation1 + Math.PI;  // flip: add 180 degrees
        }

        
        AffineTransform savedTransform = dc.g.getTransform();
        
        dc.g.setStroke(this.stroke);
        dc.g.setColor(getStrokeColor());

        // draw the first arrow
        // todo: adjust the arrow shape with the stroke width
        // do the adjustment in setStrokeWidth, actually.
        //dc.g.translate(line.getX1(), line.getY1());

        if ((mArrowState & ARROW_EP1) != 0) {
            dc.g.translate(startX, startY);
            dc.g.rotate(rotation1);
            dc.g.translate(-ep1Shape.getWidth() / 2, 0); // center shape on point (makes some assumption)
            dc.g.fill(ep1Shape);
            dc.g.draw(ep1Shape);
            dc.g.setTransform(savedTransform);
        }
        
        if ((mArrowState & ARROW_EP2) != 0) {
            // draw the second arrow
            //dc.g.translate(line.getX2(), line.getY2());
            dc.g.translate(endX, endY);
            dc.g.rotate(rotation2);
            dc.g.translate(-ep2Shape.getWidth()/2, 0); // center shape on point 
            dc.g.fill(ep2Shape);
            dc.g.draw(ep2Shape);
            dc.g.setTransform(savedTransform);
        }
    }

    
    public void draw(DrawContext dc)
    {
        if (endpointMoved)
            computeLinkEndpoints();

        BasicStroke stroke = this.stroke;

        // If either end of this link is scaled, scale stroke
        // to smallest of the scales (even better: render the stroke
        // in a variable width narrowing as it went...)
        // todo: cache this scaled stroke
        // todo: do we really even want this functionality?
        /*
        if (ep1 != null && ep2 != null) { // todo cleanup
        if ((ep1 != null && ep1.getScale() != 1f) || (ep2 != null && ep2.getScale() != 1f)) {
            float strokeWidth = getStrokeWidth();
            if (ep1.getScale() < ep2.getScale())
                strokeWidth *= ep1.getScale();
            else
                strokeWidth *= ep2.getScale();
            //g.setStroke(new BasicStroke(strokeWidth));
            stroke = new BasicStroke(strokeWidth);
        } else {
            //g.setStroke(this.stroke);
            stroke = this.stroke;
        }
        }
        */
        Graphics2D g = dc.g;
        
        if (isSelected()) {
            g.setColor(COLOR_HIGHLIGHT);
            g.setStroke(new BasicStroke(stroke.getLineWidth() + 5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));//todo:config
            g.draw(getShape());
        }

        /*
         * Split the curves into green & red halves for debugging
        if (curveControls == 1) {
            QuadCurve2D left = new QuadCurve2D.Float();
            QuadCurve2D right = new QuadCurve2D.Float();
            quadCurve.subdivide(left,right);
            g.setColor(Color.red);
            g.setStroke(STROKE_TWO);
            g.draw(left);
            g.setColor(Color.green);
            g.draw(right);
        } else if (curveControls == 2) {
            CubicCurve2D left = new CubicCurve2D.Float();
            CubicCurve2D right = new CubicCurve2D.Float();
            cubicCurve.subdivide(left,right);
            g.setColor(Color.red);
            g.setStroke(STROKE_TWO);
            g.draw(left);
            g.setColor(Color.green);
            g.draw(right);
        }
        */
        
        //-------------------------------------------------------
        // Draw the stroke
        //-------------------------------------------------------

        if (isIndicated())
            g.setColor(COLOR_INDICATION);
        //else if (isSelected())
        //  g.setColor(COLOR_SELECTION);
        else
            g.setColor(getStrokeColor());

        g.setStroke(stroke);
        
        if (this.curve != null) {
            //-------------------------------------------------------
            // draw the curve
            //-------------------------------------------------------

            g.draw(this.curve);

            if (isSelected()) {
                //-------------------------------------------------------
                // draw faint lines to control points if selected
                // TODO: need to do this at time we paint the selection,
                // so these are always on top -- perhaps have a
                // LWComponent drawSkeleton, who's default is to
                // just draw an outline shape, which can replace
                // the manual code in MapViewer, and in the case of
                // LWLink, can also draw the control lines.
                //-------------------------------------------------------
                g.setColor(COLOR_SELECTION);
                //g.setColor(Color.red);
                //g.setStroke(new BasicStroke(0.5f / (float) g.getTransform().getScaleX()));
                g.setStroke(new BasicStroke(0.5f / (float) dc.zoom));
                // todo opt: less object allocation (put constant screen strokes in the DrawContext)
                if (curveControls == 2) {
                    Line2D ctrlLine = new Line2D.Float(line.getP1(), cubicCurve.getCtrlP1());
                    g.draw(ctrlLine);
                    //float clx1 = line.x1 + cubicCurve.ctrlx
                    ctrlLine.setLine(line.getP2(), cubicCurve.getCtrlP2());
                    g.draw(ctrlLine);
                } else {
                    Line2D ctrlLine = new Line2D.Float(line.getP1(), quadCurve.getCtrlPt());
                    g.draw(ctrlLine);
                    ctrlLine.setLine(line.getP2(), quadCurve.getCtrlPt());
                    g.draw(ctrlLine);
                }
                g.setStroke(stroke);
            }
            //g.drawLine((int)line.getX1(), (int)line.getY1(), (int)curve.getCtrlX(), (int)curve.getCtrlY());
            //g.drawLine((int)line.getX2(), (int)line.getY2(), (int)curve.getCtrlX(), (int)curve.getCtrlY());
        } else {
            //-------------------------------------------------------
            // draw the line
            //-------------------------------------------------------
            g.draw(this.line);
        }

        if (mArrowState > 0)
            drawArrows(dc);

        //-------------------------------------------------------
        // Paint label if there is one
        //-------------------------------------------------------
        
        //float textBoxWidth = 0;
        //float textBoxHeight = 0;
        //boolean textBoxBeingEdited = false;
        Color fillColor = isSelected() ? COLOR_HIGHLIGHT : getFillColor();
        if (fillColor == null && getParent() != null)
            fillColor = getParent().getFillColor();
        //fillColor = ContrastFillColor;
        if (hasLabel()) {
            TextBox textBox = getLabelBox();
            // only draw if we're not an active edit on the map
            if (textBox.getParent() != null) {
                //textBoxBeingEdited = true;
            } else {
                float lx = getLabelX();
                float ly = getLabelY();

                // since links don't have a sensible "location" in terms of an
                // upper left hand corner, the textbox needs to have an absolute
                // map location we can check later for hits -- we set it here
                // everytime we paint -- its a hack.
                //textBox.setMapLocation(lx, ly);

                // We force a fill color on link labels to make sure we create
                // a contrast between the text and the background, which otherwise
                // would include the usually black link stroke in the middle, obscuring
                // some of the text.
                // todo perf: only set opaque-bit/background once/when it changes.
                // (probably put a textbox factory on LWComponent and override in LWLink)

                    //c = getParent().getFillColor(); // todo: maybe have a getBackroundColor which searches up parents
                if (!DEBUG.BOXES) {
                    if (fillColor != null) {
                        textBox.setBackground(fillColor);
                        textBox.setOpaque(true);
                    }
                } else
                    textBox.setOpaque(false);
                
                g.translate(lx, ly);
                //if (isZoomedFocus()) g.scale(getScale(), getScale());
                // todo: need to re-center label when this component relative to scale,
                // and patch contains to understand a scaled label box...
                textBox.draw(dc);
                
                /* draw border
                if (isSelected()) {
                    Dimension s = textBox.getSize();
                    g.setColor(COLOR_SELECTION);
                    //g.setStroke(STROKE_HALF); // todo: needs to be unscaled / handled by selection
                    g.setStroke(new BasicStroke(1f / (float) dc.zoom));
                    // -- i guess we could compute based on zoom level -- maybe MapViewer could
                    // keep such a stroke handy for us... (DrawContext would be handy again...)
                    g.drawRect(0,0, s.width, s.height);
                }
                */
                
                //if (isZoomedFocus()) g.scale(1/getScale(), 1/getScale());
                g.translate(-lx, -ly);
                
                if (false) { // debug
                    // draw label in center of bounding box just for
                    // comparing to our on-curve center computation
                    lx = getCenterX() - textBox.getMapWidth() / 2;
                    ly = getCenterY() - textBox.getMapHeight() / 2;
                    g.translate(lx,ly);
                    //textBox.setBackground(Color.lightGray);
                    textBox.setOpaque(false);
                    g.setColor(Color.blue);
                    textBox.draw(dc);
                    g.translate(-lx,-ly);
                }
            }
        }

        if (mIconBlock.isShowing()) {
            //dc.g.setStroke(STROKE_HALF);
            //dc.g.setColor(Color.gray);
            //dc.g.draw(mIconBlock);
            dc.g.setColor(fillColor);
            dc.g.fill(mIconBlock);
            mIconBlock.draw(dc);
        }
        
        // todo perf: don't have to compute icon block location every time
        /*
        if (!textBoxBeingEdited && mIconBlock.isShowing()) {
            mIconBlock.layout();
            // at right
            //float ibx = getLabelX() + textBoxWidth;
            //float iby = getLabelY();
            // at bottom
            float ibx = getCenterX() - mIconBlock.width / 2;
            float iby = getLabelY() + textBoxHeight;
            mIconBlock.setLocation(ibx, iby);
            mIconBlock.draw(dc);
        }
        */

        if (DEBUG.CONTAINMENT) { g.setStroke(STROKE_HALF); g.draw(getBounds()); }
    }


    //private Point2D.Float[] mPoints = null;
    //private int mPointCount;
    public void layout()
    {
        float cx;
        float cy;

        if (curveControls > 0) {
            cx = mCurveCenterX;
            cy = mCurveCenterY;
        } else {
            cx = getCenterX();
            cy = getCenterY();
        }
        
        /*
         * For very fancy computation of curve center, use below
         * code and then walk the segments computing actual
         * length of curve, then walk again searching for
         * segment at middle of that distance...
         
        if (curveControls > 0) {
            if (mPoints == null)
                mPoints = new Point2D.Float[128];
            // If curved, guess at center of curve via middle segment
            PathIterator i = new java.awt.geom.FlatteningPathIterator(getShape().getPathIterator(null), 0.1);
            float[] point = new float[2];
            int pcnt = 0;
            while (!i.isDone()) {
                i.currentSegment(point);
                if (mPoints[pcnt] == null)
                    mPoints[pcnt] = new Point2D.Float();
                mPoints[pcnt].x = point[0];
                mPoints[pcnt].y = point[1];
                //System.out.println(point[0] + "," + point[1]);
                pcnt++;
                i.next();
            }
            mPointCount = pcnt;
            if (pcnt == 2) {
                cx = getCenterX();
                cy = getCenterY();
            } else {
                int centerp = (int) (pcnt/2+0.5);
                cx = mPoints[centerp].x;
                cy = mPoints[centerp].y;
            }
            System.out.println("CURVE POINTS: " + pcnt);
        } else {
            cx = getCenterX();
            cy = getCenterY();
        }
        */            

        
        float totalHeight = 0;
        float totalWidth = 0;

        boolean putBelow = hasResource();
        
        // Always call LWIcon.Block.layout first to have it compute size/determine if showing
        // before asking it if isShowing()
        
        boolean vertical = false;
        if (hasLabel() && !putBelow) {
            // Check to see if we want to make it vertical
            mIconBlock.setOrientation(LWIcon.Block.VERTICAL);
            mIconBlock.layout();
            vertical = (labelBox.getMapHeight() >= mIconBlock.getHeight());
            if (!vertical) {
                mIconBlock.setOrientation(LWIcon.Block.HORIZONTAL);
                mIconBlock.layout();
            }
        } else {
            // default to horizontal
            mIconBlock.setOrientation(LWIcon.Block.HORIZONTAL);
            mIconBlock.layout();
        }
        
        boolean iconBlockShowing = mIconBlock.isShowing(); // must ask isShowing *after* mIconBlock.layout()
        if (iconBlockShowing) {
            totalWidth += mIconBlock.getWidth();
            totalHeight += mIconBlock.getHeight();
        }


        float lx = 0;
        float ly = 0;
        if (hasLabel()) {
            // since links don't have a sensible "location" in terms of an
            // upper left hand corner, the textbox needs to have an absolute
            // map location we can check later for hits 
            totalWidth += labelBox.getMapWidth();
            totalHeight += labelBox.getMapHeight();
            if (putBelow) {
                // for putting icons below
                lx = cx - labelBox.getMapWidth() / 2;
                ly = cy - totalHeight / 2;
                //if (iconBlockShowing)
                // put label just over center so link splits block & label if horizontal                
                //ly = cy - (labelBox.getMapHeight() + getStrokeWidth() / 2);
            } else {
                // for putting icons at right
                lx = cx - totalWidth / 2;
                ly = cy - labelBox.getMapHeight() / 2;
            }
            labelBox.setMapLocation(lx, ly);
        }
        if (iconBlockShowing) {
            float ibx, iby;
            if (putBelow) {
                // for below
                ibx = (float) (cx - mIconBlock.getWidth() / 2);
                if (hasLabel())
                    iby = labelBox.getMapY() + labelBox.getMapHeight();
                else
                    iby = (float) (cy - mIconBlock.getHeight() / 2f);
                // we're seeing a sub-pixel gap -- this should fix
                iby -= 0.5;
            } else {
                // for at right
                if (hasLabel())
                    ibx = (float) lx + labelBox.getMapWidth();
                else
                    ibx = (float) (cx - mIconBlock.getWidth() / 2);
                iby = (float) (cy - mIconBlock.getHeight() / 2);
                // we're also seeing a sub-pixel gap here -- this should fix
                ibx -= 0.5;
            }
            mIconBlock.setLocation(ibx, iby);
        }
        
        // at right
        //float ibx = getLabelX() + textBoxWidth;
        //float iby = getLabelY();
        // at bottom
        //float ibx = getCenterX() - mIconBlock.width / 2;
        //float iby = getLabelY() + textBoxHeight;
    }

    /*
    public float getLabelY()
    {
        if (hasLabel())
            return labelBox.getMapY();
        else
            return getCenterY();
    }
    public float getLabelX()
    {
        if (hasLabel())
            return labelBox.getMapX();
        else
            return getCenterX();
    }
    
    public float X_getLabelY()
    {
        float y = getCenterY();
        if (hasLabel()) {
            y -= labelBox.getMapHeight() / 2;
            if (mIconBlock.isShowing())
                y -= mIconBlock.getHeight() / 2;
        }
        return y;
    }
    */
    

    /** Create a duplicate LWLink.  The new link will
     * not be connected to any endpoints */
    public LWComponent duplicate()
    {
        //todo: make sure we've got everything (styles, etc)
        LWLink link = (LWLink) super.duplicate();
        link.startX = startX;
        link.startY = startY;
        link.endX = endX;
        link.endY = endY;
        link.centerX = centerX;
        link.centerY = centerY;
        link.ordered = ordered;
        link.mArrowState = mArrowState;
        if (curveControls > 0) {
            link.setCtrlPoint0(getCtrlPoint0());
            if (curveControls > 1)
                link.setCtrlPoint1(getCtrlPoint1());
        }
        return link;
    }
    
    public String paramString()
    {
        return " " + startX+","+startY
            + " -> " + endX+","+endY
            +  " ctrl=" + getControlCount();
    }

    // these two to support a special dynamic link
    // which we use while creating a new link
    //boolean viewerCreationLink = false;
    // todo: this boolean a hack until we no longer need to use
    // clip-regions to draw the links
    LWLink(LWComponent ep2)
    {
        //viewerCreationLink = true;
        this.ep2 = ep2;
        setStrokeWidth(2f); //todo config: default link width
    }
    
    // sets ep1 WIHOUT adding a link ref -- used for
    // temporary drawing of link hack during drag outs --
    // you know, we should just skip using a LWLink object
    // for that crap alltogether. TODO
    void setTemporaryEndPoint1(LWComponent ep1)
    {
        this.ep1 = ep1;
    }
    
    
}
