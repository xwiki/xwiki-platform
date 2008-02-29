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
 *
 */

package com.xpn.xwiki.objects;


import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
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

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.objects.ObjectInterface#getxWikiClass(com.xpn.xwiki.XWikiContext)
     */
    public BaseClass getxWikiClass(XWikiContext context)
    {
        if ((context == null) || (context.getWiki() == null)) {
            return null;
        }
        String name = getClassName();
        String wiki = getWiki();

        String database = context.getDatabase();
        try {
            context.setDatabase(wiki);
            return context.getWiki().getClass(name, context);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            context.setDatabase(database);
        }
    }

    public String getStringValue(String name) {
        BaseProperty prop = (BaseProperty) safeget(name);
        if (prop==null || prop.getValue()==null)
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

    public List getListValue(String name) {
        ListProperty prop = (ListProperty)safeget(name);
        if (prop==null)
         return new ArrayList();
        else {
            return  (List)prop.getValue();
        }
    }

    /**
     * @deprecated use setStringListValue or setDBStringListProperty
     * @param name
     * @param value
     */
    public void setListValue(String name, List value) {
        ListProperty property = (ListProperty) safeget(name);
        if (property==null)
         property = new StringListProperty();
        property.setValue(value);
        safeput(name, property);
    }

    public void setStringListValue(String name, List value) {
        ListProperty property = (ListProperty) safeget(name);
        if (property==null)
         property = new StringListProperty();
        property.setValue(value);
        safeput(name, property);
    }

    public void setDBStringListValue(String name, List value) {
        ListProperty property = (ListProperty) safeget(name);
        if (property==null)
         property = new DBStringListProperty();
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

    /**
     * Return an iterator that will operate on a collection of values (as would be returned
     * by getProperties or getFieldList) sorted by their name (ElementInterface.getName()).
     */
    public Iterator getSortedIterator() {
        Iterator it = null;
        try {
            // Use getProperties to get the values in list form (rather than as generic collection)
            List propList = Arrays.asList(getProperties());
            
            // Use the element comparator to sort the properties by name (based on ElementInterface)
            Collections.sort(propList, new ElementComparator());
            
            // Iterate over the sorted property list
            it = propList.iterator();
        } catch (ClassCastException ccex ) {
            // If sorting by the comparator resulted in a ClassCastException (possible), 
            // iterate over the generic collection of values.         
            it = getFieldList().iterator();
        }
        
        return it;
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

    public List getDiff(Object oldObject, XWikiContext context) {
        ArrayList<ObjectDiff> difflist = new ArrayList<ObjectDiff>();
        BaseCollection oldCollection = (BaseCollection) oldObject;
        // Iterate over the new properties first, to handle changed and added objects
        for (Object key : this.getFields().keySet()) {
            String propertyName = (String) key;
            BaseProperty newProperty = (BaseProperty) this.getFields().get(propertyName);
            BaseProperty oldProperty = (BaseProperty) oldCollection.getFields().get(propertyName);

            if (oldProperty==null) {
                // The property exist in the new object, but not in the old one
                if ((newProperty != null) && (!newProperty.toText().equals(""))) {
                    String newPropertyValue =
                        (newProperty.getValue() instanceof String) ? newProperty.toText()
                            : ((PropertyClass) getxWikiClass(context).getField(propertyName))
                                .displayView(propertyName, this, context);
                    difflist.add(new ObjectDiff(getClassName(), getNumber(), "added",
                            propertyName, "", newPropertyValue));
                }
            } else if (!oldProperty.toText().equals(((newProperty==null) ? "" : newProperty.toText()))) {
                // The property exists in both objects and is different
                BaseClass bclass = getxWikiClass(context);
                PropertyClass pclass = (PropertyClass) ((bclass==null) ? null : bclass.getField(propertyName));
                if (pclass != null) {
                    // Put the values as they would be displayed in the interface
                    String newPropertyValue =
                        (newProperty.getValue() instanceof String) ? newProperty.toText()
                            : pclass.displayView(propertyName, this, context);
                    String oldPropertyValue =
                        (oldProperty.getValue() instanceof String) ? oldProperty.toText()
                            : pclass.displayView(propertyName, oldCollection, context);
                    difflist.add(new ObjectDiff(getClassName(), getNumber(), "changed",
                            propertyName, oldPropertyValue, newPropertyValue));
                } else {
                    // Cannot get property definition, so use the plain value
                    difflist.add(new ObjectDiff(getClassName(), getNumber(), "changed",
                        propertyName, oldProperty.toText(), newProperty.toText()));
                }
            }
        }

        // Iterate over the old properties, in case there are some removed properties
        for (Object key : oldCollection.getFields().keySet()) {
            String propertyName = (String) key;
            BaseProperty newProperty = (BaseProperty) this.getFields().get(propertyName);
            BaseProperty oldProperty = (BaseProperty) oldCollection.getFields().get(propertyName);

            if (newProperty == null) {
                // The property exists in the old object, but not in the new one
                if ((oldProperty!=null)&&(!oldProperty.toText().equals(""))) {
                    BaseClass bclass = getxWikiClass(context);
                    PropertyClass pclass = (PropertyClass) ((bclass==null) ? null : bclass.getField(propertyName));
                    if (pclass != null) {
                        // Put the values as they would be displayed in the interface
                        String oldPropertyValue =
                            (oldProperty.getValue() instanceof String) ? oldProperty.toText()
                                : ((PropertyClass) getxWikiClass(context).getField(propertyName))
                                    .displayView(propertyName, oldCollection, context);
                        difflist.add(new ObjectDiff(getClassName(), getNumber(), "removed",
                                propertyName, oldPropertyValue, ""));
                    } else {
                        // Cannot get property definition, so use the plain value
                        difflist.add(new ObjectDiff(getClassName(), getNumber(), "removed",
                                propertyName, oldProperty.toText(), ""));
                    }
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

    public Map getCustomMappingMap() throws XWikiException {
        Map map = new HashMap();
        for (Iterator it = fields.keySet().iterator();it.hasNext();) {
            String name = (String) it.next();
            BaseProperty property = (BaseProperty) get(name);
            map.put(name, property.getCustomMappingValue());
        }
        map.put("id", new Integer(getId()));
        return map;
    }
}
