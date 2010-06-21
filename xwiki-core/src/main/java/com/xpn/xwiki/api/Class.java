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
package com.xpn.xwiki.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;

public class Class extends Collection
{
    public Class(BaseClass obj, XWikiContext context)
    {
        super(obj, context);
    }

    protected BaseClass getBaseClass()
    {
        return (BaseClass) getCollection();
    }

    /**
     * Returns a String table of the property names.
     * 
     * @see com.xpn.xwiki.api.Collection#getPropertyNames()
     */
    @Override
    public java.lang.Object[] getPropertyNames()
    {
        Element[] properties = getProperties();
        if (properties == null) {
            return super.getPropertyNames();
        }
        String[] props = new String[properties.length];
        for (int i = 0; i < properties.length; i++) {
            String propname = properties[i].getName();
            props[i] = propname;
        }
        return props;
    }

    /**
     * Get the names of the class properties that are enabled.
     * 
     * @return a list of enabled property names
     * @see #getEnabledProperties()
     * @see PropertyClass#isDisabled()
     * @since 2.4M2
     */
    public List<String> getEnabledPropertyNames()
    {
        List<com.xpn.xwiki.objects.classes.PropertyClass> properties = this.getBaseClass().getEnabledProperties();
        List<String> result = new ArrayList<String>(properties.size());
        for (com.xpn.xwiki.objects.classes.PropertyClass property : properties) {
            if (property != null) {
                result.add(property.getName());
            }
        }
        return result;
    }

    /**
     * Get the names of the class properties that are disabled.
     * 
     * @return a list of disabled property names
     * @see #getDisabledProperties()
     * @see PropertyClass#isDisabled()
     * @since 2.4M2
     */
    public List<String> getDisabledPropertyNames()
    {
        List<com.xpn.xwiki.objects.classes.PropertyClass> properties = this.getBaseClass().getDisabledProperties();
        List<String> result = new ArrayList<String>(properties.size());
        for (com.xpn.xwiki.objects.classes.PropertyClass property : properties) {
            if (property != null) {
                result.add(property.getName());
            }
        }
        return result;
    }

    /**
     * Get the names of the class properties that are disabled, and exist in the given object. This list is a subset of
     * all the disabled properties in a class, since the object could have been created and stored before some of the
     * class properties were added.
     * 
     * @param object the instance of this class where the disabled properties must exist
     * @return a list of disabled property names
     * @see #getDisabledObjectProperties(Object)
     * @see PropertyClass#isDisabled()
     * @since 2.4M2
     */
    public List<String> getDisabledObjectPropertyNames(Object object)
    {
        List<com.xpn.xwiki.objects.classes.PropertyClass> properties =
            this.getBaseClass().getDisabledObjectProperties(object.getBaseObject());
        List<String> result = new ArrayList<String>(properties.size());
        for (com.xpn.xwiki.objects.classes.PropertyClass property : properties) {
            if (property != null) {
                result.add(property.getName());
            }
        }
        return result;
    }

    /**
     * Get the names of deprecated properties of the given object compared to the class. A deprecated property is a
     * property which exists in the Object but doesn't exist anymore in the Class. This is used for synchronization of
     * existing or imported Objects with respect to the modifications of their associated Class.
     * 
     * @param object the instance of this class where to look for undefined properties
     * @return a list of deprecated property names
     * @see #getDeprecatedObjectProperties(Object)
     * @since 2.4M2
     */
    public List<String> getDeprecatedObjectPropertyNames(Object object)
    {
        List<BaseProperty> properties = this.getBaseClass().getDeprecatedObjectProperties(object.getBaseObject());
        List<String> result = new ArrayList<String>(properties.size());
        for (BaseProperty property : properties) {
            if (property != null) {
                result.add(property.getName());
            }
        }
        return result;
    }

    /**
     * @return an array with the properties of the class
     */
    @Override
    public Element[] getProperties()
    {
        @SuppressWarnings("unchecked")
        java.util.Collection<com.xpn.xwiki.objects.classes.PropertyClass> coll = getCollection().getFieldList();
        if (coll == null) {
            return null;
        }
        PropertyClass[] properties = new PropertyClass[coll.size()];
        int i = 0;
        for (com.xpn.xwiki.objects.classes.PropertyClass prop : coll) {
            properties[i++] = new PropertyClass(prop, getXWikiContext());
        }
        Arrays.sort(properties, new PropertyComparator());
        return properties;
    }

    /**
     * Get the list of enabled (the default, normal state) property definitions that exist in this class.
     * 
     * @return a list containing the enabled properties of the class
     * @see PropertyClass#isDisabled()
     * @see #getEnabledPropertyNames()
     * @since 2.4M2
     */
    public List<PropertyClass> getEnabledProperties()
    {
        List<com.xpn.xwiki.objects.classes.PropertyClass> enabledProperties =
            getBaseClass().getEnabledProperties();

        List<PropertyClass> result = new ArrayList<PropertyClass>(enabledProperties.size());

        for (com.xpn.xwiki.objects.classes.PropertyClass property : enabledProperties) {
            result.add(new PropertyClass(property, getXWikiContext()));
        }

        return result;
    }

    /**
     * Get the list of disabled property definitions that exist in this class.
     * 
     * @return a list containing the disabled properties of the class
     * @see PropertyClass#isDisabled()
     * @see #getDisabledPropertyNames()
     * @since 2.4M2
     */
    public List<PropertyClass> getDisabledProperties()
    {
        List<com.xpn.xwiki.objects.classes.PropertyClass> disabledProperties =
            getBaseClass().getDisabledProperties();

        List<PropertyClass> result = new ArrayList<PropertyClass>(disabledProperties.size());

        for (com.xpn.xwiki.objects.classes.PropertyClass property : disabledProperties) {
            result.add(new PropertyClass(property, getXWikiContext()));
        }

        return result;
    }

    /**
     * Get the list of disabled properties that exist in a given object. This list is a subset of all the disabled
     * properties in a class, since the object could have been created and stored before some of the class properties
     * were added.
     * 
     * @param object the instance of this class where the disabled properties must exist
     * @return a list containing the disabled properties of the given object
     * @see PropertyClass#isDisabled()
     * @see #getDisabledObjectPropertyNames(Object)
     * @since 2.4M2
     */
    public List<PropertyClass> getDisabledObjectProperties(Object object)
    {
        List<com.xpn.xwiki.objects.classes.PropertyClass> disabledObjectProperties =
            getBaseClass().getDisabledObjectProperties(object.getBaseObject());

        List<PropertyClass> result = new ArrayList<PropertyClass>(disabledObjectProperties.size());
        for (com.xpn.xwiki.objects.classes.PropertyClass property : disabledObjectProperties) {
            result.add(new PropertyClass(property, getXWikiContext()));
        }

        return result;
    }

    /**
     * Retrieves deprecated properties of the given object compared to the class. A deprecated property is a property
     * which exists in the Object but doesn't exist anymore in the Class. This is used for synchronization of existing
     * or imported Objects with respect to the modifications of their associated Class.
     * 
     * @param object the instance of this class where to look for undefined properties
     * @return a list containing the properties of the object which don't exist in the class
     * @see #getDeprecatedObjectPropertyNames(Object)
     * @since 2.4M2
     */
    public List<Property> getDeprecatedObjectProperties(Object object)
    {
        List<BaseProperty> deprecatedObjectProperties =
            getBaseClass().getDeprecatedObjectProperties(object.getBaseObject());

        List<Property> result = new ArrayList<Property>(deprecatedObjectProperties.size());
        for (BaseProperty property : deprecatedObjectProperties) {
            result.add(new Property(property, getXWikiContext()));
        }

        return result;
    }

    /**
     * @param name the name of the element
     * @return the PropertyClass for the given name
     * @see PropertyClass
     * @see Element
     */
    public Element get(String name)
    {
        com.xpn.xwiki.objects.classes.PropertyClass property =
            (com.xpn.xwiki.objects.classes.PropertyClass) getCollection().safeget(name);
        if (property != null) {
            return new PropertyClass(property, getXWikiContext());
        }
        return null;
    }

    /**
     * @return the BaseClass (without the wrapping) if you have the programming right.
     */
    public BaseClass getXWikiClass()
    {
        if (hasProgrammingRights()) {
            return (BaseClass) getCollection();
        } else {
            return null;
        }
    }

    /**
     * @return a new object from this class
     */
    public Object newObject() throws XWikiException
    {
        BaseObject obj = (BaseObject) getBaseClass().newObject(getXWikiContext());
        return obj.newObjectApi(obj, getXWikiContext());
    }
}

class PropertyComparator implements Comparator<PropertyClass>
{
    public int compare(PropertyClass o1, PropertyClass o2)
    {
        return o1.getNumber() - o2.getNumber();
    }
}
