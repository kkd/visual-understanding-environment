package tufts.vue;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class ZoomTool extends VueTool
// implements VueKeys -- to keep organized all in one place
{
    static final int KEY_ZOOM_IN_0  = KeyEvent.VK_EQUALS;
    static final int KEY_ZOOM_IN_1  = KeyEvent.VK_ADD;
    static final int KEY_ZOOM_OUT_0 = KeyEvent.VK_MINUS;
    static final int KEY_ZOOM_OUT_1 = KeyEvent.VK_SUBTRACT;
    static final int KEY_ZOOM_FIT   = KeyEvent.VK_0;
    static final int KEY_ZOOM_ACTUAL= KeyEvent.VK_1;
    
    static private final int ZOOM_MANUAL = -1;
    static private final double[] ZoomDefaults = {
        1.0/32, 1.0/24, 1.0/16, 1.0/12, 1.0/8, 1.0/6, 1.0/5, 1.0/4, 1.0/3, 1.0/2, 2.0/3, 0.75,
        1.0,
        1.25, 1.5, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64
    };
    private int curZoom = ZOOM_MANUAL;
    private Point2D zoomPoint = null;

    private static final int ZOOM_FIT_PAD = 16;

    public ZoomTool(MapViewer mapViewer)
    {
        super(mapViewer);
    }
    public String getToolName()
    {
        return "Zoom";
    }

    public boolean handleKeyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();
        if (e.isControlDown()) {
            // todo: if on a mac, make these
            // Meta chords instead (the apple key)
            switch (key) {
            case KEY_ZOOM_IN_0:
            case KEY_ZOOM_IN_1:
                setZoomBigger();
                break;
            case KEY_ZOOM_OUT_0:
            case KEY_ZOOM_OUT_1:
                setZoomSmaller();
                break;
            case KEY_ZOOM_ACTUAL:
                setZoom(1.0);
                break;
            case KEY_ZOOM_FIT:
                setZoomFitContent(e.getComponent());
                this.mapView.repaint();
                break;
            default:
                return false;
            }
            return true;
        }
        return false;
    }
    
    
    /**
     * set the center-on point in the map for the next zoom
     */
    public void setZoomPoint(Point2D mapLocation)
    {
        this.zoomPoint = mapLocation;
    }

    public boolean setZoomBigger()
    {
        if (curZoom == ZOOM_MANUAL) {
            // find next highest zoom default
            for (int i = 0; i < ZoomDefaults.length; i++) {
                if (ZoomDefaults[i] > mapView.getZoomFactor()) {
                    setZoom(ZoomDefaults[curZoom = i]);
                    break;
                }
            }
        } else if (curZoom >= ZoomDefaults.length - 1)
            return false;
        else
            setZoom(ZoomDefaults[++curZoom]);
        return true;
    }
    
    public boolean setZoomSmaller()
    {
        if (curZoom == ZOOM_MANUAL) {
            // find next lowest zoom default
            for (int i = ZoomDefaults.length - 1; i >= 0; i--) {
                if (ZoomDefaults[i] < mapView.getZoomFactor()) {
                    setZoom(ZoomDefaults[curZoom = i]);
                    break;
                }
            }
        } else if (curZoom < 1)
            return false;
        else
            setZoom(ZoomDefaults[--curZoom]);
        return true;
    }
    
    public void setZoom(double zoomFactor)
    {
        setZoom(zoomFactor, true);
    }
    
    private void setZoom(double newZoomFactor, boolean adjustViewport)
    {
        curZoom = ZOOM_MANUAL;
        for (int i = 0; i < ZoomDefaults.length; i++) {
            if (newZoomFactor == ZoomDefaults[i]) {
                curZoom = i;
                break;
            }
        }

        if (adjustViewport) {
            Container c = this.mapView;
            Point2D zoomMapCenter = null;
            if (this.zoomPoint == null) {
                // center on the viewport
                zoomMapCenter = new Point2D.Float(mapView.screenToMapX(c.getWidth() / 2),
                                                  mapView.screenToMapY(c.getHeight() / 2));
            } else {
                // center on given point (e.g., where user clicked)
                zoomMapCenter = this.zoomPoint;
                this.zoomPoint = null;
            }

            float offsetX = (float) (zoomMapCenter.getX() * newZoomFactor) - c.getWidth() / 2;
            float offsetY = (float) (zoomMapCenter.getY() * newZoomFactor) - c.getHeight() / 2;

            this.mapView.setMapOriginOffset(offsetX, offsetY);
        }
        
        this.mapView.setZoomFactor(newZoomFactor);
        
    }

    public void setZoomFitContent(java.awt.Component viewport)
    {
        Point2D.Double offset = new Point2D.Double();
        double newZoom = computeZoomFit(viewport.getSize(),
                                        ZOOM_FIT_PAD,
                                        this.mapView.getMap().getBounds(),
                                        //this.mapView.getAllComponentBounds(),
                                        offset);
        setZoom(newZoom, false);
        this.mapView.setMapOriginOffset(offset.getX(), offset.getY());
    }

    public static double computeZoomFit(Dimension viewport,
                                        int borderGap,
                                        Rectangle2D bounds,
                                        Point2D offset)
    {
        int viewWidth = viewport.width - borderGap * 2;
        int viewHeight = viewport.height - borderGap * 2;
        double vertZoom = (double) viewHeight / bounds.getHeight();
        double horzZoom = (double) viewWidth / bounds.getWidth();
        boolean centerVertical;
        double newZoom;
        if (horzZoom < vertZoom) {
            newZoom = horzZoom;
            centerVertical = true;
        } else {
            newZoom = vertZoom;
            centerVertical = false;
        }

        // Now center the components within the dimension
        // that had extra room to scale in.
                    
        double offsetX = bounds.getX() * newZoom - borderGap;
        double offsetY = bounds.getY() * newZoom - borderGap;

        if (centerVertical)
            offsetY -= (viewHeight - bounds.getHeight()*newZoom) / 2;
        else // center horizontal
            offsetX -= (viewWidth - bounds.getWidth()*newZoom) / 2;

        offset.setLocation(offsetX, offsetY);
        return newZoom;
    }
    
}
