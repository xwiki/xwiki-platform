/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 * @author ludovic
 * @author erwan
 * @author sdumitriu
 */

package com.xpn.xwiki.objects;


import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.ListOrderedMap;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

public abstract class BaseCollection extends BaseElement implements ObjectInterface, Serializable {
    protected String className;
    protected Map fields = ListOrderedMap.decorate(new HashMap());
    protected List fieldsToRemove = new ArrayList();
    protected int number;

    public int getId() {
        return hashCode();
    }

    public int hashCode() {
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
		// TODO: order?
        checkField(name);
        safeput(name, property);
    }


    public BaseClass getxWikiClass(XWikiContext context) {
        String name = getClassName();
        try {
            if ((context==null)||(context.getWiki()==null))
                return null;
            return context.getWiki().getClass(name, context);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getStringValue(String name) {
        BaseProperty prop = (BaseProperty) safeget(name);
        if (prop==null)
         return "";
        else
         return prop.getValue().toString();
    }

    public String getLargeStringValue(String name) {
           return getStringValue(name);
       }
    public void setStringValue(String name, String value) {
        BaseStringProperty property = (BaseStringProperty) safeget(name);
        if (property==null)
         property = new StringProperty();
        property.setName(name);
        property.setValue(value);
        safeput(name, property);
    }

    public void setLargeStringValue(String name, String value) {
        BaseStringProperty property = (BaseStringProperty) safeget(name);
        if (property==null)
         property = new LargeStringProperty();
        property.setName(name);
        property.setValue(value);
        safeput(name, property);
    }


    public int getIntValue(String name) {
        return getIntValue(name, 0);
    }

    public int getIntValue(String name, int default_value) {
        try {
        NumberProperty prop = (NumberProperty)safeget(name);
        if (prop==null)
         return default_value;
        else
         return ((Number)prop.getValue()).intValue();
        }
         catch (Exception e) {
            return default_value;
        }
    }

    public void setIntValue(String name, int value) {
        NumberProperty property = new IntegerProperty();
        property.setName(name);
        property.setValue(new Integer(value));
        safeput(name, property);
    }

    public long getLongValue(String name) {
        try {
        NumberProperty prop = (NumberProperty)safeget(name);
        if (prop==null)
         return 0;
        else
         return ((Number)prop.getValue()).longValue();
        }
         catch (Exception e) {
            return 0;
        }             
    }

    public void setLongValue(String name, long value) {
        NumberProperty property = new LongProperty();
        property.setName(name);
        property.setValue(new Long(value));
        safeput(name, property);
    }


    public float getFloatValue(String name) {
        try {
        NumberProperty prop = (NumberProperty)safeget(name);
        if (prop==null)
         return 0;
        else
         return ((Number)prop.getValue()).floatValue();
        }
         catch (Exception e) {
            return 0;
        }
    }

    public void setFloatValue(String name, float value) {
        NumberProperty property = new FloatProperty();
        property.setName(name);
        property.setValue(new Float(value));
        safeput(name, property);
    }

    public double getDoubleValue(String name) {
        try {
        NumberProperty prop = (NumberProperty)safeget(name);
        if (prop==null)
         return 0;
        else
         return ((Number)prop.getValue()).doubleValue();
        }
         catch (Exception e) {
            return 0;
        }
    }

    public void setDoubleValue(String name, double value) {
        NumberProperty property = new DoubleProperty();
        property.setName(name);
        property.setValue(new Double(value));
        safeput(name, property);
    }

    public Date getDateValue(String name) {
        try {
        DateProperty prop = (DateProperty)safeget(name);
        if (prop==null)
         return null;
        else
         return (Date)prop.getValue();
        }
         catch (Exception e) {
            return null;
        }
    }

    public void setDateValue(String name, Date value) {
        DateProperty property = new DateProperty();
        property.setName(name);
        property.setValue(value);
        safeput(name, property);
    }

    public Set getSetValue(String name) {
        ListProperty prop = (ListProperty)safeget(name);
        if (prop==null)
         return new HashSet();
        else {
            return new HashSet((Collection)prop.getValue());
        }
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
        collection.setNumber(getNumber());
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
    

    public List getDiff(Object coll, XWikiContext context) {
        ArrayList difflist = new ArrayList();
        BaseCollection collection = (BaseCollection) coll;
        Iterator itfields = getFields().keySet().iterator();
        while (itfields.hasNext()) {
            String name = (String) itfields.next();
            BaseElement prop = (BaseElement)getFields().get(name);
            BaseElement prop2 = (BaseElement)collection.getFields().get(name);

            if (prop2==null) {
                String dprop = ((PropertyClass)getxWikiClass(context).getField(name)).displayView(name, this,context);
                difflist.add(new ObjectDiff(getClassName(), getNumber(), "added",
                        name, dprop , ""));
            } else if (!prop.equals(prop2)) {
                BaseClass bclass = getxWikiClass(context);
                PropertyClass pclass = (PropertyClass) ((bclass==null) ? null : bclass.getField(name));
                if (pclass==null) {
                    difflist.add(new ObjectDiff(getClassName(), getNumber(), "changed",
                            name, prop.toString() , prop2.toString()));
                } else {
                    String dprop = pclass.displayView(name,this,context);
                    String dprop2 = pclass.displayView(name,collection,context);
                    difflist.add(new ObjectDiff(getClassName(), getNumber(), "changed",
                            name, dprop , dprop2));
                }
            }
        }

        itfields = collection.getFields().keySet().iterator();
        while (itfields.hasNext()) {
            String name = (String) itfields.next();
            BaseElement prop = (BaseElement)getFields().get(name);
            BaseElement prop2 = (BaseElement)collection.getFields().get(name);

            if (prop==null) {
                BaseClass bclass = getxWikiClass(context);
                PropertyClass pclass = (PropertyClass) ((bclass==null) ? null : bclass.getField(name));
                if (pclass==null) {
                    difflist.add(new ObjectDiff(getClassName(), getNumber(), "changed",
                            name, "" , prop2.toString()));
                } else {
                    String dprop2 = ((PropertyClass)getxWikiClass(context).getField(name)).displayView(name,collection,context);
                    difflist.add(new ObjectDiff(getClassName(), getNumber(), "removed",
                            name, "" , dprop2));
                }
            }
        }

        return difflist;
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

    public Map getMap() throws XWikiException {
        Map map = new HashMap();
        for (Iterator it = fields.keySet().iterator();it.hasNext();) {
            String name = (String) it.next();
            BaseProperty property = (BaseProperty) get(name);
            map.put(name, property.getValue());
        }
        map.put("id", new Integer(getId()));
        return map;
    }
}
