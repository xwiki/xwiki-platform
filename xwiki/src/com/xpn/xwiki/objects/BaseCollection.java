/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 9 déc. 2003
 * Time: 11:36:06
 */
package com.xpn.xwiki.objects;


import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import org.apache.commons.collections.map.ListOrderedMap;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.*;

public abstract class BaseCollection extends BaseElement implements ObjectInterface, Serializable {
    private String className;
    private Map fields = ListOrderedMap.decorate(new HashMap());
    private List fieldsToRemove = new ArrayList();
    private int number;

    public int getId() {
            return (getName()+getClassName()).hashCode();
        }

    public void setId(int id) {
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public void addPropertyForRemoval(PropertyInterface field) {
        getFieldsToRemove().add(field);
    }

    public String getClassName() {
         return (className == null) ? "" : className;
    }

    public void setClassName(String name) {
        className = name;
    }


    public void checkField(String name) throws XWikiException {
        /*  // Let's stop checking.. This is a pain
        if (getxWikiClass(context).safeget(name)==null) {
            Object[] args = { name, getxWikiClass(context).getName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_CLASSES, XWikiException.ERROR_XWIKI_CLASSES_FIELD_DOES_NOT_EXIST,
                    "Field {0} does not exist in class {1}", null, args);
        } */
    }

    public PropertyInterface safeget(String name) {
        return (PropertyInterface) getFields().get(name);
    }

    public PropertyInterface get(String name) throws XWikiException {
        checkField(name);
        return safeget(name);
    }

    public void safeput(String name, PropertyInterface property) {
        addField(name, property);
        if (property instanceof BaseProperty) {
         ((BaseProperty)property).setObject(this);
         ((BaseProperty)property).setName(name);
        }
    }

    public void put(String name, PropertyInterface property) throws XWikiException {
        checkField(name);
        safeput(name, property);
    }


    public BaseClass getxWikiClass(XWikiContext context) {
        String name = getClassName();
        try {
            XWikiDocInterface doc = context.getWiki().getDocument(name, context);
         if (doc==null)
          return null;
        else
         return doc.getxWikiClass();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /*public void setxWikiClass(BaseClass xWikiClass) {
        setClassName(xWikiClass.getName());
    }*/

    public String getStringValue(String name) {
        StringProperty prop = (StringProperty)safeget(name);
        if (prop==null)
         return "";
        else
         return (String)prop.getValue();
    }

    public String getLargeStringValue(String name) {
           return getStringValue(name);
       }
    public void setStringValue(String name, String value) {
        StringProperty property = new StringProperty();
        property.setName(name);
        property.setValue(value);
        safeput(name, property);
    }

    public void setLargeStringValue(String name, String value) {
        LargeStringProperty property = new LargeStringProperty();
        property.setName(name);
        property.setValue(value);
        safeput(name, property);
    }


    public int getIntValue(String name) {
        try {
        NumberProperty prop = (NumberProperty)safeget(name);
        if (prop==null)
         return 0;
        else
         return ((Number)prop.getValue()).intValue();
        }
         catch (Exception e) {
            return 0;
        }
    }

    public void setIntValue(String name, int value) {
        NumberProperty property = new IntegerProperty();
        property.setName(name);
        property.setValue(new Integer(value));
        safeput(name, property);
    }

    public Set getSetValue(String name) {
        ListProperty prop = (ListProperty)safeget(name);
        if (prop==null)
         return new HashSet();
        else
         return (Set)prop.getValue();
    }

    public void setSetValue(String name, Set value) {
        ListProperty property = new ListProperty();
        property.setValue(value);
        safeput(name, property);
    }

    // These functions should not be used
    // but instead our own implementation
    private Map getFields() {
        return fields;
    }

    public void setFields(Map fields) {
        this.fields = fields;
    }

    public PropertyInterface getField(String name) {
        return (PropertyInterface)fields.get(name);
    }

    public void addField(String name, PropertyInterface element) {
        fields.put(name, element);
    }

    public void removeField(String name) {
        Object field = safeget(name);
        if (field!=null) {
         fields.remove(name);
         fieldsToRemove.add(field);
        }
    }

    public Collection getFieldList() {
        return fields.values();
    }

    public Set getPropertyList() {
        return fields.keySet();
    }

    public Object[] getProperties() {
        Object[] array = getFields().values().toArray();
        return array;
    }

    public Object[] getPropertyNames() {
        Object[] array = getFields().keySet().toArray();
        return array;
    }

    public boolean equals(Object coll) {
     if (!super.equals(coll))
      return false;
     BaseCollection collection = (BaseCollection) coll;
     if (collection.getClassName()==null) {
         if (getClassName()!=null)
         return false;
     } else if (!collection.getClassName().equals(getClassName()))
         return false;

     if (getFields().size()!=collection.getFields().size())
         return false;

     Iterator itfields = getFields().keySet().iterator();
     while (itfields.hasNext()) {
       String name = (String) itfields.next();
       Object prop = getFields().get(name);
       Object prop2 = collection.getFields().get(name);
       if (!prop.equals(prop2))
        return false;
     }

     return true;
    }

    public Object clone() {
        BaseCollection collection = (BaseCollection) super.clone();
        collection.setClassName(getClassName());
        Map fields = getFields();
        Map cfields = new HashMap();
        Iterator itfields = fields.keySet().iterator();
        while (itfields.hasNext()) {
            String name = (String)itfields.next();
            PropertyInterface prop = (PropertyInterface)((BaseElement)fields.get(name)).clone();
            prop.setObject(collection);
            cfields.put(name, prop);
        }
        collection.setFields(cfields);
        return collection;
    }

    public void merge(BaseObject object) {
        Iterator itfields = object.getPropertyList().iterator();
        while (itfields.hasNext()) {
          String name = (String) itfields.next();
          if (safeget(name)==null)
              safeput(name, (PropertyInterface) ((BaseElement)object.safeget(name)).clone());
        }
    }

    public List getFieldsToRemove() {
        return fieldsToRemove;
    }

    public void setFieldsToRemove(List fieldsToRemove) {
        this.fieldsToRemove = fieldsToRemove;
    }

    public abstract Element toXML(BaseClass bclass);

    public String toXMLString() {
        Document doc = new DOMDocument();
        doc.setRootElement(toXML(null));
        OutputFormat outputFormat = new OutputFormat("", true);
        StringWriter out = new StringWriter();
        XMLWriter writer = new XMLWriter( out, outputFormat );
        try {
            writer.write(doc);
            return out.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public String toString() {
        return toXMLString();
    }


}
