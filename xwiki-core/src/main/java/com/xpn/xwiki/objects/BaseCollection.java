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

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

public abstract class BaseCollection extends BaseElement implements ObjectInterface, Cloneable
{
    protected static final Log LOG = LogFactory.getLog(BaseCollection.class);

    protected String className;

    protected Map<String, Object> fields = new LinkedHashMap<String, Object>();

    protected List fieldsToRemove = new ArrayList();

    protected int number;

    public int getId()
    {
        return hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return (getName() + getClassName()).hashCode();
    }

    public void setId(int id)
    {
    }

    public int getNumber()
    {
        return this.number;
    }

    public void setNumber(int number)
    {
        this.number = number;
    }

    public void addPropertyForRemoval(PropertyInterface field)
    {
        getFieldsToRemove().add(field);
    }

    public String getClassName()
    {
        return (this.className == null) ? "" : this.className;
    }

    public void setClassName(String name)
    {
        this.className = name;
    }

    public void checkField(String name) throws XWikiException
    {
        /*
         * // Let's stop checking.. This is a pain if (getxWikiClass(context).safeget(name)==null) { Object[] args = {
         * name, getxWikiClass(context).getName() }; throw new XWikiException( XWikiException.MODULE_XWIKI_CLASSES,
         * XWikiException.ERROR_XWIKI_CLASSES_FIELD_DOES_NOT_EXIST, "Field {0} does not exist in class {1}", null,
         * args); }
         */
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.ObjectInterface#safeget(java.lang.String)
     */
    public PropertyInterface safeget(String name)
    {
        return (PropertyInterface) getFields().get(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.ObjectInterface#get(java.lang.String)
     */
    public PropertyInterface get(String name) throws XWikiException
    {
        checkField(name);
        return safeget(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.ObjectInterface#safeput(java.lang.String, com.xpn.xwiki.objects.PropertyInterface)
     */
    public void safeput(String name, PropertyInterface property)
    {
        addField(name, property);
        if (property instanceof BaseProperty) {
            ((BaseProperty) property).setObject(this);
            ((BaseProperty) property).setName(name);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.ObjectInterface#put(java.lang.String, com.xpn.xwiki.objects.PropertyInterface)
     */
    public void put(String name, PropertyInterface property) throws XWikiException
    {
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
        BaseClass baseClass = null;

        if ((context == null) || (context.getWiki() == null)) {
            return baseClass;
        }

        String name = getClassName();
        String wiki = getWiki();
        if (wiki == null) {
            wiki = context.getDatabase();
        }

        String database = context.getDatabase();
        try {
            context.setDatabase(wiki);

            baseClass = context.getWiki().getClass(name, context);
        } catch (Exception e) {
            LOG.error("Failed to get class [" + name + "] from wiki [" + wiki + "]", e);
        } finally {
            context.setDatabase(database);
        }

        return baseClass;
    }

    public String getStringValue(String name)
    {
        BaseProperty prop = (BaseProperty) safeget(name);
        if (prop == null || prop.getValue() == null) {
            return "";
        } else {
            return prop.getValue().toString();
        }
    }

    public String getLargeStringValue(String name)
    {
        return getStringValue(name);
    }

    public void setStringValue(String name, String value)
    {
        BaseStringProperty property = (BaseStringProperty) safeget(name);
        if (property == null) {
            property = new StringProperty();
        }
        property.setName(name);
        property.setValue(value);
        safeput(name, property);
    }

    public void setLargeStringValue(String name, String value)
    {
        BaseStringProperty property = (BaseStringProperty) safeget(name);
        if (property == null) {
            property = new LargeStringProperty();
        }
        property.setName(name);
        property.setValue(value);
        safeput(name, property);
    }

    public int getIntValue(String name)
    {
        return getIntValue(name, 0);
    }

    public int getIntValue(String name, int default_value)
    {
        try {
            NumberProperty prop = (NumberProperty) safeget(name);
            if (prop == null) {
                return default_value;
            } else {
                return ((Number) prop.getValue()).intValue();
            }
        } catch (Exception e) {
            return default_value;
        }
    }

    public void setIntValue(String name, int value)
    {
        NumberProperty property = new IntegerProperty();
        property.setName(name);
        property.setValue(value);
        safeput(name, property);
    }

    public long getLongValue(String name)
    {
        try {
            NumberProperty prop = (NumberProperty) safeget(name);
            if (prop == null) {
                return 0;
            } else {
                return ((Number) prop.getValue()).longValue();
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public void setLongValue(String name, long value)
    {
        NumberProperty property = new LongProperty();
        property.setName(name);
        property.setValue(value);
        safeput(name, property);
    }

    public float getFloatValue(String name)
    {
        try {
            NumberProperty prop = (NumberProperty) safeget(name);
            if (prop == null) {
                return 0;
            } else {
                return ((Number) prop.getValue()).floatValue();
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public void setFloatValue(String name, float value)
    {
        NumberProperty property = new FloatProperty();
        property.setName(name);
        property.setValue(new Float(value));
        safeput(name, property);
    }

    public double getDoubleValue(String name)
    {
        try {
            NumberProperty prop = (NumberProperty) safeget(name);
            if (prop == null) {
                return 0;
            } else {
                return ((Number) prop.getValue()).doubleValue();
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public void setDoubleValue(String name, double value)
    {
        NumberProperty property = new DoubleProperty();
        property.setName(name);
        property.setValue(new Double(value));
        safeput(name, property);
    }

    public Date getDateValue(String name)
    {
        try {
            DateProperty prop = (DateProperty) safeget(name);
            if (prop == null) {
                return null;
            } else {
                return (Date) prop.getValue();
            }
        } catch (Exception e) {
            return null;
        }
    }

    public void setDateValue(String name, Date value)
    {
        DateProperty property = new DateProperty();
        property.setName(name);
        property.setValue(value);
        safeput(name, property);
    }

    public Set< ? > getSetValue(String name)
    {
        ListProperty prop = (ListProperty) safeget(name);
        if (prop == null) {
            return new HashSet<Object>();
        } else {
            return new HashSet<Object>((Collection< ? >) prop.getValue());
        }
    }

    public void setSetValue(String name, Set< ? > value)
    {
        ListProperty property = new ListProperty();
        property.setValue(value);
        safeput(name, property);
    }

    public List getListValue(String name)
    {
        ListProperty prop = (ListProperty) safeget(name);
        if (prop == null) {
            return new ArrayList();
        } else {
            return (List) prop.getValue();
        }
    }

    public void setStringListValue(String name, List value)
    {
        ListProperty property = (ListProperty) safeget(name);
        if (property == null) {
            property = new StringListProperty();
        }
        property.setValue(value);
        safeput(name, property);
    }

    public void setDBStringListValue(String name, List value)
    {
        ListProperty property = (ListProperty) safeget(name);
        if (property == null) {
            property = new DBStringListProperty();
        }
        property.setValue(value);
        safeput(name, property);
    }

    // These functions should not be used
    // but instead our own implementation
    private Map<String, Object> getFields()
    {
        return this.fields;
    }

    public void setFields(Map fields)
    {
        this.fields = fields;
    }

    public PropertyInterface getField(String name)
    {
        return (PropertyInterface) this.fields.get(name);
    }

    public void addField(String name, PropertyInterface element)
    {
        this.fields.put(name, element);
    }

    public void removeField(String name)
    {
        Object field = safeget(name);
        if (field != null) {
            this.fields.remove(name);
            this.fieldsToRemove.add(field);
        }
    }

    public Collection getFieldList()
    {
        return this.fields.values();
    }

    public Set<String> getPropertyList()
    {
        return this.fields.keySet();
    }

    public Object[] getProperties()
    {
        Object[] array = getFields().values().toArray();
        return array;
    }

    public String[] getPropertyNames()
    {
        String[] array = getFields().keySet().toArray(new String[0]);
        return array;
    }

    /**
     * Return an iterator that will operate on a collection of values (as would be returned by getProperties or
     * getFieldList) sorted by their name (ElementInterface.getName()).
     */
    public Iterator getSortedIterator()
    {
        Iterator it = null;
        try {
            // Use getProperties to get the values in list form (rather than as generic collection)
            List propList = Arrays.asList(getProperties());

            // Use the element comparator to sort the properties by name (based on ElementInterface)
            Collections.sort(propList, new ElementComparator());

            // Iterate over the sorted property list
            it = propList.iterator();
        } catch (ClassCastException ccex) {
            // If sorting by the comparator resulted in a ClassCastException (possible),
            // iterate over the generic collection of values.
            it = getFieldList().iterator();
        }

        return it;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseElement#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object coll)
    {
        if (!super.equals(coll)) {
            return false;
        }
        BaseCollection collection = (BaseCollection) coll;
        if (collection.getClassName() == null) {
            if (getClassName() != null) {
                return false;
            }
        } else if (!collection.getClassName().equals(getClassName())) {
            return false;
        }

        if (getFields().size() != collection.getFields().size()) {
            return false;
        }

        for (Map.Entry<String, Object> entry : getFields().entrySet()) {
            Object prop = entry.getValue();
            Object prop2 = collection.getFields().get(entry.getKey());
            if (!prop.equals(prop2)) {
                return false;
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseElement#clone()
     */
    @Override
    public Object clone()
    {
        BaseCollection collection = (BaseCollection) super.clone();
        collection.setClassName(getClassName());
        collection.setNumber(getNumber());
        Map fields = getFields();
        Map cfields = new HashMap();
        for (Object objEntry : fields.entrySet()) {
            Map.Entry entry = (Map.Entry) objEntry;
            PropertyInterface prop = (PropertyInterface) ((BaseElement) entry.getValue()).clone();
            prop.setObject(collection);
            cfields.put(entry.getKey(), prop);
        }
        collection.setFields(cfields);

        return collection;
    }

    public void merge(BaseObject object)
    {
        Iterator itfields = object.getPropertyList().iterator();
        while (itfields.hasNext()) {
            String name = (String) itfields.next();
            if (safeget(name) == null) {
                safeput(name, (PropertyInterface) ((BaseElement) object.safeget(name)).clone());
            }
        }
    }

    public List<ObjectDiff> getDiff(Object oldObject, XWikiContext context)
    {
        ArrayList<ObjectDiff> difflist = new ArrayList<ObjectDiff>();
        BaseCollection oldCollection = (BaseCollection) oldObject;
        // Iterate over the new properties first, to handle changed and added objects
        for (Object key : this.getFields().keySet()) {
            String propertyName = (String) key;
            BaseProperty newProperty = (BaseProperty) this.getFields().get(propertyName);
            BaseProperty oldProperty = (BaseProperty) oldCollection.getFields().get(propertyName);

            if (oldProperty == null) {
                // The property exist in the new object, but not in the old one
                if ((newProperty != null) && (!newProperty.toText().equals(""))) {
                    String newPropertyValue =
                        (newProperty.getValue() instanceof String) ? newProperty.toText()
                            : ((PropertyClass) getxWikiClass(context).getField(propertyName)).displayView(propertyName,
                                this, context);
                    difflist.add(new ObjectDiff(getClassName(), getNumber(), "added", propertyName, "",
                        newPropertyValue));
                }
            } else if (!oldProperty.toText().equals(((newProperty == null) ? "" : newProperty.toText()))) {
                // The property exists in both objects and is different
                BaseClass bclass = getxWikiClass(context);
                PropertyClass pclass = (PropertyClass) ((bclass == null) ? null : bclass.getField(propertyName));
                if (pclass != null) {
                    // Put the values as they would be displayed in the interface
                    String newPropertyValue =
                        (newProperty.getValue() instanceof String) ? newProperty.toText() : pclass.displayView(
                            propertyName, this, context);
                    String oldPropertyValue =
                        (oldProperty.getValue() instanceof String) ? oldProperty.toText() : pclass.displayView(
                            propertyName, oldCollection, context);
                    difflist.add(new ObjectDiff(getClassName(), getNumber(), "changed", propertyName, oldPropertyValue,
                        newPropertyValue));
                } else {
                    // Cannot get property definition, so use the plain value
                    difflist.add(new ObjectDiff(getClassName(), getNumber(), "changed", propertyName, oldProperty
                        .toText(), newProperty.toText()));
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
                if ((oldProperty != null) && (!oldProperty.toText().equals(""))) {
                    BaseClass bclass = oldCollection.getxWikiClass(context);
                    PropertyClass pclass = (PropertyClass) ((bclass == null) ? null : bclass.getField(propertyName));
                    if (pclass != null) {
                        // Put the values as they would be displayed in the interface
                        String oldPropertyValue =
                            (oldProperty.getValue() instanceof String) ? oldProperty.toText() : pclass.displayView(
                                propertyName, oldCollection, context);
                        difflist.add(new ObjectDiff(oldCollection.getClassName(), oldCollection.getNumber(), "removed",
                            propertyName, oldPropertyValue, ""));
                    } else {
                        // Cannot get property definition, so use the plain value
                        difflist.add(new ObjectDiff(oldCollection.getClassName(), oldCollection.getNumber(), "removed",
                            propertyName, oldProperty.toText(), ""));
                    }
                }
            }
        }

        return difflist;
    }

    public List getFieldsToRemove()
    {
        return this.fieldsToRemove;
    }

    public void setFieldsToRemove(List fieldsToRemove)
    {
        this.fieldsToRemove = fieldsToRemove;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.ObjectInterface#toXML(com.xpn.xwiki.objects.classes.BaseClass)
     */
    public abstract Element toXML(BaseClass bclass);

    public String toXMLString()
    {
        Document doc = new DOMDocument();
        doc.setRootElement(toXML(null));
        OutputFormat outputFormat = new OutputFormat("", true);
        StringWriter out = new StringWriter();
        XMLWriter writer = new XMLWriter(out, outputFormat);
        try {
            writer.write(doc);
            return out.toString();
        } catch (IOException e) {
            e.printStackTrace();

            return "";
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return toXMLString();
    }

    public Map<String, Object> getCustomMappingMap() throws XWikiException
    {
        Map<String, Object> map = new HashMap<String, Object>();
        for (String name : this.fields.keySet()) {
            BaseProperty property = (BaseProperty) get(name);
            map.put(name, property.getCustomMappingValue());
        }
        map.put("id", getId());

        return map;
    }
}
