package tufts.vue;


import java.awt.Shape;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// this is really ours
//import java.awt.PColor;

/**
 * LWComponent.java
 * 
 * Light-weight component base class for creating components to be
 * rendered by the MapViewer class.
 *
 * @author Scott Fraize
 * @version 3/10/03
 */

public class LWComponent
    implements MapItem,
               VueConstants
{
    public interface Listener extends java.util.EventListener
    {
        public void LWCChanged(LWCEvent e);
    }
    
    public void setID(String ID)
    {
        if (this.ID != null)
            throw new IllegalStateException("Can't set ID to [" + ID + "], already set on " + this);
        //System.out.println("setID [" + ID + "] on " + this);
        this.ID = ID;
    }
    public void setLabel(String label)
    {
        this.label = label;
        notify("label");
    }
    public void setNotes(String notes)
    {
        this.notes = notes;
        notify("notes");
    }
    public void setMetaData(String metaData)
    {
        this.metaData = metaData;
        notify("meta-data");
    }
    public void setCategory(String category)
    {
        this.category = category;
        notify("category");
    }
    public void setResource(Resource resource)
    {
        this.resource = resource;
        notify("resource");
    }
    public void setResource(String urn)
    {
        if (urn == null || urn.length() == 0)
            setResource((Resource)null);
        else
            setResource(new Resource(urn));
    }
    public Resource getResource()
    {
        return this.resource;
    }
    public String getCategory()
    {
        return this.category;
    }
    public String getID()
    {
        return this.ID;
    }
    public String getLabel()
    {
        return this.label;
    }
    public String getNotes()
    {
        return this.notes;
    }
    public String getMetaData()
    {
        return this.metaData;
    }

    public String OLD_toString()
    {
        String s = getClass().getName() + "[id=" + getID();
        if (getLabel() != null)
            s += " \"" + getLabel() + "\"";
        s += "]";
        return s;
    }

    /*
     * Persistent information
     */
    private static final String EMPTY = "";

    // persistent core
    private String ID = null;
    private String label = null;
    private String notes = null;
    private String metaData = null;
    private String category = null;
    private Resource resource = null;
    private float x;
    private float y;
    
    // persistent impl
    protected float width;
    protected float height;

    protected Color fillColor = null;           //style
    protected Color textColor = COLOR_TEXT;     //style
    protected Color strokeColor = COLOR_STROKE; //style
    protected float strokeWidth = 2f;            //style (equate w/default stroke)
    protected Font font = null;                 //style
    //protected Font font = FONT_DEFAULT;
    
    /*
     * Runtime only information
     */
    protected transient BasicStroke stroke = STROKE_TWO;//equate with above strokeWidth default
    protected transient boolean displayed = true;
    protected transient boolean selected = false;
    protected transient boolean indicated = false;

    protected transient LWContainer parent = null;

    // list of LWLinks that contain us as an endpoint
    private transient java.util.List links = new java.util.ArrayList();

    // Scale currently exists ONLY to support the child-node feature.
    protected transient float scale = 1.0f;
    protected final float ChildScale = 0.75f;

    private transient java.util.List listeners;

    /** for save/restore only & internal use only */
    public LWComponent()
    {
        //System.out.println(Integer.toHexString(hashCode()) + " LWComponent construct of " + getClass().getName());
    }
    
    // If the component has an area, it should
    // implement getShape().  Links, for instance,
    // don't need to implement this.
    public Shape getShape()
    {
        return null;
    }
    public void setShape(Shape shape)
    {
        throw new RuntimeException("UNIMPLEMNTED setShape in " + this);
    }
    public Color getFillColor()
    {
        return this.fillColor;
    }
    public void setFillColor(Color color)
    {
        this.fillColor = color;
        notify("fillColor");
    }
    /** for persistance */
    public String getXMLfillColor()
    {
        return ColorToString(getFillColor());
    }
    /** for persistance */
    public void setXMLfillColor(String xml)
    {
        setFillColor(StringToColor(xml));
    }
    
    public Color getTextColor()
    {
        return this.textColor;
    }
    public void setTextColor(Color color)
    {
        this.textColor = color;
        notify("textColor");
    }
    /** for persistance */
    public String getXMLtextColor()
    {
        return ColorToString(getTextColor());
    }
    /** for persistance */
    public void setXMLtextColor(String xml)
    {
        setTextColor(StringToColor(xml));
    }
    
    public Color getStrokeColor()
    {
        return this.strokeColor;
    }
    public void setStrokeColor(Color color)
    {
        this.strokeColor = color;
        notify("strokeColor");
    }
    /** for persistance */
    public String getXMLstrokeColor()
    {
        return ColorToString(getStrokeColor());
    }
    /** for persistance */
    public void setXMLstrokeColor(String xml)
    {
        setStrokeColor(StringToColor(xml));
    }
    static String ColorToString(Color c)
    {
        if (c == null || (c.getRGB() & 0xFFFFFF) == 0)
            return null;
        return "#" + Integer.toHexString(c.getRGB() & 0xFFFFFF);
    }
    static Color StringToColor(String xml)
    {
	Color c = COLOR_DEFAULT;
        try {
            Integer intval = Integer.decode(xml);
            c = new Color(intval.intValue());
        } catch (NumberFormatException e) {
            System.err.println("[" + xml + "] " + e);
        }
        return c;
    }
    
    
    public float getStrokeWidth()
    {
        return this.strokeWidth;
    }
    public void setStrokeWidth(float w)
    {
        if (this.strokeWidth != w) {
            this.strokeWidth = w;
            if (w > 0)
                this.stroke = new BasicStroke(w);
            else
                this.stroke = STROKE_ZERO;
            notify("strokeWidth");
        }
    }
    public Font getFont()
    {
        return this.font;
    }
    public void setFont(Font font)
    {
        this.font = font;
        //layout();
        notify("font");
    }
    /** to support XML persistance */
    public String getXMLfont()
    {
        //if (this.font == null || this.font == getParent().getFont())
        //return null;
        
	String strStyle;
	if (font.isBold()) {
	    strStyle = font.isItalic() ? "bolditalic" : "bold";
	} else {
	    strStyle = font.isItalic() ? "italic" : "plain";
	}
        return font.getName() + "-" + strStyle + "-" + font.getSize();
    }
    /** to support XML persistance */
    public void setXMLfont(String xml)
    {
        setFont(Font.decode(xml));
    }
    
    public boolean isManagedColor()
    {
        // todo: either get rid of this or make it more sophisticated
        Color c = getFillColor();
        return c != null && (COLOR_NODE_DEFAULT.equals(c) || COLOR_NODE_INVERTED.equals(c));
    }
    
    public float getLabelX()
    {
        return getCenterX();
    }
    public float getLabelY()
    {
        return getCenterY();
    }
    
    public boolean isChild()
    {
        return this.parent != null || parent instanceof LWMap; // todo: kind of a hack
    }
    void setParent(LWContainer c)
    {
        this.parent = c;
    }
    public LWContainer getParent()
    {
        return this.parent;
    }

    public boolean hasChildren()// todo: can we get rid of this?
    {
        return false;
    }

    /* for tracking who's linked to us */
    void addLinkRef(LWLink link)
    {
        if (this.links.contains(link))
            throw new IllegalStateException("addLinkRef: " + this + " already contains " + link);
        this.links.add(link);
    }
    /* for tracking who's linked to us */
    void removeLinkRef(LWLink link)
    {
        if (!this.links.remove(link))
            throw new IllegalStateException("removeLinkRef: " + this + " didn't contain " + link);
    }
    /* tell us all the links who have us as one of their endpoints */
    java.util.List getLinkRefs()
    {
        return this.links;
    }
    
    /**
     * Return an iterator over all link endpoints,
     * which will all be instances of LWComponent.
     * If this is a LWLink, it should include it's
     * own endpoints in the list.
     */
    public java.util.Iterator getLinkEndpointsIterator()
    {
        return
            new java.util.Iterator() {
                java.util.Iterator i = getLinkRefs().iterator();
                public boolean hasNext() {return i.hasNext();}
		public Object next()
                {
                    LWLink lwl = (LWLink) i.next();
                    if (lwl.getComponent1() == LWComponent.this)
                        return lwl.getComponent2();
                    else
                        return lwl.getComponent1();
                }
		public void remove() {
		    throw new UnsupportedOperationException();
                }
            };
    }
    
    /**
     * Return all LWComponents connected via LWLinks to this object.
     * Included everything except LWLink objects themselves (unless
     * it's an endpoint -- a link to a link)
     *
     * todo opt: this is repaint optimization -- when links
     * eventually know their own bounds (they know real connection
     * endpoints) we can re-do this as getAllConnections(), which
     * will can return just the linkRefs and none of the endpoints)
     */
    public java.util.List getAllConnectedNodes()
    {
        java.util.List list = new java.util.ArrayList(this.links.size());
        java.util.Iterator i = this.links.iterator();
        while (i.hasNext()) {
            LWLink l = (LWLink) i.next();
            if (l.getComponent1() != this)
                list.add(l.getComponent1());
            else if (l.getComponent2() != this) // todo opt: remove extra check eventually
                list.add(l.getComponent2());
            else
                throw new IllegalStateException("link to self on " + this);
        }
        return list;
    }

    public LWLink getLinkTo(LWComponent c)
    {
        java.util.Iterator i = this.links.iterator();
        while (i.hasNext()) {
            LWLink l = (LWLink) i.next();
            if (l.getComponent1() == c || l.getComponent2() == c)
                return l;
        }
        return null;
    }

    public boolean hasLinkTo(LWComponent c)
    {
        return getLinkTo(c) != null;
    }

    // todo: okay, this is messy -- do we really want this?
    protected void ensureLinksPaintOnTopOfAllParents()
    {
        java.util.Iterator i = this.links.iterator();
        while (i.hasNext()) {
            LWLink l = (LWLink) i.next();
            // don't need to do anything if link doesn't cross a (logical) parent boundry
            if (l.getComponent1().getParent() == l.getComponent2().getParent())
                continue;
            // also don't need to do anything if link is BETWEEN a parent and a child
            // (in which case, btw, we don't even SEE the link)
            if (l.getComponent1().getParent() == l.getComponent2())
                continue;
            if (l.getComponent2().getParent() == l.getComponent1())
                continue;
            /*
            System.err.println("*** ENSURING " + l);
            System.err.println("    (parent) " + l.getParent());
            System.err.println("  ep1 parent " + l.getComponent1().getParent());
            System.err.println("  ep2 parent " + l.getComponent2().getParent());
            */
            LWContainer commonParent = l.getParent();
            if (commonParent != getParent()) {
                // If we don't have the same parent, we may need to shuffle the deck
                // so that any links to us will be sure to paint on top of the parent
                // we do have, so you can see the link goes to us (this), and not our
                // parent.  todo: nothing in runtime that later prevents user from
                // sending link to back and creating a very confusing visual situation,
                // unless all of our parents happen to be transparent.
                LWComponent topMostParentThatIsSiblingOfLink = getParentWithParent(commonParent);
                if (topMostParentThatIsSiblingOfLink == null)
                    System.err.println("### COULDN'T FIND COMMON PARENT FOR " + this);
                else
                    commonParent.ensurePaintSequence(topMostParentThatIsSiblingOfLink, l);
            }
        }
    }

    LWComponent getParentWithParent(LWContainer parent)
    {
        if (getParent() == parent)
            return this;
        if (getParent() == null)
            return null;
        return getParent().getParentWithParent(parent);
    }

    public void setScale(float scale)
    {
        this.scale = scale;
        notify("scale");
        //System.out.println("Scale set to " + scale + " in " + this);
    }
    
    public float getScale()
    {
        return this.scale;
    }
    public void translate(float dx, float dy)
    {
        setLocation(this.x + dx,
                    this.y + dy);
    }
    public void setLocation(float x, float y)
    {
        //System.out.println(this + " setLocation("+x+","+y+")");
        this.x = x;
        this.y = y;
        //notify("location"); // todo: does anyone need this?
        // also: if enable, don't forget to put in setX/getX!
    }
    
    public void setLocation(double x, double y)
    {
        setLocation((float) x, (float) y);
    }

    public void setLocation(Point2D p)
    {
        setLocation((float) p.getX(), (float) p.getY());
    }
    
    public Point2D getLocation()
    {
        return new Point2D.Float(this.x, this.y);
    }
    
    public void setSize(float w, float h)
    {
        if (DEBUG_LAYOUT) System.out.println("*** LWComponent setSize " + w + "x" + h + " " + this);
        this.width = w;
        this.height = h;
    }

    public float getX() { return this.x; }
    public float getY() { return this.y; }
    /** for XML restore only --todo: remove*/
    public void setX(float x) { this.x = x; }
    /** for XML restore only! --todo remove*/
    public void setY(float y) { this.y = y; }
    public float getWidth() { return this.width * getScale(); }
    public float getHeight() { return this.height * getScale(); }
    public float getCenterX() { return this.x + getWidth() / 2; }
    public float getCenterY() { return this.y + getHeight() / 2; }

    // these 4 for persistance
    public float getAbsoluteWidth() { return this.width; }
    public float getAbsoluteHeight() { return this.height; }
    public void setAbsoluteWidth(float w) { this.width = w; }
    public void setAbsoluteHeight(float h) { this.height = h; }
    
    public Rectangle2D getBounds()
    {
        // todo opt: cache this object?
        return new Rectangle2D.Float(this.x, this.y, getWidth(), getHeight());
    }
    
    /**
     * Default implementation: checks bounding box
     */
    public boolean contains(float x, float y)
    {
        return x >= this.x && x <= (this.x+getWidth())
            && y >= this.y && y <= (this.y+getHeight());
    }
    
    public boolean intersects(Rectangle2D rect)
    {
        return rect.intersects(getX(), getY(), getWidth(), getHeight());
    }

    /**
     * Does x,y fall within the selection target for this component.
     * This default impl adds a 30 pixel swath to bounding box.
     */
    public boolean targetContains(float x, float y)
    {
        final int swath = 30; // todo: preference
        float sx = this.x - swath;
        float sy = this.y - swath;
        float ex = this.x + getWidth() + swath;
        float ey = this.y + getHeight() + swath;
        
        return x >= sx && x <= ex && y >= sy && y <= ey;
    }

    /**
     * We divide area around the bounding box into 8 regions -- directly
     * above/below/left/right can compute distance to nearest edge
     * with a single subtract.  For the other regions out at the
     * corners, do a distance calculation to the nearest corner.
     * Behaviour undefined if x,y are within component bounds.
     */
    public float distanceToEdgeSq(float x, float y)
    {
        float ex = this.x + getWidth();
        float ey = this.y + getHeight();

        if (x >= this.x && x <= ex) {
            // we're directly above or below this component
            return y < this.y ? this.y - y : y - ey;
        } else if (y >= this.y && y <= ey) {
            // we're directly to the left or right of this component
            return x < this.x ? this.x - x : x - ex;
        } else {
            // This computation only makes sense following the above
            // code -- we already know we must be closest to a corner
            // if we're down here.
            float nearCornerX = x > ex ? ex : this.x;
            float nearCornerY = y > ey ? ey : this.y;
            float dx = nearCornerX - x;
            float dy = nearCornerY - y;
            return dx*dx + dy*dy;
        }
    }

    public Point2D nearestPoint(float x, float y)
    {
        float ex = this.x + getWidth();
        float ey = this.y + getHeight();
        Point2D.Float p = new Point2D.Float(x, y);

        if (x >= this.x && x <= ex) {
            // we're directly above or below this component
            if (y < this.y)
                p.y = this.y;
            else
                p.y = ey;
        } else if (y >= this.y && y <= ey) {
            // we're directly to the left or right of this component
            if (x < this.x)
                p.x = this.x;
            else
                p.x = ex;
        } else {
            // This computation only makes sense following the above
            // code -- we already know we must be closest to a corner
            // if we're down here.
            float nearCornerX = x > ex ? ex : this.x;
            float nearCornerY = y > ey ? ey : this.y;
            p.x = nearCornerX;
            p.y = nearCornerY;
        }
        return p;
    }

    public float distanceToEdge(float x, float y)
    {
        return (float) Math.sqrt(distanceToEdgeSq(x, y));
    }

    
    /**
     * Return the square of the distance from x,y to the center of
     * this components bounding box.
     */
    public float distanceToCenterSq(float x, float y)
    {
        float cx = getCenterX();
        float cy = getCenterY();
        float dx = cx - x;
        float dy = cy - y;
        return dx*dx + dy*dy;
    }
    
    public float distanceToCenter(float x, float y)
    {
        return (float) Math.sqrt(distanceToCenterSq(x, y));
    }
    
    public void draw(java.awt.Graphics2D g)
    {
        throw new RuntimeException("UNIMPLEMNTED draw in " + this);
    }

    public void addLWCListener(LWComponent.Listener listener)
    {
        if (listeners == null)
            listeners = new java.util.ArrayList();
        listeners.add(listener);
    }
    public void removeLWCListener(LWComponent.Listener listener)
    {
        if (listeners == null)
            return;
        listeners.remove(listener);
    }
    public void removeAllLWCListeners()
    {
        if (listeners != null)
            listeners.clear();
    }
    public void notifyLWCListeners(LWCEvent e)
    {
        if (listeners != null) {
            java.util.Iterator i = listeners.iterator();
            while (i.hasNext()) {
                Listener l = (Listener) i.next();
                if (DEBUG_EVENTS) System.out.println(e + " -> " + l);
                l.LWCChanged(e);
            }
        }

        // todo: have a seperate notifyParent? -- every parent
        // shouldn't have to be a listener

        // todo: "added" events don't need to go thru parent chain as
        // a "childAdded" event has already taken place (but
        // listeners, eg, inspectors, may need to know to see if the
        // parent changed)
        
        if (parent != null)
            parent.notifyLWCListeners(e);
    }
    
    protected void notify(String what)
    {
        // todo: we still need both src & component? (this,this)
        notifyLWCListeners(new LWCEvent(this, this, what));
    }
    protected void notify(String what, LWComponent c)
    {
        notifyLWCListeners(new LWCEvent(this, c, what));
    }

    /**
     * Do any cleanup needed now that this LWComponent has
     * been removed from the model
     */
    protected void removeFromModel()
    {
        removeAllLWCListeners();
    }
    
    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }
    public boolean isSelected()
    {
        return this.selected;
    }
    
    public void setDisplayed(boolean displayed)
    {
        this.displayed = displayed;
    }
    public boolean isDisplayed()
    {
        return this.displayed;
    }

    public void setIndicated(boolean indicated)
    {
        this.indicated = indicated;
    }
    
    public boolean isIndicated()
    {
        return this.indicated;
    }

    /** pesistance default */
    public void addObject(Object obj)
    {
        System.err.println("Unhandled XML obj: " + obj);
    }

    public String toString()
    {
        String cname = getClass().getName();
        String s = cname.substring(cname.lastIndexOf('.')+1);
        s += "[" + getID();
        if (getLabel() != null)
            s += " \"" + getLabel() + "\"";
        if (getScale() != 1f)
            s += " z" + getScale();
        s += " " + x+","+y;
        s += " " + width + "x" + height;
        s += "]";
        return s;
    }
}
