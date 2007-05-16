package tufts.vue;

public class VueToolUtils 
{
	private static boolean sDebug = false;
	 /**
     * This method loads a VueTool with the given name from the
     * vue properties file.
     **/
    public static VueTool loadTool(String pName)
    {
        final String classKey = pName + ".class";
        final String className = VueResources.getString(classKey);

        if (className == null) {
            System.err.println(VueToolUtils.class.getName() + " loadTool["
                               + pName + "]; missing class key in resources: [" + classKey + "]");
            return null;
        }
		
        VueTool tool = null;
        
        try {
            //if (DEBUG.Enabled) System.out.println("Loading tool class " + className);
            Class toolClass = ClassLoader.getSystemClassLoader().loadClass(className);

            if (DEBUG.INIT) System.out.println("Loading tool " + pName + " " + toolClass);
            
           	tool = (VueTool) toolClass.newInstance();            
        	setToolProperties(tool,pName);
            // set the tool's properties...
        } catch (Exception e) {
            debug("loadTool() exception:");
            e.printStackTrace();
        }
    	        
        return tool;
    }
    
    private static VueTool loadToolFromMap(String pName)
    {
        final String classKey = pName + ".class";
        final String className = VueResources.getString(classKey);

        if (className == null) {
            System.err.println(VueToolUtils.class.getName() + " loadTool["
                               + pName + "]; missing class key in resources: [" + classKey + "]");
            return null;
        }
		
        VueTool tool = null;
        Class toolClass = null;
        try {
            //if (DEBUG.Enabled) System.out.println("Loading tool class " + className);
             toolClass = ClassLoader.getSystemClassLoader().loadClass(className);

        } catch (Exception e) {
            debug("loadTool() exception:");
            e.printStackTrace();
        }
        
        return VueTool.getInstance(toolClass);
    	
    }
    
    public synchronized static VueTool[] loadToolsFromMap(String DefaultToolsKey)
    {
    	String[] names = VueResources.getStringArray(DefaultToolsKey);
    	VueTool[] mVueTools = null;
    	int toolCount = getToolCount(names);
    	
    	if( names != null) {
            mVueTools = new VueTool[toolCount];
            int count=0;
            for (int i = 0; i < names.length; i++) {
            	
            	
                debug("Loading tool " + names[i] );
                
                if (!names[i].equals("separator"))
                {
//                	if (names[i].contains("."))                		
                		mVueTools[count++] = VueToolUtils.loadToolFromMap(names[i]);
                	
                }                
            }
    	}
    	
    	return mVueTools;
    }
    private static int getToolCount(String[] names)
    {
    	 //you can specify the separators for the toolbar in the props so you need to remove them from
        //the count here.
        int toolCount = 0;
        
        for (int i=0;i< names.length;i++)
        {
        	if (!names[i].equals("separator"))
        		toolCount++;
        }
        
        return toolCount;
        
    }
    
    public static int[] getSeparatorPositions(String DefaultToolsKey)
    {
    	String[] names = VueResources.getStringArray(DefaultToolsKey);
    	
    	int separatorCount = names.length - getToolCount(names);
    	int[] separatorPositions = new int[separatorCount];
    	
    	separatorCount=0;
    	for (int i=0;i < names.length;i++)
    	{
    		if (names[i].equals("separator"))
    			separatorPositions[separatorCount++] = i;
    	}
    	return separatorPositions;
    }
    
    /**
     * Loads the default tools specified in a resource file
     **/
    public synchronized static VueTool[] loadTools(String DefaultToolsKey)
    {
        String[] names = VueResources.getStringArray(DefaultToolsKey);
        VueTool[] mVueTools = null;
        
        int toolCount = getToolCount(names);
        
       
        if( names != null) {
            mVueTools = new VueTool[toolCount];
            int count=0;
            for (int i = 0; i < names.length; i++) {
            	
            	
                debug("Loading tool " + names[i] );
                
                if (!names[i].equals("separator"))
                {
//                	if (names[i].contains("."))                		
                		mVueTools[count++] = VueToolUtils.loadTool(names[i]);
                	
                }
            }
        }

        return mVueTools;
    }
    
    public static void setToolProperties(VueTool tool, String pName)
    {

        tool.setID(pName);
        tool.initFromResources();       

        String subtools[];
        String defaultTool = null;
        subtools = VueResources.getStringArray( pName+".subtools");
        if (subtools != null) {
			
            tool.setOverlayUpIcon( VueResources.getImageIcon( pName+".overlay"));
            tool.setOverlayDownIcon( VueResources.getImageIcon( pName+".overlaydown") );
		
            for(int i=0; i<subtools.length; i++) {
                VueTool subTool = loadTool(pName+"."+subtools[i]); // recursion...
                subTool.setParentTool( tool);
                tool.addSubTool(subTool);
            }
            // load menu overlays (if any)
			
            // setup default tool (if any)
            defaultTool = VueResources.getString( pName+".defaultsubtool");
            if( defaultTool == null) {
                defaultTool = subtools[0];
            }
			
            VueTool dst = tool.getSubTool( pName + "."+defaultTool );
            if( dst != null) {
                tool.setIcon( dst.getIcon() );
                tool.setDownIcon( dst.getDownIcon() );
                tool.setSelectedIcon( dst.getSelectedIcon() );
                tool.setRolloverIcon( dst.getRolloverIcon() );
                tool.setDisabledIcon( dst.getDisabledIcon() );
                tool.setSelectedSubTool( dst);
            }
            else {
                // should never happen unless bad properties file
                debug("  !!! Error: missing subtool: "+defaultTool );
				
            }
            // tool.set
        }
    	

    }
    
    private static void debug(String pMsg) {
        if( sDebug )
            System.out.println( pMsg);
    }
}
