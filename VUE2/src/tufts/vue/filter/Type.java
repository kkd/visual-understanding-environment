/*
 * Class.java
 *
 * Created on February 13, 2004, 2:45 PM
 */

package tufts.vue.filter;

/**
 *
 * @author  akumar03
 *
 * A type defines all 
 */
import java.util.List;

public interface Type{
    
    /** Creates a new instance of Class */
    public void setDisplayName(String displayName);
    public String getDisplayName();
    public List getOperators();
    public Operator getDefaultOperator();
    public List getSettableOperators();
    
    public boolean isValidKey();
    public boolean isValidValue();
}
