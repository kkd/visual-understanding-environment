/*
 * LWPathway.java
 *
 * Created on June 18, 2003, 1:37 PM
 *
 * @author  Jay Briedis
 */

package tufts.vue;

import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.geom.Area;
import java.util.ArrayList;

public class LWPathway
    implements Pathway
{
    private LinkedList elementList = null;
    private int weight = 1;
    private String comment = "";
    private boolean ordered = false;
    private Color borderColor = Color.blue;
    private LWMap map = null;
    private String label = "";
    private int currentIndex;
    private String notes = "";
    private boolean showing = true;
    private boolean open = true;
    private boolean locked = false;
    private boolean mDoingXMLRestore = false;
    private ArrayList colorArray = new ArrayList();
    
    /**default constructor used for marshalling*/
    public LWPathway() {
        //added by Daisuke
        elementList = new LinkedList();    
        currentIndex = -1;
        mDoingXMLRestore = true;
        colorArray.add(new Color(255, 255, 51));
        colorArray.add(new Color(255, 102, 51));
        colorArray.add(new Color(204, 51, 204));
        colorArray.add(new Color(51, 204, 204));
        colorArray.add(new Color(51, 204, 51));
        LWPathwayManager manager = VUE.getActiveMap().getPathwayManager();
        if(manager != null && manager.getPathwayList() != null){
            int num = manager.getPathwayList().size();
            borderColor = (Color)colorArray.get(num % 5);
        }
        
        System.out.println("manager: " + manager.toString());
        System.out.println("pathway border color: " + borderColor.toString());
    }
    
    public LWPathway(LWMap map, String label) {
        this(label);
        this.map = map;        
    }
    
    /** Creates a new instance of LWPathway with the specified label */
    public LWPathway(String label) {
        this.setLabel(label);
        elementList = new LinkedList();
        currentIndex = -1;
        
        colorArray.add(new Color(255, 255, 51));
        colorArray.add(new Color(255, 102, 51));
        colorArray.add(new Color(204, 51, 204));
        colorArray.add(new Color(51, 204, 204));
        colorArray.add(new Color(51, 204, 51));
        LWPathwayManager manager = VUE.getActiveMap().getPathwayManager();
        if(manager != null && manager.getPathwayList() != null){
            int num = manager.getPathwayList().size();
            borderColor = (Color)colorArray.get(num % 5);
        }
        
        System.out.println("manager: " + manager.toString());
        System.out.println("pathway border color: " + borderColor.toString());
    }
     
    /** adds an element to the end of the pathway */
    public void addElement(LWComponent element) {
       elementList.add(element);
       element.addPathwayRef(this);
       if (currentIndex == -1) currentIndex = length() - 1;
       //maybe need to repaint the view?
    }
    
    public LWMap getPathwayMap(){
        return map;
    }
    
    
    public void setShowing(boolean showing){
        this.showing = showing;
    }
    
    public void setShowing(){
        if(this.showing)
            this.showing = false;
        else
            this.showing = true;
    }
    
    public boolean getShowing(){
        return showing;
    }
    
    public void setLocked(){
         if(this.locked)
            this.locked = false;
        else
            this.locked = true;
    }
    
    public boolean getLocked(){
        return locked;
    }
    
    public void setOpen(boolean open){
        this.open = open;
    }
    
    public void setOpen(){
        if(this.open)
            this.open = false;
        else
            this.open = true;
    }
    
    public boolean getOpen(){
        return open;
    }
    
    public void drawPathway(Graphics2D g){
        Iterator iter = this.getElementIterator();
        Color oldColor = g. getColor();
        BasicStroke oldStroke = (BasicStroke)g.getStroke();
        float width = oldStroke.getLineWidth();
        BasicStroke currentStroke = new BasicStroke(width*4);
        while(iter.hasNext()){
            LWComponent comp = (LWComponent)iter.next();
            g.setColor(borderColor);
            g.setStroke(oldStroke);
            if((LWComponent)elementList.get(this.getCurrentIndex()) == comp)
                g.setStroke(currentStroke);
            if(comp instanceof LWNode)
                g.draw(comp.getShape());      
            else if(comp instanceof LWLink){
                LWLink link = (LWLink)comp;
                LWComponent ep1 = link.getComponent1();
                LWComponent ep2 = link.getComponent2();
                if (!(ep1 instanceof LWLink && ep2 instanceof LWLink)
                && !(ep1.getShape() == null && ep2.getShape() == null)) {
                Area clipArea = new Area(g.getClipBounds());
                if (!(ep1 instanceof LWLink) && ep1.getShape() != null)
                    clipArea.subtract(new Area(ep1.getShape()));
                if (!(ep2 instanceof LWLink) && ep2.getShape() != null)
                    clipArea.subtract(new Area(ep2.getShape()));
                g.clip(clipArea);
            }
                g.draw(link.getShape());
            }
        }
        g.setColor(oldColor);
    }
    
    public boolean contains(LWComponent element){
        return elementList.contains(element);
    }
    
    public int length() {
        return elementList.size();
    }
    
    public LWComponent getFirst() {        
        LWComponent firstElement = null;
        
        try{
            firstElement = (LWComponent)elementList.getFirst();
            currentIndex = 0;
        }catch(NoSuchElementException ne){
            firstElement = null;
        }        
        
        return firstElement;
    }
    
    public boolean isFirst(){
        return (currentIndex == 0);
    }
    
    public LWComponent getLast() {        
        LWComponent lastElement = null;  
        
        try{
            lastElement = (LWComponent)elementList.getLast();
            currentIndex = length() - 1;
        }catch(NoSuchElementException ne){
            lastElement = null;
        }
        return lastElement;
    }
    
    public boolean isLast(){
        return (currentIndex == (length() - 1));
    }
      
    public LWComponent getPrevious(){
        if (currentIndex > 0)
            return (LWComponent)elementList.get(--currentIndex);        
        else
            return null;
    }
    
    public LWComponent getNext(){
        if (currentIndex < (length() - 1))
            return (LWComponent)elementList.get(++currentIndex);        
        else 
            return null;
    }
    
    public LWComponent getElement(int index){
        LWComponent element = null;
        
        try{
            element = (LWComponent)elementList.get(index);
        }catch (IndexOutOfBoundsException ie){
            element = null;
        }    
        
        return element;
    }
    
    public java.util.Iterator getElementIterator() {
        return elementList.iterator();
    }
    
    public void removeElement(int index) {
        System.out.println("this remove element is called...");
        
        //if the current node needs to be deleted and it isn't the first node, 
        //set the current index to the one before, else keep the same index
        if (index == currentIndex && !isFirst())
            //if (!isFirst())
          currentIndex--;
        
        //if the node to be deleted is before the current node, set the current index to the one before
        else if (index < currentIndex)
          currentIndex--;
        
        LWComponent element = (LWComponent)elementList.remove(index);
        
        if(element == null)
            System.err.println("LWPathway.removeElement: element does not exist in pathway");
    }
       
    public void removeElement(LWComponent element) {
      
       System.out.println("the element version of the remove is being called");
       for(int i = 0; i < elementList.size(); i++){
            LWComponent comp = (LWComponent)elementList.get(i);
            if(comp.equals(element)){
                this.removeElement(i);
                element.removePathwayRef(this);
                //break;
            }
       }
    }

    public void moveElement(int oldIndex, int newIndex) {
        throw new UnsupportedOperationException("LWPathway.moveElement");
        // will need to clean up add/remove element code at bottom
        // and track addPathRefs before can put this back in
        /*
        LWComponent element = getElement(oldIndex);
        removeElement(oldIndex);
        addElement(element, newIndex);
        */
    }
    
    /**accessor methods used also by xml marshalling process*/
    public Color getBorderColor(){
        return borderColor;
    }
    
    public void setBorderColor(Color color){
        this.borderColor = color;
    }
    
    public int getWeight() {
        return weight;
    }
    
    public void setWeight(int weight) {
        this.weight = weight;
    }
    
    public boolean getOrdered() {
        return ordered;
    }
    
    public void setOrdered(boolean ordered) {
        this.ordered = ordered;
    }
    
    public java.util.List getElementList() {
        //System.out.println(this + " getElementList type  ="+elementList.getClass().getName()+"  size="+elementList.size());
        return elementList;
    }
    
    public void setElementList(java.util.List elementList) {
        this.elementList = (LinkedList)elementList;
        if (elementList.size() >= 1) currentIndex = 0;
        System.out.println(this + " setElementList type  ="+elementList.getClass().getName()+"  size="+elementList.size());
    }

    private List idList = new ArrayList();
    /** for XML save/restore only */
    public List getElementIDList() {
        if (mDoingXMLRestore) {
            return idList;
        } else {
            idList.clear();
            Iterator i = getElementIterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                idList.add(c.getID());
            }
        }
        System.out.println(this + " getElementIDList: " + idList);
        return idList;
    }


    /*
    public void setElementIDList(List idList) {
        System.out.println(this + " setElementIDList: " + idList);
        this.idList = idList;
    }
    */
    
    void completeXMLRestore(LWMap map)
    {
        System.out.println(this + " completeXMLRestore, map=" + map);
        this.map = map;
        Iterator i = this.idList.iterator();
        while (i.hasNext()) {
            String id = (String) i.next();
            LWComponent c = this.map.findChildByID(id);
            System.out.println("\tpath adding " + c);
            addElement(c);
        }
        mDoingXMLRestore = false;
    }
    
    /** Interface for the linked list used by the Castor mapping file*/
    /**
    public ArrayList getElementArrayList()
    {
        System.out.println("calling get elementarraylist for " + getLabel());
         return new ArrayList(elementList);
    }
    
    public void setElementArrayList(ArrayList list)
    {
        System.out.println("calling set elementarraylist for " + getLabel());
        elementList = new LinkedList(list);
    }
    **/
    public void setElementArrayList(LWComponent component)
    {
        System.out.println("calling set elementarraylist for " + getLabel());
        elementList.add(component);
    }
    
    /** end of Castor Interface */
    
    public LWComponent getCurrent() { 
        LWComponent element = null;        
        try{
            element = (LWComponent)elementList.get(currentIndex);
        }catch (IndexOutOfBoundsException ie){
            element = null;
        }      
        return element;
    }
    
    public String getComment(){
        return comment;
    }
    
    public void setComment(String comment){
        this.comment = comment;
    }
    
    public void setCurrentIndex(int i){
        System.out.println("Current pathway node is now " + i);
        currentIndex = i;
        VUE.getActiveMap().notify(this, LWCEvent.Repaint);
    }
    
    public int getCurrentIndex(){
        return currentIndex;
    }
    
    public int getElementIndex(LWComponent comp){
        Iterator iter = this.elementList.iterator();
        int index = 0;
        while(iter.hasNext()){
            LWComponent c = (LWComponent)iter.next();
            if(c.equals(comp)){
                return index;
            }
            index++;
        }
        return -1;
    }
    
    public String getLabel() {
        return this.label;
    }
    
    public String getNotes() {
        return this.notes;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }


    public String toString()
    {
        return "LWPathway[" + label
            + " n="
            + (elementList==null?-1:elementList.size())
            + " idx="+currentIndex
            + " map=" + (map==null?"null":map.getLabel())
            + "]";
    }

    
/*****************************/    
/**methods below are not used (actually: moveElement uses them -- SMF) */    
/*****************************/
    
    /** adds an element at the specified location within the pathway*/
    /*
    public void addElement(LWComponent element, int index){
        if(elementList.size() >= index){
            elementList.add(index, element);
            //if(current == null) setCurrent(element);
            if (currentIndex == -1) currentIndex = index;
        }else{
            System.out.println("LWPathway.addElement(element, index), index out of bounds");
        }
    }
    */
    /** adds an element in between two other elements, if they are adjacent*/
    /*
    public void addElement(LWComponent element, LWComponent adj1, LWComponent adj2){
        int index1 = elementList.indexOf(adj1);
        int index2 = elementList.indexOf(adj2);
        int dif = index1 - index2;
        if(elementList.size() >= index1 && elementList.size() >= index2){
            if(Math.abs(dif) == 1){
                if(dif == -1)
                    elementList.add(index2, element);
                else
                    elementList.add(index1, element);
            }
        }else{
            System.out.println("LWPathway.addElement(element,adj1,adj2), index out of bounds");
        }
    }
    
    public LWComponent getNext(LWComponent current) {
        int index = elementList.indexOf(current);
        
        if (index >= 0 && index < (length() - 1))
          return (LWComponent)elementList.get(++index);
        
        //if (currentIndex >= 0 && currentIndex < (length() - 1))
          //return (LWComponent)elementList.get(currentIndex + 1);
        else
          return null;
    }
    
    public LWComponent getPrevious(LWComponent current) {
        int index = elementList.indexOf(current);
        
        if (index > 0)
          return (LWComponent)elementList.get(--index);
        
        //if (currentIndex > 0)
          //return (LWComponent)elementList.get(currentIndex - 1);
        else
          return null;
        
    }
    */
}
