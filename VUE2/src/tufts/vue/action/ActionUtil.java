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

package tufts.vue.action;

import tufts.Util;
import tufts.vue.VueUtil;
import tufts.vue.VUE;
import tufts.vue.LWMap;
import tufts.vue.VueFileFilter;
import tufts.vue.VueResources;
import tufts.vue.XMLUnmarshalListener;
import tufts.vue.DEBUG;

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.MarshalListener;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.UnmarshalListener;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.exolab.castor.util.Logger;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import java.net.URL;
import java.net.URI;
import java.util.HashMap;
import java.io.*;

/**
 * A class which defines utility methods for any of the action class.
 * Most of this code is for save/restore persistance thru castor XML.
 *
 * @version $Revision: 1.44 $ / $Date: 2006-08-10 21:33:03 $ / $Author: sfraize $
 * @author  Daisuke Fujiwara
 * @author  Scott Fraize
 */
// TODO: rename / relocate most of this code! -- SMF
public class ActionUtil {
    private final static String XML_MAPPING_CURRENT_VERSION_ID = VueResources.getString("mapping.lw.current_version");
    private final static URL XML_MAPPING_DEFAULT =      VueResources.getURL("mapping.lw.version_" + XML_MAPPING_CURRENT_VERSION_ID);
    private final static URL XML_MAPPING_UNVERSIONED =  VueResources.getURL("mapping.lw.version_none");
    private final static URL XML_MAPPING_OLD_RESOURCES =VueResources.getURL("mapping.lw.version_resource_fix");

    private final static String VUE_COMMENT_START = "<!-- Do Not Remove:";
    private final static String OUTPUT_ENCODING = "US-ASCII";
    private final static String DEFAULT_WINDOWS_ENCODING = "windows-1252"; // (a.k.a Cp1252) for reading pre ASCII enforced save files from Windows
    private final static String DEFAULT_MAC_ENCODING = "UTF-8"; // "MacRoman" not supported on Windows platform
    private final static String DEFAULT_INPUT_ENCODING = "UTF-8"; // safest default input encoding

    // Note: the encoding format of the incoming file will normally either be UTF-8 for
    // older VUE save files, or US-ASCII for newer files.  In any case, the encoding is
    // indicated in the <?xml> tag at the top of the file, and castor handles adjusting
    // for it.  If we want to write UTF-8 files, we have to be sure the stream that's
    // created to write the file is created with the same encoding, or we sometimes get
    // problems, depending on the platform.  We always READ (unmarshall) via a UTF-8
    // stream, no matter what, as US-ASCII will pass through a UTF-8 stream untouched,
    // and it will handle UTF-8 if that turns out to be the encoding.
    
    public ActionUtil() {}
    
    /**A static method which displays a file chooser for the user to choose which file to save into.
       It returns the selected file or null if the process didn't complete*/
    public static File selectFile(String title, String fileType)
    {
        File picked = null;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setAcceptAllFileFilterUsed(false);
        
        if (fileType != null)
         chooser.setFileFilter(new VueFileFilter(fileType)); 
        
        else
        {
            VueFileFilter defaultFilter = new VueFileFilter("vue");
            
            chooser.addChoosableFileFilter(defaultFilter);  
            chooser.addChoosableFileFilter(new VueFileFilter("jpeg"));  
            chooser.addChoosableFileFilter(new VueFileFilter("svg"));
            //chooser.addChoosableFileFilter(new VueFileFilter("pdf"));
            //chooser.addChoosableFileFilter(new VueFileFilter("html"));
            chooser.addChoosableFileFilter(new VueFileFilter("imap"));
            
            chooser.setFileFilter(defaultFilter); 
        }
            
        if(VueUtil.isCurrentDirectoryPathSet()) 
          chooser.setCurrentDirectory(new File(VueUtil.getCurrentDirectoryPath()));  
        
        int option = chooser.showDialog(VUE.getDialogParent(), "Save");
        
        if (option == JFileChooser.APPROVE_OPTION) 
        {
            picked = chooser.getSelectedFile();
            
            String fileName = picked.getAbsolutePath();
            String extension = chooser.getFileFilter().getDescription();
                
            //if it isn't a file name with the right extention 
            if (!fileName.endsWith("." + extension)) {
                fileName += "." + extension;
                picked = new File(fileName);
            }
            
            if (picked.exists()) {
                int n = JOptionPane.showConfirmDialog(null, "Would you Like to Replace the File \'" + picked.getName() + "\'", 
                        "Replacing File", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                  
                if (n == JOptionPane.NO_OPTION)
                    picked = null;
            } 
            
            if (picked != null)
                VueUtil.setCurrentDirectoryPath(picked.getParent());
        }
        
        return picked;
    }
    
    /**A static method which displays a file chooser for the user to choose which file to open.
       It returns the selected file or null if the process didn't complete
    TODO BUG: do not allow more than one dialog open at a time -- two "Ctrl-O" in quick succession
    will open two open file dialogs. */
    
    public static File openFile(String title, String extension)
    {
        File file = null;
        
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new VueFileFilter(extension));
        
        if (VueUtil.isCurrentDirectoryPathSet()) 
            chooser.setCurrentDirectory(new File(VueUtil.getCurrentDirectoryPath()));  
        
        int option = chooser.showDialog(VUE.getDialogParent(), "Open");
        
        if (option == JFileChooser.APPROVE_OPTION) {
            final File chooserFile = chooser.getSelectedFile();
            if (chooserFile == null)
                return null;
            final String fileName;
            final String chosenPath = chooserFile.getAbsolutePath();
            
            // if they type a file name w/out an extension
            if (chooserFile.getName().indexOf('.') < 0)
                fileName = chosenPath + "." + extension;
            else
                fileName = chosenPath;

            file = new File(fileName);

            if (file.exists()) {
                VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getParent());
            } else {
                File dir = new File(chosenPath);
                if (dir.exists() && dir.isDirectory()) {
                    //System.out.println("chdir " + chosenPath);
                    VueUtil.setCurrentDirectoryPath(chosenPath);
                } else {
                    VUE.Log.debug("File '" + chosenPath + "' " + file + " can't  be found.");
                    tufts.vue.VueUtil.alert(chooser, "Could not find " + file, "File Not Found");
                }
                file = null;
            }
        }
        return file;
    }
    
    
    /**
     * Return the current mapping used for saving new VUE data.
     */
    public static Mapping getDefaultMapping()
    {
        Object result = _loadMapping(XML_MAPPING_DEFAULT);
        if (result instanceof Exception) {
            JOptionPane.showMessageDialog(null, "Mapping file error: will be unable to load or save maps!"
                                          + "\nMapping url: " + XML_MAPPING_DEFAULT
                                          + "\n" + result,
                                          "XML Mapping File Exception", JOptionPane.ERROR_MESSAGE);
        }
        return (Mapping) result;
    }

    public static Unmarshaller getDefaultUnmarshaller()
        throws org.exolab.castor.mapping.MappingException
    {
        return getDefaultUnmarshaller(null, "(unknown source)");
    }
    public static Unmarshaller getDefaultUnmarshaller(String sourceName)
        throws org.exolab.castor.mapping.MappingException
    {
        return getDefaultUnmarshaller(null, sourceName);
    }
    
    /**
     * Return the default unmarshaller for VUE data, which includes an installed
     * unmarshall listener, which is required for the proper restoration of VUE objects.
     */
    public static Unmarshaller getDefaultUnmarshaller(Mapping mapping, String sourceName)
        throws org.exolab.castor.mapping.MappingException
    {
        if (mapping == null)
            mapping = getDefaultMapping();
        
        // todo: can cache this with it's mapping set (tho need to cache by mapping,
        // as we still have different mapping files for old versions of the VUE save file)
        Unmarshaller unmarshaller = new Unmarshaller();
        
        unmarshaller.setIgnoreExtraAttributes(true);
        unmarshaller.setIgnoreExtraElements(true);
        unmarshaller.setValidation(false);
        //unmarshaller.setWhitespacePreserve(true); // doesn't affect elements!  (e.g. <notes> foo bar </notes>)
        // HOWEVER: castor 0.9.7 now automatically encodes/decodes white space for attributes...
        //unmarshaller.setLogWriter(new PrintWriter(System.out));
        Logger logger = new Logger(System.err);
        if (sourceName != null)
            logger.setPrefix("Castor " + sourceName);
        else
            logger.setPrefix("Castor");
        unmarshaller.setLogWriter(logger);
        //unmarshaller.setLogWriter(new Logger.getSystemLogger());
        // not a good sign: above getSystemLogger is decl public in castor code, but not public in doc, and non-existent in jar

        if (DEBUG.XML) unmarshaller.setDebug(true);
        
        unmarshaller.setUnmarshalListener(new VueUnmarshalListener());
        unmarshaller.setMapping(mapping);

        if (DEBUG.CASTOR || DEBUG.XML || DEBUG.IO)
            VUE.Log.debug("got default unmarshaller for mapping " + mapping + " source " + sourceName + "\n\n");

        return unmarshaller;
    }
    

    private static Mapping getMapping(URL mappingSource) {
        Object result = _loadMapping(mappingSource);
        if (result instanceof Mapping)
            return (Mapping) result;
        else
            return null;
    }
    
    private static HashMap LoadedMappings = new HashMap();
    /** return's a Mapping if succesful, or an Exception if not.
     * Results are cahced (if load was successful) for future calls.*/
    private static Object _loadMapping(URL mappingSource)
    //throws java.io.IOException //, org.exolab.castor.mapping.MappingException
    {
        if (LoadedMappings.containsKey(mappingSource))
            return (Mapping) LoadedMappings.get(mappingSource);
        Mapping mapping = new Mapping();
        if (DEBUG.IO || DEBUG.INIT) System.out.println("Loading mapping " + mappingSource + "...");
        try {
            mapping.loadMapping(mappingSource);
        } catch (Exception e) { // MappingException or IOException
            e.printStackTrace();
            System.err.println("Failed to load mapping " + mappingSource);
            return e;
        }
        if (DEBUG.IO || DEBUG.INIT) System.out.println("*Loaded mapping " + mappingSource);
        LoadedMappings.put(mappingSource, mapping);
        return mapping;
    }
    
    /**A static method which creates an appropriate marshaller and marshal the active map*/
    public static void marshallMap(File file)
    {
        marshallMap(file, tufts.vue.VUE.getActiveMap());
    }

    /**
     * Marshall the given map to XML and write it out to the given file.
     */
    public static void marshallMap(File file, LWMap map)
    /*throws java.io.IOException,
               org.exolab.castor.mapping.MappingException,
               org.exolab.castor.xml.MarshalException,
               org.exolab.castor.xml.ValidationException*/
    {
        Marshaller marshaller = null;

        try {  
            final String path = file.getAbsolutePath().replaceAll("%20"," ");
            final Writer writer;
            if (OUTPUT_ENCODING.equals("UTF-8") || OUTPUT_ENCODING.equals("UTF8")) {
                writer = new OutputStreamWriter(new FileOutputStream(path), OUTPUT_ENCODING);
            } else {
                writer = new FileWriter(path);
                // For the actual file writer we can use the default encoding because
                // we're marshalling specifically in US-ASCII.  E.g., because we direct
                // castor to fully encode any special characters via
                // setEncoding("US-ASCII"), we'll only have ASCII chars to write anyway,
                // and any default encoding will handle that...
                
            }
            
            writer.write(VUE_COMMENT_START
                         + " VUE mapping "
                         + "@version(" + XML_MAPPING_CURRENT_VERSION_ID + ")"
                         + " " + XML_MAPPING_DEFAULT
                         + " -->\n");
            writer.write(VUE_COMMENT_START
                         + " Saved date " + new java.util.Date()
                         + " by " + VUE.getSystemProperty("user.name")
                         + " on platform " + VUE.getSystemProperty("os.name")
                         + " " + VUE.getSystemProperty("os.version")
                         + " in JVM " + VUE.getSystemProperty("java.runtime.version")
                         + " -->\n");
            writer.write(VUE_COMMENT_START
                         + " Saving version " + tufts.vue.Version.WhatString
                         + " -->\n");
            if (DEBUG.CASTOR || DEBUG.IO) System.out.println("Wrote VUE header to " + writer);
            marshaller = new Marshaller(writer);
            //marshaller.setDebug(DEBUG.CASTOR);
            marshaller.setDebug(true); // doesn't appear to do anything yet...
            marshaller.setEncoding(OUTPUT_ENCODING);
            // marshal as document (default): make sure we add at top: <?xml version="1.0" encoding="<encoding>"?>
            marshaller.setMarshalAsDocument(true);
            marshaller.setNoNamespaceSchemaLocation("none");
            // setting to "none" gets rid of all the spurious tags like these:
            // xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"

            //marshaller.setDoctype("foo", "bar"); // not in 0.9.4.3, must wait till we can run 0.9.5.3+

            /*
            marshaller.setMarshalListener(new MarshalListener() {
                    public boolean preMarshal(Object o) {
                        System.out.println(" preMarshal " + o.getClass().getName() + " " + o);
                        return true;
                    }
                    public void postMarshal(Object o) {
                        System.out.println("postMarshal " + o.getClass().getName() + " " + o);
                    }
                });
            */

            //marshaller.setRootElement("FOOBIE"); // overrides name of root element
            
            marshaller.setMapping(getDefaultMapping());
            Logger logger = new Logger(System.err);
            logger.setPrefix("Castor ");
            marshaller.setLogWriter(logger);
            
            if (DEBUG.CASTOR || DEBUG.IO) System.out.println("Marshalling " + map + " ...");
            marshaller.marshal(map);
            if (DEBUG.CASTOR || DEBUG.IO) System.out.println("Completed marshalling " + map);
            
            writer.flush();
            writer.close();

            map.setFile(file);
            map.markAsSaved();

            if (DEBUG.CASTOR || DEBUG.IO) System.out.println("Wrote " + file);

        } catch (Exception e) {
            System.err.println("ActionUtil.marshallMap: " + e);
            e.printStackTrace();
            // until everyone has chance to update their code
            // to handle the exceptions, wrap this in a runtime exception.
            throw new RuntimeException(e);
        }

    }

    private static class VueUnmarshalListener implements UnmarshalListener {
        public void initialized(Object o) {
            if (DEBUG.XML) System.out.println("**** VUL initialized " + o.getClass().getName() + " " + o);
            if (o instanceof XMLUnmarshalListener)
                ((XMLUnmarshalListener)o).XML_initialized();
        }
        public void attributesProcessed(Object o) {
            if (DEBUG.XML) System.out.println("      got attributes " + o.getClass().getName() + " " + o);
        }
        public void unmarshalled(Object o) {
            if (DEBUG.XML||DEBUG.CASTOR) System.out.println("VUL unmarshalled " + o.getClass().getName() + " " + o);
            if (o instanceof XMLUnmarshalListener)
                ((XMLUnmarshalListener)o).XML_completed();
        }
        public void fieldAdded(String name, Object parent, Object child) {
            if (DEBUG.XML) System.out.println("VUL fieldAdded: parent: " + parent.getClass().getName() + "\t[" + parent + "]\n"
                             + "             new child: " +  child.getClass().getName() + " \"" + name + "\" [" + child + "]\n"
                               );
            if (child instanceof XMLUnmarshalListener)
                ((XMLUnmarshalListener)child).XML_addNotify(name, parent);
        }
    }

    /** Unmarshall a LWMap from the given file (XML map data) */
    public static LWMap unmarshallMap(File file)
        throws java.io.IOException
    {
        return unmarshallMap(file.toURL());
        //return unmarshallMap(file.toURI().toURL());
    }

    /** Unmarshall a LWMap from the given URL (XML map data) */
    /*
    public static LWMap unmarshallMap(java.net.URL url)
        throws java.io.IOException
    {
        //return unmarshallMap(url, getDefaultMapping());
        return unmarshallMap(url, null);
    }
    */

    /** Unmarshall a LWMap from the given URL using the given mapping */
    /*
    private static LWMap unmarshallMap(java.net.URL url, Mapping mapping)
        throws java.io.IOException
    {
        return unmarshallMap(url, mapping, DEFAULT_INPUT_ENCODING);
    }
    */
    
    public static LWMap unmarshallMap(java.net.URL url)
        throws java.io.IOException
    {
        // We scan for lines at top of file that are comments.  If there are NO comment lines, the
        // file is of one of our original save formats that is not versioned, and that may need
        // special processing for the Resource class to Resource interface change over.  If there
        // are comments, the version instance of the string "@version(##)" will set the version ID
        // to ##, and we'll use the mapping appropriate for that version of the save file.

        // We ALWAYS read with an input encoding of UTF-8, even if the XML was written with a
        // US-ASCII encoding.  This is because pure ascii will translate fine through UTF-8, but in
        // case it winds up being that the XML was written out my the marshaller with a UTF-8
        // encoding, we're covered.

        // (tho maybe with very old save files with platform specific encodings, (e.g, MacRoman or
        // windows-1255/Cp1255) we'll lose a special char here or there, such as left-quote /
        // right-quote).
        
        if (DEBUG.CASTOR || DEBUG.IO) System.out.println("\nSCANNING: " + url);
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        
        String firstNonCommentLine;
        String versionID = null;
        boolean savedOnWindowsPlatform = false;
        boolean savedOnMacPlatform = false;
        String guessedEncoding = null;
        Mapping mapping = null;
    
        // We need to skip past the comments to position the reader at the <?xml line for
        // unmarshalling to start.  Also, we look at these comments to determine version of the
        // mapping to use, as well as if it's a pre VUE 1.5 (August 2006) save file, in which case
        // we must guess an encoding, and re-open the file using an InputStreamReader with the
        // proper encoding.

        for (;;) {
            reader.mark(2048); // a single comment line can't be longer than this...
            String line = reader.readLine();
            if (line == null) {
                System.err.println("Unexpected end-of-stream in [" + url + "]");
                throw new java.io.IOException("end of stream in " + url);
            }
            if (DEBUG.CASTOR || DEBUG.IO) System.out.println("Scanning[" + line + "]");
            if (line.startsWith("<!--") == false) {
                // we should have just hit thie "<?xml ..." line -- done with comments
                firstNonCommentLine = line;
                break;
            }

            if (line.startsWith(VUE_COMMENT_START + " Saved")) {

                // The "saved on platform" comments were never expected to be used functionally
                // (only for debug), so determining if the save file was written on a Windows box
                // it's not 100% reliable: e.g., if a user somehow had the name "platform Windows",
                // we would mistake this "saved by" for a "saved on", but we're just going to take
                // this risk -- this is just a workaround backward compat hack because castor
                // wasn't naming the real encoding in it's XML output (turns out it was always
                // puting UTF-8), even if it was using the default Windows encoding of
                // Cp1252/windows-1252.
                
                //if (DEBUG.IO) System.out.println("scanning for Windows platform...");
                if (line.indexOf("platform Windows") > 0) {
                    if (DEBUG.IO) System.out.println(url + " was saved in the Windows environment");
                    savedOnWindowsPlatform = true;
                } else if (line.indexOf("platform Mac") > 0) {
                    if (DEBUG.IO) System.out.println(url + " was saved in the Mac environment");
                    savedOnMacPlatform = true;
                }
            }
            
            
            // Scan the comment line for a version tag to base our mapping on:
            int idx;
            if ((idx = line.indexOf("@version(")) >= 0) {
                String s = line.substring(idx);
                //System.out.println("Found version start:" + s);
                int x = s.indexOf(')');
                if (x > 0) {
                    versionID = s.substring(9,x);
                    if (DEBUG.CASTOR || DEBUG.IO) System.out.println("Found version ID[" + versionID + "]");
                    if (versionID.equals(XML_MAPPING_CURRENT_VERSION_ID)) {
                        mapping = getDefaultMapping();
                    } else {
                        URL mappingURL = VueResources.getURL("mapping.lw.version_" + versionID);
                        if (mappingURL == null) {
                            VUE.Log.error("Failed to find mapping for version tag [" + versionID + "], attempting default.");
                            mapping = getDefaultMapping();
                        } else {
                            mapping = getMapping(mappingURL);
                        }
                    }
                }
            }
        }
        reader.close();

        if (firstNonCommentLine.startsWith("<?xml")) {
            // Check to see if we need to use a non-default input encoding.
            // NOTE: We make sure we only attempt guessedEncoding if the given encoding is
            // the default input encoding: otherwise assume we're here recursively,
            // after already guessing at an encoding (otherwise, we'll loop, and blow stack)
            if (DEBUG.IO) System.out.println("XML head [" + firstNonCommentLine + "]");
            
            if (firstNonCommentLine.indexOf("encoding=\"UTF-8\"") > 0) {
                
                // If encoding is UTF-8, this a 2nd generation save file (mapping is
                // versioned, but not all US-ASCII encoding): the actual encoding is
                // unknown: make a guess as how to best handle it.  This is our rule: if
                // we're on the SAME platform as the save file, assuming the local
                // encoding (assume it's the same user, on the same machine, and the
                // current default system encoding is the same one that was active when
                // the file was originally saved).  If we're on a different platform,
                // assume a default encoding for that platform.
                
                if (Util.isWindowsPlatform()) {
                    if (savedOnWindowsPlatform)
                        guessedEncoding = Util.getDefaultPlatformEncoding();
                    else if (savedOnMacPlatform)
                        guessedEncoding = DEFAULT_MAC_ENCODING;
                    else
                        guessedEncoding = DEFAULT_WINDOWS_ENCODING;
                } else if (Util.isMacPlatform()) {
                    if (savedOnMacPlatform)
                        guessedEncoding = Util.getDefaultPlatformEncoding();
                    else if (savedOnWindowsPlatform)
                        guessedEncoding = DEFAULT_WINDOWS_ENCODING;
                    else
                        guessedEncoding = DEFAULT_MAC_ENCODING;
                }
                VUE.Log.info(url + "; assuming " + guessedEncoding + " charset encoding");
                // Note: doing this is a real tradeoff amongst bugs: any old save file that had
                // fancy unicode characters in UTF, such as something in a japanese charset, will
                // be screwed by this, so we optimizing for what we think is the most likely case.
                // if this becomes a real problem, we could introduce a special convert dialog.
                
            }
        } else {
            VUE.Log.warn("Missing XML header in [" + firstNonCommentLine + "]");
        }

        boolean oldFormat = false;

        if (versionID == null && mapping == null) {
            oldFormat = true;
            VUE.Log.info(url + "; save file is of old pre-versioned type.");
            mapping = getMapping(XML_MAPPING_UNVERSIONED);
        }
        
        if (mapping == null)
            mapping = getDefaultMapping();        

        String encoding = guessedEncoding == null ? DEFAULT_INPUT_ENCODING : guessedEncoding;

        return unmarshallMap(url, mapping, encoding, false);
    }


    private static LWMap unmarshallMap(final java.net.URL url, Mapping mapping, String charsetEncoding, boolean oldFormat)
        throws java.io.IOException
    {
        LWMap map = null;

        if (DEBUG.CASTOR || DEBUG.IO) System.out.println("UNMARSHALLING: " + url + " charset=" + charsetEncoding);
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), charsetEncoding));

        // Skip over comments to get to start of XML

        for (;;) {
            reader.mark(2048); // a single comment line can't be longer than this...
            String line = reader.readLine();
            if (line == null) {
                System.err.println("Unexpected end-of-stream in [" + url + "]");
                throw new java.io.IOException("end of stream in " + url);
            }
            if (line.startsWith("<!--") == false) {
                // we should have just hit thie "<?xml ..." line -- done with comments
                break;
            }
            if (DEBUG.CASTOR || DEBUG.IO) System.out.println("Skipping[" + line + "]");
        }

        // Reset the reader to the start of the last line read, which should be the <?xml line,
        // which is what castor needs to see at start (it can't handle ignoring comments...)
        reader.reset();

        final String sourceName = url.toString();

        try {
            Unmarshaller unmarshaller = getDefaultUnmarshaller(mapping, sourceName);

            // unmarshall the map:
            
            try {
                map = (LWMap) unmarshaller.unmarshal(new InputSource(reader));
            } catch (org.exolab.castor.xml.MarshalException me) {
                if (oldFormat && me.getMessage().endsWith("tufts.vue.Resource")) {
                    System.err.println("ActionUtil.unmarshallMap: " + me);
                    System.err.println("Attempting specialized MapResource mapping for old format.");
                    // NOTE: delicate recursion here: won't loop as long as we pass in a non-null mapping.
                    return unmarshallMap(url, getMapping(XML_MAPPING_OLD_RESOURCES), charsetEncoding, true);
                } else
                    throw me;
            }
            
            map.setFile(new File(url.getFile()));// appears as a modification, so be sure to do completeXMLRestore last.
            map.completeXMLRestore();
            reader.close();
        }
        catch (Exception e) 
        {
            System.err.println("ActionUtil.unmarshallMap: url=" + url);
            System.err.println("\texception: " + e.getClass());
            System.err.println("\tcause: " + e.getCause());
            System.err.println("\t" + e);
            //System.err.println("\tmessage=" + e.getLocalizedMessage());
            e.printStackTrace();
            map = null;
        }
        
        return map;
    }

}
