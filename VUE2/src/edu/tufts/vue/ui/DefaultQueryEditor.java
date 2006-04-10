package edu.tufts.vue.ui;

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
 * <p>The entire file consists of original code.  Copyright &copy; 2006 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

public class DefaultQueryEditor
extends javax.swing.JPanel
implements edu.tufts.vue.fsm.QueryEditor, java.awt.event.ActionListener
{
	private edu.tufts.vue.fsm.FederatedSearchManager fsm = edu.tufts.vue.fsm.impl.VueFederatedSearchManager.getInstance();
	private edu.tufts.vue.fsm.SourcesAndTypesManager sourcesAndTypesManager = edu.tufts.vue.fsm.impl.VueSourcesAndTypesManager.getInstance();
	
	private java.awt.GridBagLayout gbLayout = new java.awt.GridBagLayout();
	private java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();
	
	private javax.swing.JTextField field = new javax.swing.JTextField(15);
	private java.io.Serializable criteria = null;
	private org.osid.shared.Properties searchProperties = null;
	
	protected javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();
	
	private org.osid.repository.Repository[] repositories;
	
	private javax.swing.JButton searchButton = new javax.swing.JButton("Search");
	private javax.swing.JButton addButton = new javax.swing.JButton("Add");
	private static final String SELECT_A_LIBRARY = "Select a library";
	private static final String NO_MESSAGE = "";
	private javax.swing.JLabel selectMessage = new javax.swing.JLabel(SELECT_A_LIBRARY);
	
	private javax.swing.JButton moreOptionsButton = new tufts.vue.gui.VueButton("advancedSearchMore");
	private static final String MORE_OPTIONS = "";
	private javax.swing.JLabel moreOptionsLabel = new javax.swing.JLabel(MORE_OPTIONS);

	private javax.swing.JButton fewerOptionsButton = new tufts.vue.gui.VueButton("advancedSearchLess");
	private static final String FEWER_OPTIONS = "";
	private javax.swing.JLabel fewerOptionsLabel = new javax.swing.JLabel(FEWER_OPTIONS);
	
	private javax.swing.JPanel moreFewerPanel = new javax.swing.JPanel();
	
	private static final int NOTHING_SELECTED = 0;
	private static final int BASIC = 1;
	private static final int ADVANCED_INTERSECTION = 2;
	private static final int ADVANCED_UNION = 3;
	private int currentStyle = BASIC;
	
	private javax.swing.JTextField[] advancedFields = null;
	
	//private javax.swing.JPanel panel = new javax.swing.JPanel();
	
	// advanced search universe of types
	private java.util.Vector advancedSearchUniverseOfTypeStringsVector = new java.util.Vector();
	private java.util.Vector advancedSearchPromptsVector = new java.util.Vector();
	
	public DefaultQueryEditor() {
		try {
			gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
			gbConstraints.insets = new java.awt.Insets(2,2,2,2);
			gbConstraints.weighty = 0;
			gbConstraints.ipadx = 0;
			gbConstraints.ipady = 0;
			
			setLayout(gbLayout);
	
			setSize(new java.awt.Dimension(100,100));
			setPreferredSize(new java.awt.Dimension(100,100));
			setMinimumSize(new java.awt.Dimension(100,100));
			
			searchButton.addActionListener(this);
			moreOptionsButton.addActionListener(this);
			fewerOptionsButton.addActionListener(this);
			searchProperties = new edu.tufts.vue.util.SharedProperties();		

			populateAdvancedSearchUniverseOfTypeStringsVector();
			repositories = sourcesAndTypesManager.getRepositoriesToSearch();
			if (repositories.length == 0) {
				makePanel(NOTHING_SELECTED);
			} else {
				makePanel(BASIC);
			}
			//add(new javax.swing.JScrollPane(this.panel));
		} catch (Throwable t) {
		}
	}
	
	public void refresh()
	{
		repositories = sourcesAndTypesManager.getRepositoriesToSearch();
		if (repositories.length == 0) {
			makePanel(NOTHING_SELECTED);
		} else {
			makePanel(this.currentStyle);
		}
	}
	
	private void makePanel(int kind)
	{
		this.removeAll();
		switch (kind) {
			case NOTHING_SELECTED:
				makeNothingSelectedPanel();
				break;
			case BASIC:
				makeBasicPanel();
				break;
			case ADVANCED_INTERSECTION:
				makeAdvancedIntersectionPanel();
				break;
			case ADVANCED_UNION:
				makeAdvancedUnionPanel();
				break;
		}
		this.repaint();
		this.validate();
	}
	
	private void makeNothingSelectedPanel()
	{
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 0;
		add(new javax.swing.JLabel("Keyword:"),gbConstraints);
		
		gbConstraints.gridx = 1;
		gbConstraints.gridy = 0;
		gbConstraints.fill = java.awt.GridBagConstraints.NONE;
		gbConstraints.weightx = 0;
		add(field,gbConstraints);
		field.addActionListener(this);
		
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 1;
		gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbConstraints.weightx = 1;
		add(selectMessage,gbConstraints);
		
		gbConstraints.gridx = 1;
		gbConstraints.gridy = 1;
		add(searchButton,gbConstraints);
		searchButton.setEnabled(false);
	}
	
	private void makeBasicPanel()
	{
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 0;
		gbConstraints.fill = java.awt.GridBagConstraints.NONE;
		gbConstraints.weightx = 0;
		add(new javax.swing.JLabel("Keyword:"),gbConstraints);
		
		gbConstraints.gridx = 1;
		gbConstraints.gridy = 0;
		gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbConstraints.weightx = 1;
		add(field,gbConstraints);
		field.addActionListener(this);
		
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 1;
		add(moreOptionsButton,gbConstraints);

		gbConstraints.gridx = 1;
		gbConstraints.gridy = 1;
		gbConstraints.weightx = 1;
		add(searchButton,gbConstraints);
		searchButton.setEnabled(true);
		this.currentStyle = BASIC;
	}
	
	private void makeAdvancedIntersectionPanel()
	{
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 0;

		java.util.Vector typesVector = getIntersectionSearchFields();
		java.util.Collections.sort(typesVector);
		int size = typesVector.size();
		
		if (size == 0) {
			// no Dublin Core Types found
			add(new javax.swing.JLabel("No Common Dublin Core OSID Types Found"),gbConstraints);
			gbConstraints.gridy++;
			searchButton.setEnabled(false);
		} else {
			this.advancedFields = new javax.swing.JTextField[size];
			
			for (int i=0; i < size; i++) {
				gbConstraints.fill = java.awt.GridBagConstraints.NONE;
				gbConstraints.weightx = 0;
				add(new javax.swing.JLabel((String)typesVector.elementAt(i)),gbConstraints);
				gbConstraints.gridx = 1;
				gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
				gbConstraints.weightx = 1;
				advancedFields[i] = new javax.swing.JTextField(8);
				add(advancedFields[i],gbConstraints);
				gbConstraints.gridx = 0;
				gbConstraints.gridy++;
			}
			searchButton.setEnabled(true);
		}
		
		gbConstraints.gridx = 0;
		add(fewerOptionsButton,gbConstraints);
		
		gbConstraints.gridy++;
		add(moreOptionsButton,gbConstraints);
		
		gbConstraints.gridx = 1;
		add(searchButton,gbConstraints);
		gbConstraints.weightx = 1;
		this.currentStyle = ADVANCED_INTERSECTION;
	}
	
	private void makeAdvancedUnionPanel()
	{
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 0;
		
		java.util.Vector typesVector = getUnionSearchFields();
		java.util.Collections.sort(typesVector);
		int size = typesVector.size();
		
		if (size == 0) {
			// no Dublin Core Types found
			add(new javax.swing.JLabel("No Dublin Core OSID Types Found"),gbConstraints);
			gbConstraints.gridy++;
			searchButton.setEnabled(false);
		} else {
			this.advancedFields = new javax.swing.JTextField[size];
			
			for (int i=0; i < size; i++) {
				gbConstraints.fill = java.awt.GridBagConstraints.NONE;
				gbConstraints.weightx = 0;
				add(new javax.swing.JLabel((String)typesVector.elementAt(i)),gbConstraints);
				gbConstraints.gridx = 1;
				advancedFields[i] = new javax.swing.JTextField(8);
				gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
				gbConstraints.weightx = 1;
				add(advancedFields[i],gbConstraints);
				gbConstraints.gridx = 0;
				gbConstraints.gridy++;
			}
			searchButton.setEnabled(true);
		}
		
		gbConstraints.gridx = 0;
		add(fewerOptionsButton,gbConstraints);
		
		gbConstraints.gridx = 1;
		gbConstraints.gridy++;
		add(searchButton,gbConstraints);
		this.currentStyle = ADVANCED_UNION;
	}
	
	public void actionPerformed(java.awt.event.ActionEvent ae)
	{
		if (ae.getSource() == this.moreOptionsButton) {
			if (this.currentStyle == BASIC) {
				makePanel(ADVANCED_INTERSECTION);
			} else {
				makePanel(ADVANCED_UNION);
			}
		} else if (ae.getSource() == this.fewerOptionsButton) {
			if (this.currentStyle == ADVANCED_UNION) {
				makePanel(ADVANCED_INTERSECTION);
			} else {
				makePanel(BASIC);
			}
		} else {
			this.criteria = field.getText();
			fireSearch(new edu.tufts.vue.fsm.event.SearchEvent(this));
		}
	}
	
	public void addSearchListener(edu.tufts.vue.fsm.event.SearchListener listener)
	{
		listenerList.add(edu.tufts.vue.fsm.event.SearchListener.class, listener);
	}
    
	public void removeSearchListener(edu.tufts.vue.fsm.event.SearchListener listener)
	{
		listenerList.remove(edu.tufts.vue.fsm.event.SearchListener.class, listener);
	}
    
	private void fireSearch(edu.tufts.vue.fsm.event.SearchEvent evt) 
	{
		this.searchButton.setEnabled(false);
		Object[] listeners = listenerList.getListenerList();
		for (int i=0; i<listeners.length; i+=2) {
			if (listeners[i] == edu.tufts.vue.fsm.event.SearchListener.class) {
				((edu.tufts.vue.fsm.event.SearchListener)listeners[i+1]).searchPerformed(evt);
			}
		}
	}
	
	public java.io.Serializable getCriteria() {
		return field.getText();
	}
	
	public void setCriteria(java.io.Serializable searchCriteria) {
		if (searchCriteria instanceof String) {
			this.criteria = searchCriteria;
			field.setText((String)this.criteria);
		} else {
			this.criteria = null;
			field.setText("");		
		}
	}
	
	public org.osid.shared.Properties getProperties() {
		return this.searchProperties;
	}
	
	public void setProperties(org.osid.shared.Properties searchProperties) {
		this.searchProperties = searchProperties;
	}

	public String getSearchDisplayName() {
		// return the criteria, no longer than 20 characters worth
		String s =  (String)getCriteria();
		if (s.length() > 20) s = s.substring(0,20) + "...";
		return s;
	}


	private java.util.Vector getUnionSearchFields()
	{
		/*
		 Find each repository that will be searched.  Get all the asset types and for each the
		 mandatory record structures.  For each structure find the part structures and their
		 types.  Check these again the VUE set (based on Dublin Core).  Return the VUE names for
		 all that match.
		 */
		
		java.util.Vector union = new java.util.Vector();
		
		try {
			org.osid.repository.Repository[] repositories = sourcesAndTypesManager.getRepositoriesToSearch();
			for (int i=0; i < repositories.length; i++) {

				// not all these methods may be implemented -- in which case we are out of luck
				try {
					org.osid.shared.TypeIterator typeIterator = repositories[i].getAssetTypes();
					while (typeIterator.hasNextType()) {
						org.osid.shared.Type nextAssetType = typeIterator.nextType();
										   
						org.osid.repository.RecordStructureIterator recordStructureIterator = repositories[i].getMandatoryRecordStructures(nextAssetType);
						while (recordStructureIterator.hasNextRecordStructure()) {
							org.osid.repository.PartStructureIterator partStructureIterator = recordStructureIterator.nextRecordStructure().getPartStructures();
							while (partStructureIterator.hasNextPartStructure()) {
								org.osid.shared.Type nextType = partStructureIterator.nextPartStructure().getType();
								String nextTypeString = edu.tufts.vue.util.Utilities.typeToString(nextType);
								
								int index = advancedSearchUniverseOfTypeStringsVector.indexOf(nextTypeString);
								if (index != -1) {
									String prompt = (String)advancedSearchPromptsVector.elementAt(index);
									if (!union.contains(prompt)) {
										union.addElement(prompt);
									}
								}
							}
						}
					}
				} catch (Throwable t) {
					edu.tufts.vue.util.Logger.log(t);
				}
			}
		} catch (Throwable t1) {
			edu.tufts.vue.util.Logger.log(t1);
		}
		return union;
	}
		
	private java.util.Vector getIntersectionSearchFields()
	{
		/*
		 Find each repository that will be searched.  Get all the asset types and for each the
		 mandatory record structures.  For each structure find the part structures and their
		 types.  Check these again the VUE set (based on Dublin Core).  Return the VUE names for
		 all that match.
		 */
		java.util.Vector intersections = new java.util.Vector();

		try {
			org.osid.repository.Repository[] repositories = sourcesAndTypesManager.getRepositoriesToSearch();
			for (int i=0; i < repositories.length; i++) {
				
				java.util.Vector intersection = new java.util.Vector();				
				// not all these methods may be implemented -- in which case we are out of luck
				try {
					org.osid.shared.TypeIterator typeIterator = repositories[i].getAssetTypes();
					while (typeIterator.hasNextType()) {
						org.osid.shared.Type nextAssetType = typeIterator.nextType();
						
						org.osid.repository.RecordStructureIterator recordStructureIterator = repositories[i].getMandatoryRecordStructures(nextAssetType);
						while (recordStructureIterator.hasNextRecordStructure()) {
							org.osid.repository.PartStructureIterator partStructureIterator = recordStructureIterator.nextRecordStructure().getPartStructures();
							while (partStructureIterator.hasNextPartStructure()) {
								org.osid.shared.Type nextType = partStructureIterator.nextPartStructure().getType();
								String nextTypeString = edu.tufts.vue.util.Utilities.typeToString(nextType);
								int index = advancedSearchUniverseOfTypeStringsVector.indexOf(nextTypeString);
								if (index != -1) {
									String prompt = (String)advancedSearchPromptsVector.elementAt(index);
									if (!intersection.contains(prompt)) {
										intersection.addElement(prompt);
									}
								}
							}
						}
					}
					intersections.addElement(intersection);
				} catch (Throwable t) {
					edu.tufts.vue.util.Logger.log(t);
				}
			}
			// now find what is common accross intersections
			int numIntersections = intersections.size();
			if (numIntersections == 0) {
				return intersections;
			} else {
				java.util.Vector intersection = (java.util.Vector)intersections.firstElement();				
				for (int j=1; j < numIntersections; j++) {
					java.util.Vector nextIntersection = (java.util.Vector)intersections.elementAt(j);
					java.util.Vector newIntersection = new java.util.Vector();

					int numCandidates = intersection.size();
					for (int k=0; k < numCandidates; k++) {
						String nextType = (String)intersection.elementAt(k);
						if (nextIntersection.contains(nextType)) {
							newIntersection.addElement(nextType);
						}
					}
					intersection = newIntersection;
				}
				return intersection;
			}
		} catch (Throwable t1) {
			edu.tufts.vue.util.Logger.log(t1);
		}
		return new java.util.Vector();
	}
	
	private void populateAdvancedSearchUniverseOfTypeStringsVector()
	{
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/contributor@edu.mit");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/coverage@edu.mit");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/creator@edu.mit");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/date@edu.mit");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/description@edu.mit");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/format@edu.mit");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/identifier@edu.mit");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/language@edu.mit");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/publisher@edu.mit");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/relation@edu.mit");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/rights@edu.mit");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/source@edu.mit");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/subject@edu.mit");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/title@edu.mit");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/type@edu.mit");
		
		advancedSearchPromptsVector.addElement("Contributor");
		advancedSearchPromptsVector.addElement("Coverage");
		advancedSearchPromptsVector.addElement("Creator");
		advancedSearchPromptsVector.addElement("Date");
		advancedSearchPromptsVector.addElement("Description");
		advancedSearchPromptsVector.addElement("Format");
		advancedSearchPromptsVector.addElement("Identifier");
		advancedSearchPromptsVector.addElement("Lanugage");
		advancedSearchPromptsVector.addElement("Publisher");
		advancedSearchPromptsVector.addElement("Relation");
		advancedSearchPromptsVector.addElement("Rights");
		advancedSearchPromptsVector.addElement("Source");
		advancedSearchPromptsVector.addElement("Subject");
		advancedSearchPromptsVector.addElement("Title");
		advancedSearchPromptsVector.addElement("Type");

		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/contributor@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/coverage@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/creator@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/date@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/description@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/format@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/identifier@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/language@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/publisher@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/relation@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/rights@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/source@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/subject@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/title@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/type@mit.edu");
		
		advancedSearchPromptsVector.addElement("Contributor");
		advancedSearchPromptsVector.addElement("Coverage");
		advancedSearchPromptsVector.addElement("Creator");
		advancedSearchPromptsVector.addElement("Date");
		advancedSearchPromptsVector.addElement("Description");
		advancedSearchPromptsVector.addElement("Format");
		advancedSearchPromptsVector.addElement("Identifier");
		advancedSearchPromptsVector.addElement("Lanugage");
		advancedSearchPromptsVector.addElement("Publisher");
		advancedSearchPromptsVector.addElement("Relation");
		advancedSearchPromptsVector.addElement("Rights");
		advancedSearchPromptsVector.addElement("Source");
		advancedSearchPromptsVector.addElement("Subject");
		advancedSearchPromptsVector.addElement("Title");
		advancedSearchPromptsVector.addElement("Type");
		
	}
}