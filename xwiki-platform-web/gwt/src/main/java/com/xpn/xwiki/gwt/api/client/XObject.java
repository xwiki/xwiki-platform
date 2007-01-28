package com.xpn.xwiki.gwt.api.client;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 1 déc. 2006
 * Time: 01:42:40
 * To change this template use File | Settings | File Templates.
 */
public class XObject implements IsSerializable {
    private Map properties = new HashMap();
    private Map viewProperties = new HashMap();
    private Map editProperties = new HashMap();
    private String name;
    private String className;
    private int nb;

    public List getPropertyNames() {
        List propnames = new ArrayList();
        Iterator it =  properties.keySet().iterator();
        while (it.hasNext()) {
             propnames.add(it.next());
        }
        return propnames;
    }

    public Map getProperties() {
        return properties;
    }

    public Object getProperty(String name) {
        return properties.get(name);
    }

    public Object get(String name) {
        return getProperty(name);
    }

    public void setProperty(String name, Object prop) {
        properties.put(name, prop);
    }

    public void set(String name, Object prop) {
        setProperty(name, prop);
    }

    public String getViewProperty(String name) {
        return (String) viewProperties.get(name);
    }

    public void setViewProperty(String name, String prop) {
        viewProperties.put(name, prop);
    }

    public String getEditProperty(String name) {
        return (String) editProperties.get(name);
    }

    public void setEditProperty(String name, String prop) {
        editProperties.put(name, prop);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getNumber() {
        return nb;
    }

    public void setNumber(int nb) {
        this.nb = nb;
    }
}
