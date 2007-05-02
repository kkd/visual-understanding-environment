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

import java.beans.PropertyChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Helper class for creating an LWPropertyProducer.
 * Handles an action-event, & firing a property change event with
 * after producing the new property value (via getPropertyValue()).
 *
 * Subclass implementors must provide produceValue/displayValue from LWEditor.
 */

// rename LWEditorChangeHandler
public abstract class LWPropertyHandler<T>
    implements LWEditor<T>, java.awt.event.ActionListener
{
    private final Object key;
    private final java.awt.Component[] gui;
    //private final PropertyChangeListener changeListener;
    
    //public LWPropertyHandler(Object propertyKey, PropertyChangeListener listener, java.awt.Component gui) {
    public LWPropertyHandler(Object propertyKey, java.awt.Component... gui) {
        this.key = propertyKey;
        //this.changeListener = listener;
        this.gui = gui;
        VUE.LWToolManager.registerEditor(this);
    }

    /*
    public LWPropertyHandler(Object propertyKey, java.awt.Component gui) {
        this(propertyKey, null, gui);
    }
    */
    public LWPropertyHandler(Object propertyKey) {
        this(propertyKey, (java.awt.Component) null);
        //this(propertyKey, null, null);
    }

    
    public Object getPropertyKey() { return key; }

    public void setEnabled(boolean enabled) {
        if (gui != null) {
            for (java.awt.Component c : gui) {
                c.setEnabled(enabled);
            }
            //gui.setEnabled(enabled);
        } else
            throw new UnsupportedOperationException(this + ": provide a gui component, or subclass should override setEnabled");
    }

    //public void itemStateChanged(java.awt.event.ItemEvent e) {
    public void actionPerformed(java.awt.event.ActionEvent e) {
        if (DEBUG.TOOL) System.out.println(this + " actionPerformed " + e.paramString());
        VUE.LWToolManager.ApplyPropertyChangeToSelection(VUE.getSelection(), key, produceValue(), e.getSource());
        
        /*
        // TODO: we want to skip doing this if we're in the middle of LWEditor.displayValue...
        // this is prob why we don't want this class being the action listener...  so we can
        // handle that all in once place.
        if (DEBUG.TOOL) System.out.println(this + ": " + e);
        if (changeListener == null)
            LWCToolPanel.ApplyPropertyChangeToSelection(VUE.getSelection(), key, produceValue(), e.getSource());
        else
            changeListener.propertyChange(new LWPropertyChangeEvent(e.getSource(), key, produceValue()));
        */
    }

    public String toString() {
        Object value = null;
        try {
            value = produceValue();
        } catch (Throwable t) {
            value = t.toString();
        }
        return getClass().getName() + ":LWPropertyHandler[" + getPropertyKey() + "=" + value + "]";
    }
}
