/*
 * VueButton.java
 *
 * Created on February 13, 2004, 10:15 AM
 *
 * This claas is a wrapper around JButton to get the look and feel for VUE.
 *VueButtons are currently used in Pathway Panel and Advanced Search.  The button sets the disabled, Up and Down icons.
 * All the icons must be present in VueResources in format buttonName.Up, buttonName.down, buttonName.disabled.
 *
 */

package tufts.vue;

/**
 *
 * @author  akumar03
 */

import javax.swing.*;
import java.awt.*;

public class VueButton extends JButton {
    
    /** Creates a new instance of VueButton */
    public static String UP = "up";
    public static String DOWN = "down";
    public static String DISABLED = "disabled";
    public VueButton(String name) {
        super(VueResources.getImageIcon(name+"."+UP));
        setSelectedIcon(VueResources.getImageIcon(name+"."+DOWN));
        setDisabledIcon(VueResources.getImageIcon(name+"."+DISABLED));
        setBorderPainted(false);
        setBackground(Color.white);
        setPreferredSize(new Dimension(17, 17));
        
    }
    
   
}
