package tufts.vue;

import java.util.ArrayList;

import java.awt.Shape;
import java.awt.Color;
import java.awt.Font;
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
               VueConstants,
               LWCListener
{
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
    //protected PColor fillColor = new PColor(COLOR_FILL); // todo: use PColor in constants
    //protected PColor textColor = new PColor(COLOR_TEXT);
    //protected PColor strokeColor = new PColor(COLOR_STROKE);
    //protected Color fillColor = COLOR_FILL;
    protected Color fillColor = null;
    protected Color textColor = COLOR_TEXT;
    protected Color strokeColor = COLOR_STROKE;
    protected float strokeWidth = 1f;
    //protected Font font = FONT_DEFAULT;
    protected Font font = null;
    
    /*
     * Runtime only information
     */
    protected transient boolean displayed = true;
    protected transient boolean selected = false;
    protected transient boolean indicated = false;

    protected transient LWContainer parent = null;

    // list of LWLinks that contain us as an endpoint
    private transient ArrayList links = new ArrayList();

    // Scale exists ONLY to support the child-node
    // convenience feature.
    protected transient float scale = 1.0f;
    protected final float ChildScale = 0.75f;

    private transient java.util.List lwcListeners;

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
        this.strokeWidth = w;
        notify("strokeWidth");
    }
    public Font getFont()
    {
        return this.font;
    }
    public void setFont(Font font)
    {
        this.font = font;
        layout();
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
    
    /**
     * If this item supports children,
     * lay them out.
     */
    protected void layout() {}
    
    public boolean isManagedColor()
    {
        return getFillColor() == COLOR_NODE_DEFAULT
            || getFillColor() == COLOR_NODE_INVERTED;
    }
    
    public float getLabelX()
    {
        return getCenterX();
    }
    public float getLabelY()
    {
        return getCenterY();
    }
    
    /* for tracking who's linked to us */
    void addLinkRef(LWLink link)
    {
        this.links.add(link);
    }
    /* for tracking who's linked to us */
    void removeLinkRef(LWLink link)
    {
        this.links.remove(link);
    }
    /* tell us all the links who have us as one of their endpoints */
    java.util.List getLinkRefs()
    {
        return this.links;
    }

    /*
    public java.util.Iterator getNodeIterator()
    {
        return
            new java.util.Iterator() {
                java.util.Iterator i = getChildIterator();
                boolean hasAnother = true;
                LWComponent c = next
                public boolean hasNext()
                {
                    return i.hasNext();
                }
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
            }*/
    
    /**
     * Return an iterator over all link endpoints,
     * which will all be instances of LWComponent.
     * If this is a LWLink, it will include it's
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

    public boolean isChild()
    {
        return this.parent != null || parent instanceof Vue2DMap; // todo: kind of a hack
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

    public LWLink getLinkTo(LWComponent c)
    {
        java.util.Iterator i = this.links.iterator();
        while (i.hasNext()) {
            LWLink lwl = (LWLink) i.next();
            if (lwl.getComponent1() == c || lwl.getComponent2() == c)
                return lwl;
        }
        return null;
    }

    public boolean hasLinkTo(LWComponent c)
    {
        return getLinkTo(c) != null;
    }

    public void setScale(float scale)
    {
        this.scale = scale;
        //System.out.println("Scale set to " + scale + " in " + this);
    }
    
    public float getScale()
    {
        //return 1f;
        return this.scale;
    }
    public float getLayer()
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
        // notify("location"); // todo: does anyone need this?
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
        this.width = w;
        this.height = h;
    }

    public float getX() { return this.x; }
    public float getY() { return this.y; }
    public void setX(float x) { this.x = x; }
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

    public void LWCChanged(LWCEvent e)
    {
        if (e.getSource() == this)
            return;
        System.out.println(e);
    }
    
    public void addLWCListener(LWCListener listener)
    {
        if (lwcListeners == null)
            lwcListeners = new ArrayList();
        lwcListeners.add(listener);
    }
    public void removeLWCListener(LWCListener listener)
    {
        if (lwcListeners == null)
            return;
        lwcListeners.remove(listener);
    }
    public void removeAllLWCListeners()
    {
        lwcListeners = null;
    }
    public void notifyLWCListeners(LWCEvent e)
    {
        if (lwcListeners != null) {
            java.util.Iterator i = lwcListeners.iterator();
            while (i.hasNext())
                ((LWCListener)i.next()).LWCChanged(e);
        }
        if (parent != null)
            parent.notifyLWCListeners(e);
    }
    
    protected void notify(String what)
    {
        // todo: we still need both src & component? (this,this)
        notifyLWCListeners(new LWCEvent(this, this, what));
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
        String s = getClass().getName() + "[id=" + getID();
        if (getLabel() != null)
            s += " \"" + getLabel() + "\"";
        s += " " + x+","+y;
        s += " " + width + "x" + height;
        s += "]";
        return s;
    }
    
}
