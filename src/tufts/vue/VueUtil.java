package tufts.vue;

import java.util.*;

public class VueUtil
{
    private static boolean WindowsPlatform = false;
    private static boolean MacPlatform = false;
    private static boolean UnixPlatform = false;
    private static float javaVersion = 1.0f;
    private static String currentDirectoryPath = "";
    
    // Mac OSX Java 1.4.1 has a bug where stroke's are exactly 0.5
    // units off center (down/right from proper center).  You need to
    // draw a minumim stroke on top of a stroke of more than 1 to see
    // this, because at stroke width 1, this looks appears as a policy
    // of drawing strokes down/right to the line. Note that there are
    // other problems with Mac strokes -- a stroke width of 1.0
    // doesn't appear to scale with the graphics context, but any
    // value just over 1.0 will.
    public static boolean StrokeBug05 = false;
   
    static {
        String osName = System.getProperty("os.name");
        String javaSpec = System.getProperty("java.specification.version");

        try {
            javaVersion = Float.parseFloat(javaSpec);
            System.out.println("Java Version: " + javaVersion);
        } catch (Exception e) {
            System.err.println("Couldn't parse java.specifcaion.version: [" + javaSpec + "]");
            System.err.println(e);
        }

        String osn = osName.toUpperCase();
        if (osn.startsWith("MAC")) {
            MacPlatform = true;
            System.out.println("Mac JVM: " + osName);
            System.out.println("Mac mrj.version: " + System.getProperty("mrj.version"));
        } else if (osn.startsWith("WINDOWS")) {
            WindowsPlatform = true;
            System.out.println("Windows JVM: " + osName);
        } else {
            UnixPlatform = true;
        }
        //if (isMacPlatform()) 
        //  StrokeBug05 = true; // todo: only if mrj.version < 69.1, where they fixed this bug
        if (StrokeBug05)
            System.out.println("Stroke compensation active (0.5 unit offset bug)");
    }

    public static void main(String args[])
    {
        test_OpenURL();
    }

    public static void test_OpenURL()
    {
         System.getProperties().list(System.out);
        try {
            openURL("file:///tmp/two words.txt");       // does not work on OSX 10.2
            openURL("\"file:///tmp/two words.txt\"");   // does not work on OSX 10.2
            openURL("\'file:///tmp/two words.txt\'");   // does not work on OSX 10.2
            openURL("file:///tmp/two%20words.txt");     // works on OSX 10.2, but not Windows 2000
            //openURL("file:///tmp/foo.txt");
            //openURL("file:///tmp/index.html");
            //openURL("file:///tmp/does_not_exist");
            //openURL("file:///zip/About_Developer_Tools.pdf");
        } catch (Exception e) {
            System.err.println(e);
        }
    }


    public static double getJavaVersion()
    {
        return javaVersion;
    }
       

    public static void openURL(String url)
        throws java.io.IOException
    {
        // todo: spawn this in another thread just in case it hangs
        
        // there appears to be no point in quoting the URL...
        String quotedURL;
        if (true || url.charAt(0) == '\'')
            quotedURL = url;
        else
            quotedURL = "\'" + url + "\'";
        
        if (isMacPlatform())
            openURL_Mac(quotedURL);
        else if (isUnixPlatform())
            openURL_Unix(quotedURL);
        else // default is a windows platform
            openURL_Windows(quotedURL);
    }

    private static final String PC_OPENURL_CMD = "rundll32 url.dll,FileProtocolHandler";
    private static void openURL_Windows(String url)
        throws java.io.IOException
    {
        String cmd = PC_OPENURL_CMD + " " + url;
        System.err.println("Opening PC URL with: [" + cmd + "]");
        Process p = Runtime.getRuntime().exec(cmd);
        if (false) {
            try {
                System.err.println("waiting...");
                p.waitFor();
            } catch (Exception ex) {
                System.err.println(ex);
            }
            System.err.println("exit value=" + p.exitValue());
        }
    }
    
    private static void openURL_Mac(String url)
        throws java.io.IOException
    {
        System.err.println("Opening Mac URL: [" + url + "]");
        if (url.indexOf(':') < 0 && !url.startsWith("/")) {
            // OSX won't default to use current directory
            // for a relative reference, so we prepend
            // the current directory manually.
            url = "file://" + System.getProperty("user.dir") + "/" + url;
            System.err.println("Opening Mac URL: [" + url + "]");
        }
        if (getJavaVersion() >= 1.4f) {
            // FYI -- this will not compile using mac java 1.3
          //  com.apple.eio.FileManager.openURL(url);

            // use this if want to compile < 1.4
            //Class c = Class.forName("com.apple.eio.FileManager");
            //java.lang.reflect.Method openURL = c.getMethod("openURL", new Class[] { String[].class });
            //openURL.invoke(null, new Object[] { new String[] { url } });

        } else {
            // this has been deprecated in mac java 1.4, so
            // just ignore the warning if using a 1.4 or beyond
            // compiler
        //    com.apple.mrj.MRJFileUtils.openURL(url);
        }
        System.err.println("returned from openURL_Mac " + url);
    }

    private static void openURL_Unix(String url)
        throws java.io.IOException
    {
        throw new java.io.IOException("Unimplemented");
    }


    public static boolean isWindowsPlatform()
    {
        return WindowsPlatform;
    }
    public static boolean isMacPlatform()
    {
        return MacPlatform;
    }
    public static boolean isUnixPlatform()
    {
        return UnixPlatform;
    }
    

    public static class GroupIterator implements Iterator
    {
        private Object[] iterables = new Object[4];
        int nIter = 0;
        int iterIndex = 0;
        Iterator curIter;
        
        public GroupIterator(Object l1, Object l2)
        {
            this(l1, l2, null);
        }
        public GroupIterator(Object l1, Object l2, Object l3)
        {
            if (l1 == null || l2 == null)
                throw new IllegalArgumentException("null Collection or Iterator");
            iterables[nIter++] = l1;
            iterables[nIter++] = l2;
            if (l3 != null)
                iterables[nIter++] = l3;
            for (int i = 0; i < nIter; i++) {
                if (!(iterables[i] instanceof Collection) &&
                    !(iterables[i] instanceof Iterator))
                    throw new IllegalArgumentException("arg i not Collection or Iterator");
            }
        }

        public boolean hasNext()
        {
            if (curIter == null) {
                if (iterIndex >= nIter)
                    return false;
                if (iterables[iterIndex] instanceof Collection)
                    curIter = ((Collection)iterables[iterIndex]).iterator();
                else
                    curIter = (Iterator) iterables[iterIndex];
                iterIndex++;
                if (curIter == null)
                    return false;
            }
            if (!curIter.hasNext()) {
                curIter = null;
                return hasNext();
            }
            return true;
        }

        public Object next()
        {
            if (curIter == null)
                return null;
            else
                return curIter.next();
        }

        public void remove()
        {
            if (curIter != null)
                curIter.remove();
        }
    }

    /*
     * Compute two items: the zoom factor that will fit
     * everything within the given bounds into the given
     * viewport, and put into @param offset the offset
     * to place the viewport at. Used to figure out how
     * to fit everything within a map on the screen and
     * where to pan to so you can see it all.
     */
    public static double computeZoomFit(java.awt.Dimension viewport,
                                        int borderGap,
                                        java.awt.geom.Rectangle2D bounds,
                                        java.awt.geom.Point2D offset)
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
    
    public static void  setCurrentDirectoryPath(String cdp) {
        currentDirectoryPath = cdp;
    }
    
    public static String getCurrentDirectoryPath() {
        return currentDirectoryPath;
    }    
    
    public static boolean isCurrentDirectoryPathSet() {
        if(currentDirectoryPath.equals("")) 
            return false;
        else
            return true;
    }


}
