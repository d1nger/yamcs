package org.yamcs.xtce;


import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.yamcs.xtce.NameDescription;
import org.yamcs.xtce.xml.XtceAliasSet;


/**
 * Keeps a list of parameters with corresponding indexes to be able to retrieve them in any namespace.
 * 
 * Currently the name is case sensitive while aliases are not. Is this the correct behaviour???
 * @author nm
 *
 */
public class NamedDescriptionIndex<T extends NameDescription> implements Serializable{
    private static final long serialVersionUID = 3L;
    
    private LinkedHashMap<String, LinkedHashMap<String,T>> aliasIndex =new LinkedHashMap<String, LinkedHashMap<String,T>>();
    private LinkedHashMap<String,T> index =new LinkedHashMap<String,T>();
    
    
    public void add(T o) {
        XtceAliasSet aliases=o.getAliasSet();
        if(aliases!=null) {
            for(String ns:aliases.getNamespaces()) {
                LinkedHashMap<String, T> m=aliasIndex.get(ns);
                if(m==null) {
                    m=new LinkedHashMap<String, T>();
                    aliasIndex.put(ns, m);
                }
                m.put(aliases.getAlias(ns).toUpperCase(), o);
            }
        }
        index.put(o.getName(), o);
    }
   
    /**
     * returns the object based on its canonical name
     * @param name
     * @return
     */
    public T get(String name) {
        return index.get(name);
    }
    
    /**
     * returns the object in namespace
     * @param name
     * @param nameSpace
     * @return
     */
    public T get(String nameSpace, String name) {
        Map<String, T>m=aliasIndex.get(nameSpace);
        if (m!=null) {
            return m.get(name.toUpperCase());
        } else {
            return null;
        }
    }
    /**
     *  returns a collection of all the objects (parameters) in the index
     * @return
     */
    public Collection<T> getObjects() {
        return index.values();
    }

    /**
     * 
     * @return number of objects in index
     */
    public int size() {
        return index.size();
    }
}