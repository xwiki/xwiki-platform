/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.gwt.api.client;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class XObject implements IsSerializable {
    private Map properties = new HashMap();
    private Map viewProperties = new HashMap();
    private Map editProperties = new HashMap();
    private Map editPropertiesFieldName = new HashMap();
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

    /**
     * 
     * @param name the name of the field
     * @return html code to view the given field
     */
    public String getViewProperty(String name) {
        return (String) viewProperties.get(name);
    }

    public void setViewProperty(String name, String prop) {
        viewProperties.put(name, prop);
    }

    /**
     *
     * @param name the name of the field
     * @return html code to edit the given field
     */
    public String getEditProperty(String name) {
        return (String) editProperties.get(name);
    }

    public void setEditProperty(String name, String prop) {
        editProperties.put(name, prop);
    }

    public String getEditPropertyFieldName(String name) {
        return (String) editPropertiesFieldName.get(name);
    }

    public void setEditPropertyFieldName(String name, String fieldName) {
        editPropertiesFieldName.put(name, fieldName);
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
