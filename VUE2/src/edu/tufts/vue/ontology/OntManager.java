/*
 * OntManager.java
 *
 * Created on March 12, 2007, 10:59 AM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */
package edu.tufts.vue.ontology;

import java.util.*;
import edu.tufts.vue.style.*;
import java.net.*;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.util.iterator.*;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.iterator.Filter;
import java.io.*;

// castor classes
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.xml.sax.InputSource;
import osid.dr.*;

public class OntManager {
    public static final String ONT_NOT_FOUND = "Ontology Not Found";
    public static final String ONT_FILE = tufts.vue.VueResources.getString("ontology.save");
    public static final int RDFS = 0;
    public static final int OWL = 1;
    /** Creates a new instance of OntManager */
    List<Ontology> ontList = Collections.synchronizedList(new ArrayList<Ontology>());
    static OntManager ontManager;
    public OntManager() {
    }
    public Ontology readOntologyWithStyle(URL ontUrl,URL cssUrl,org.osid.shared.Type type) {
        Ontology ont = null;
        if(type.isEqual(OntologyType.RDFS_TYPE)) {
            ont =  new RDFSOntology(ontUrl,cssUrl);
        } else if(type.isEqual(OntologyType.OWL_TYPE)) {
            ont = new OWLLOntology(ontUrl,cssUrl);
        }
        if(ont != null) {
            ontList.add(ont);
        }
        return ont;
    }
    
    public Ontology readOntology(URL ontUrl,org.osid.shared.Type type) {
        Ontology ont =null;
        if(type.isEqual(OntologyType.RDFS_TYPE)) {
            ont = new RDFSOntology(ontUrl);
        } else if(type.isEqual(OntologyType.OWL_TYPE)) {
            ont = new OWLLOntology(ontUrl);
        }
        if(ont != null) {
            ontList.add(ont);
        }
        return ont;
    }
    
    public void applyStyleToOntology(URL ontUrl,URL cssUrl) throws Throwable  {
        Ontology ont = getOntology(ontUrl);
        if(ont == null) {
            throw new Exception(ONT_NOT_FOUND);
        }
        ont.applyStyle(cssUrl);
        
    }
    public List<Ontology> getOntList() {
        return ontList;
    }
    
    public void setOntList(List<Ontology> list) {
        this.ontList = list;
    }
    public Ontology getOntology(URL ontUrl) {
        for(Ontology ont: ontList) {
            if(ont.getBase().equalsIgnoreCase(ontUrl.toString())){
                return ont;
            }
        }
        return null;
    }
    
    public void removeOntology(URL ontUrl) {
        for(Ontology ont: ontList) {
            if(ont.getBase().equalsIgnoreCase(ontUrl.toString())){
                ontList.remove(ont);
                return;
            }
        }
    }
    
    public Ontology applyStyle(URL ontUrl, URL cssUrl) {
        return null;
    }
    
    public void save() {
        if(ontManager != null) {
            try {
                File file = new File(tufts.vue.VueUtil.getDefaultUserFolder()+File.separator+ONT_FILE);
                Mapping mapping = new Mapping();
                FileWriter writer = new FileWriter(file);
                Marshaller marshaller = new Marshaller(writer);
                marshaller.setMapping(tufts.vue.action.ActionUtil.getDefaultMapping());
                System.out.println("Marshalling OntManager. Class = "+this.getClass());
                marshaller.marshal(this);
                writer.flush();
                writer.close();
            } catch(Exception ex) {
                tufts.vue.VUE.Log.error("OntManager.save: "+ex);
            }
        } else
            tufts.vue.VUE.Log.error("OntManager.save: OntManager not initialized");
        
    }
    
    public void load() {
        try {
            Unmarshaller unmarshaller = tufts.vue.action.ActionUtil.getDefaultUnmarshaller();
            File file = new File(tufts.vue.VueUtil.getDefaultUserFolder()+File.separator+ONT_FILE);
            FileReader reader = new FileReader(file);
            ontManager = (OntManager) unmarshaller.unmarshal(new InputSource(reader));
            reader.close();
        } catch(Exception ex) {
            tufts.vue.VUE.Log.error("OntManager.save: "+ex);
        }
    }
    public  static OntManager getOntManager() {
        if(ontManager == null) {
            ontManager = new OntManager();
        }
        return ontManager;
    }
    
}
