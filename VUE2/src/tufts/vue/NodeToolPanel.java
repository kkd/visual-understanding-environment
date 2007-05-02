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

import tufts.vue.gui.*;
import tufts.vue.beans.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.geom.RectangularShape;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.Icon;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JSeparator;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.*;

/**
 * This creates an editor panel for LWNode's
 *
 * @version $Revision: 1.54 $ / $Date: 2007-05-02 23:00:28 $ / $Author: sfraize $
 */
 
public class NodeToolPanel extends ToolPanel
{
    private final ShapeMenuButton mShapeButton;
    private final LinkMenuButton mLinkButton;
    
    public NodeToolPanel() {
        mShapeButton = new ShapeMenuButton();
        mLinkButton = new LinkMenuButton();        
    }
    
    public void buildBox()
    {
         //JLabel label = new JLabel("   Node: ");
         //label.setFont(VueConstants.FONT_SMALL);
         GridBagConstraints gbc = new GridBagConstraints();
     	    
        gbc.gridx = 0;
 		gbc.gridy = 0;    		
 		gbc.gridwidth = 1;
 		gbc.gridheight=1;
 		gbc.insets= new Insets(0,3,0,0);
 		gbc.fill = GridBagConstraints.VERTICAL; // the label never grows
 		gbc.anchor = GridBagConstraints.EAST;
 		
 		JLabel shapeLabel = new JLabel("Shape: ");
                shapeLabel.setLabelFor(mShapeButton);
 		shapeLabel.setForeground(new Color(51,51,51));
 		shapeLabel.setFont(tufts.vue.VueConstants.SmallFont);
 		getBox().add(shapeLabel,gbc);
         
        gbc.gridx = 0;
 		gbc.gridy = 1;    		
 		gbc.gridwidth = 1; // next-to-last in row
 		gbc.gridheight=1;
 		gbc.fill = GridBagConstraints.VERTICAL; // the label never grows
 		gbc.anchor = GridBagConstraints.EAST;
 		JLabel strokeLabel = new JLabel("Line: ");
 		strokeLabel.setLabelFor(mLinkButton);
 		strokeLabel.setForeground(new Color(51,51,51));
 		strokeLabel.setFont(tufts.vue.VueConstants.SmallFont);
 		getBox().add(strokeLabel,gbc);
         
     	gbc.gridx = 1;
 		gbc.gridy = 0;    				
 		gbc.fill = GridBagConstraints.NONE; // the label never grows
 		gbc.insets = new Insets(1,1,1,5);
 		gbc.anchor = GridBagConstraints.WEST;
         getBox().add(mShapeButton, gbc);
         
         gbc.gridx = 1;
  		gbc.gridy = 1;    				
  		gbc.fill = GridBagConstraints.BOTH; // the label never grows
  		gbc.insets = new Insets(1,1,1,5);
  		gbc.anchor = GridBagConstraints.WEST;
         getBox().add(mLinkButton,gbc);
         
         //mShapeButton.addPropertyChangeListener(this);
         //mLinkButton.addPropertyChangeListener(this);
         //addEditor(mLinkButton);
         
         //add(mShapeButton);
         //getBox().add(label, 0);
         //getBox().add(label, 0);
    }
    public boolean isPreferredType(Object o) {
        return o instanceof LWNode;
    }
    
    static class ShapeMenuButton extends VueComboMenu<Class<? extends RectangularShape>>
    {
        public ShapeMenuButton() {
            super(LWKey.Shape, NodeTool.getTool().getAllShapeClasses());
            setToolTipText("Node Shape");
            setRenderer(new ShapeComboRenderer());
            this.setMaximumRowCount(10);
        }

        protected Icon makeIcon(Class<? extends RectangularShape> shapeClass) {
            try {
                return new NodeTool.SubTool.ShapeIcon(shapeClass.newInstance());
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t);
            }
            return null;
        }

        class ShapeComboRenderer extends JLabel implements ListCellRenderer {
        	
            public ShapeComboRenderer() {
                setOpaque(true);
                setHorizontalAlignment(CENTER);
                setVerticalAlignment(CENTER);
                setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
            }

        
            public Component getListCellRendererComponent(
                                                          JList list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus)
            {
                if (isSelected) {
                    setBackground(list.getSelectionBackground());
                    setForeground(list.getSelectionForeground());
                } else {
                    setBackground(Color.white);
                    setForeground(list.getForeground());
                }
                
                //setEnabled(ShapeMenuButton.this.isEnabled());
                // the combo box will NOT repaint our icon when it becomes disabled!
                // Tho this works fine for the image-icons in the LinkMenuButton below!
                
                Icon icon = getIconForValue(value);
                setIcon(icon);
                if (DEBUG.TOOL && !isEnabled())
                    System.out.println("RENDERER SET DISABLED ICON: " + icon + " for value " + value);
                //setIcon(getIconForValue(value));
                if (DEBUG.TOOL && DEBUG.META) setText(value.toString());

                return this;
            }
        }        
	 
    }
    
    static class LinkMenuButton extends VueComboMenu<Integer>
    {
        private final Action[] actionsWithIcons;
        
        public LinkMenuButton() {
            //super(LWKey.LinkShape, LinkTool.getTool().getSetterActions());
            super(LWKey.LinkShape, new Integer[] { 0, 1, 2 });
            actionsWithIcons = LinkTool.getTool().getSetterActions();
            setToolTipText("Link Shape");
            setRenderer(new LinkComboRenderer());
            this.setMaximumRowCount(10);
        }

        protected Icon getIconForValue(Object i) {
            return (Icon) actionsWithIcons[((Integer)i)].getValue(Action.SMALL_ICON);
        }

        class LinkComboRenderer extends JLabel implements ListCellRenderer {
        	
            public LinkComboRenderer() {
                setOpaque(true);
                setHorizontalAlignment(CENTER);
                setVerticalAlignment(CENTER);
                setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
            }

        
            public Component getListCellRendererComponent(
                                                          JList list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
        		
                if (isSelected) {
                    setBackground(list.getSelectionBackground());
                    setForeground(list.getSelectionForeground());
                } else {
                    setBackground(Color.white);
                    setForeground(list.getForeground());
                }        	         		
        		
                setEnabled(LinkMenuButton.this.isEnabled());
                setIcon(getIconForValue(value));
                //setText(value.toString());
                return this;
            }
        }        
	 
    }
  

    
    public static void main(String[] args) {
        System.out.println("NodeToolPanel:main");
        VUE.init(args);
        LWCToolPanel.debug = true;
        VueUtil.displayComponent(new NodeToolPanel());
    }
}
