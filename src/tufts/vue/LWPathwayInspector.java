/*
 * LWPathwayInspector.java
 *
 * Created on June 23, 2003, 3:54 PM
 */

package tufts.vue;

import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.ListSelectionModel;
import javax.swing.border.LineBorder;
import javax.swing.text.Document;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.awt.*;
import javax.swing.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.util.Vector;

/**
 *
 * @author  Jay Briedis
 */
public class LWPathwayInspector extends InspectorWindow
{
    
    /**Pane holds three tabs: general info, nodes in path, pathway notes*/
    private static JTabbedPane pane = null;
    
    /**'info' holds the pathway's general info*/
    private JTable info = null, pathwayTable = null;
    
    /**current pathway as indicated by the manager*/
    private LWPathway pathway = null;
    private LWMap map = null;
    
    /**third tab which holds the notes for the current pathway*/
    public JTextArea text = null;
    
    /** buttons to navigate between pathway nodes*/
    private JButton moveUp, moveDown, remove, submit;
    
    /**standard border for all tabs*/
    private LineBorder border = new LineBorder(Color.black);
    
    public InfoTableModel model = null;
    private Notes notes = null;
    private PathwayTab pathwayTab = null;
    //private Info info = null;
    
    public LWPathwayInspector(JFrame owner, LWPathway pathway){
        this(owner);
        this.setPathway(pathway);
    }
 
    public LWPathwayInspector(JFrame owner) {
        super(owner, "");
        //this.setResizable(true);//tool-window?
        
        InfoTable table = new InfoTable();
        notes = new Notes();
        pathwayTab = new PathwayTab(this);
        Info info = new Info();
        info.setInfo(this.getPathway());
        
        this.setTitle("Pathway Inspector");
        
        pane = new JTabbedPane();
        
        pane.addTab("Info", null, info, "Info Tab");
        pane.addTab("Notes", null, notes, "Notes Tab");
        pane.addTab("Pathways", null, pathwayTab, "Pathways Tab");
        pane.addTab("Filters", null, new JLabel("Filters..."), "Filters Tab");
        
        /**adding pane and setting location of this stand alone window*/
        this.getContentPane().add(pane);
        this.setSize(350, 450);
        /*unselects checkbox in VUE window menu on closing*/
        /*
        super.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e) {setButton(false);}});
        */
    }
    
    public PathwayTab getPathwayTab(){
        return this.pathwayTab;
    }
    
    public void setPathwayManager(LWPathwayManager pathwayManager){
        pathwayTab.setPathwayManager(pathwayManager);
    }
    
    public LWPathway getCurrentPathway(){
        return pathwayTab.getPathwayTableModel().getCurrentPathway();
    }
    
    public LWPathway getPathway(){
        return pathway;
    }
    
    public void setPathway(LWPathway pathway){
        this.pathway = pathway;
        //if(pathway!=null) map = pathway.getPathwayMap();
        //if(pathway != null)
        //    setTitle("PATHWAY INSPECTOR: " + pathway.getLabel());
        //else
        //    setTitle("PATHWAY INSPECTOR");
        
        //pathwayTab.setPathway(pathway);
        //notes.setNotes();        
        //model.fireTableDataChanged();        
    }
    
    public void notifyPathwayTab(){
        pathwayTab.updateTable();
        pathwayTab.updateControlPanel();
    }
    
     /*protected void getField(String name,
                               GridBagLayout gridbag,
                               GridBagConstraints c,
                               JPanel infoPanel) {
         JTextField field = new JTextField("");
         gridbag.setConstraints(field, c);
         infoPanel.add(field);
     }*/
     
     private class Info extends JPanel
    {
        private JTextField titleField = new JTextField("");
        private JTextField authorField = new JTextField("");
        
        public Info()
        {
             
             GridBagLayout gridbag = new GridBagLayout();
             GridBagConstraints c = new GridBagConstraints();
             
             setLayout(gridbag);

             c.fill = GridBagConstraints.HORIZONTAL;
             c.weightx = 1.0;
             c.insets = new Insets(10, 5, 5, 10);
             c.gridwidth = GridBagConstraints.REMAINDER; //end row
             JLabel label = new JLabel();
             gridbag.setConstraints(label, c);
             add(label);
             c.weightx = 0.0;		   

             c.gridwidth = GridBagConstraints.RELATIVE; 
             getLabel("Title", gridbag, c, this);

             c.gridwidth = GridBagConstraints.REMAINDER; 
             gridbag.setConstraints(titleField, c);
             add(titleField);

             c.gridwidth = GridBagConstraints.RELATIVE; 
             getLabel("Author", gridbag, c, this);

             c.gridwidth = GridBagConstraints.REMAINDER; 
             gridbag.setConstraints(authorField, c);
             add(authorField);

             c.gridwidth = GridBagConstraints.RELATIVE; 
             getLabel("Date", gridbag, c, this);

             c.gridwidth = GridBagConstraints.REMAINDER; 
             getLabel("", gridbag, c, this);

             c.gridwidth = GridBagConstraints.RELATIVE; 
             getLabel("Location", gridbag, c, this);

             c.gridwidth = GridBagConstraints.REMAINDER; 
             getLabel("", gridbag, c, this); 
        }
        
        public void setInfo(LWPathway pathway){
              if(pathway != null){
                  titleField.setText(pathway.getLabel());
                authorField.setText("Current User");
              }  
        }
        
        public void getLabel(String name,
                               GridBagLayout gridbag,
                               GridBagConstraints c,
                               JPanel infoPanel) {
             JLabel label = new JLabel(name);
             label.setAlignmentY(JLabel.CENTER_ALIGNMENT);
             gridbag.setConstraints(label, c);
             infoPanel.add(label);
        }
     
    }
     
     public void getLabel(String name,
                               GridBagLayout gridbag,
                               GridBagConstraints c,
                               JPanel infoPanel) {
             JLabel label = new JLabel(name);
             label.setAlignmentY(JLabel.CENTER_ALIGNMENT);
             gridbag.setConstraints(label, c);
             infoPanel.add(label);
    }
    
    public JPanel getNotesPanel(){
        JPanel notesPanel = new JPanel();
        JTextArea text = new JTextArea();
        JPanel filler = new JPanel();
        filler.setLayout(new GridLayout(1,4));
        
        JLabel label1 = new JLabel(" ");
        JLabel label2 = new JLabel(" ");
        JLabel label3 = new JLabel(" ");
        JButton saveButton = new JButton("Save");
        
        filler.add(label1);
        filler.add(label2);
        filler.add(label3);
        filler.add(saveButton);
        
        notesPanel.setLayout(new BorderLayout(5,5));
        notesPanel.add(new JLabel("Notes: "), BorderLayout.NORTH);
        notesPanel.add(text, BorderLayout.CENTER);
        notesPanel.add(filler, BorderLayout.SOUTH);
       
        return notesPanel;
    }
    
    private class Notes extends JPanel
    {
        private JTextArea area = null;
        
        public Notes ()
        {
            //JPanel notesPanel = new JPanel();
            //JTextArea text = new JTextArea();
            JPanel filler = new JPanel();
            filler.setLayout(new GridLayout(1,4));

            JLabel label1 = new JLabel(" ");
            JLabel label2 = new JLabel(" ");
            JLabel label3 = new JLabel(" ");
            JButton saveButton = new JButton("Save");

            filler.add(label1);
            filler.add(label2);
            filler.add(label3);
            filler.add(saveButton);

            setLayout(new BorderLayout(5,5));
            add(new JLabel("Notes: "), BorderLayout.NORTH);
            area = new JTextArea();
            area.setWrapStyleWord(true);
            area.setLineWrap(true);
            area.addKeyListener(new KeyAdapter()
                {
                    public void keyTyped(KeyEvent e)
                    {
                        //notesPathway.setComment(area.getText());
                        pathway.setComment(area.getText());
                    }
                }); 
            
            add(area, BorderLayout.CENTER);
            add(filler, BorderLayout.SOUTH);

            //setLayout(new BorderLayout());
            //setPreferredSize(new Dimension(315, 200));
            //setBorder(border);    
            
                           
            //JLabel north = new JLabel("Pathway Notes", JLabel.CENTER);
        
            //add(north, BorderLayout.NORTH);
            //add(area, BorderLayout.CENTER);    
        }
        
        public void setNotes()
        {
            if (pathway == null){
                area.setEnabled(false);
                area.setText(null);
            }           
            else{
                area.setEnabled(true);
                area.setText(pathway.getComment());
            }     
        }
    }
    
    /**class to create a new general info table*/
    class InfoTable extends JTable{
           public InfoTable(){
                
               /**sets up model to handle changes in pathway data*/
               model = new InfoTableModel();
               this.setModel(model);
               model.addTableModelListener(this);
               
               this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
               this.setCellSelectionEnabled(true);
               this.setBorder(border);
               this.addMouseListener(new MouseAdapter(){
                   public void mouseClicked(MouseEvent e){
                        int row = getSelectedRow();
                        int col = getSelectedColumn();
                        if(row==3 && col==1 && pathway!=null){
                            JColorChooser choose = new JColorChooser();
                            Color newColor = choose.showDialog((Component)null, 
                                "Choose Pathway Color", 
                                (Color)model.getValueAt(row, col));
                            if(newColor != null)
                                model.setValueAt(newColor, row, col);           
                        }
                   }
                });
               
               
               TableColumn col = this.getColumn(this.getColumnName(1));
               InfoRenderer rend = new InfoRenderer();
               col.setCellRenderer(rend);
           }
    }
    
    /**renderers each of the cells according to location*/
    class InfoRenderer implements TableCellRenderer {       
        public Component getTableCellRendererComponent(
            JTable table, Object val,
            boolean isSelected, boolean focus,
            int row, int col){  
            if(row <= 2){
                JTextField field = new JTextField((String)val);
                field.setBorder(null);
                return field;
            }    
            else if(row == 3){
                JPanel panel = new JPanel();
                panel.setBackground((Color)val);
                return panel;
            }
            return null;
        }
    }
    
    /**model for info table to handle data from pathway instance*/
    class InfoTableModel extends AbstractTableModel 
    {
        int columns = 2;
        final String[] rowNames = {"Title", "Length", "Weight", "Color"};
       
        public int getRowCount() { return rowNames.length; }
        public int getColumnCount() {return columns;}

        public Object getValueAt(int row, int col) {
            
            if(col == 0) return rowNames[row];
            else if(col == 1 && pathway != null){
                if(row == 0){
                    return pathway.getLabel();
                }
                else if(row == 1){
                    return Integer.toString(pathway.length());
                }
                else if(row == 2){
                    return Integer.toString(pathway.getWeight());
                }
                else if(row == 3){
                    return pathway.getBorderColor();
                }
            }
            return null;
        }

        public boolean isCellEditable(int row, int col){
            if(pathway == null) return false;
            if(col==0 || row==3 || row==1) return false;
            return true;
        }

        public void setValueAt(Object value, int row, int col) {
            if(col == 1){
                if(row == 0){
                    pathway.setLabel((String)value);
                    setTitle("Pathway Inspector: " + pathway.getLabel());
                    VUE.getPathwayInspector().repaint();
                } 
                //can't set the length
                else if(row == 2){
                    pathway.setWeight(Integer.parseInt((String)value));
                }
                else if(row == 3){
                    pathway.setBorderColor((Color)value);
                }
                
                if((row == 0 || row == 3) && map != null)
                    map.draw((Graphics2D)getGraphics());
                    //pathway.getPathwayMap().draw((Graphics2D)getGraphics());
            }
        }

        public String getColumnName(int i){
            if(i == 0) return "Property";
            else if (i == 1) return "Value";
            else return null;
        }
    }
    
   
}
