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
package com.xpn.xwiki.internal.store;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * Internal utility class used to convert property values when their type changes. This is useful:
 * <ul>
 * <li>when the property value store (e.g. the database table used to store the value) changes as a result of modifying
 * the property type settings (e.g. switching from single selection to multiple selection)</li>
 * <li>when the property type changes completely, e.g. by deleting the property and adding a new one with the same name
 * but with a different type</li>
 * </ul>
 * .
 * 
 * @version $Id$
 * @since 7.0M2
 */
@Component(roles = PropertyConverter.class)
@Singleton
public class PropertyConverter
{
    @Inject
    private Logger logger;

    /**
     * Converts the given property to the specified type.
     * 
     * @param storedProperty the property to convert
     * @param modifiedPropertyClass the new property type
     * @return the new (converted) property
     */
    public BaseProperty<?> convertProperty(BaseProperty<?> storedProperty, PropertyClass modifiedPropertyClass)
    {
        Object newValue = convertPropertyValue(storedProperty.getValue(), modifiedPropertyClass);
        BaseProperty<?> newProperty = null;
        if (newValue != null) {
            newProperty = modifiedPropertyClass.newProperty();
            try {
                // Try to set the converted value.
                newProperty.setValue(newValue);
            } catch (Exception e) {
                // Looks like the conversion didn't succeed. Let's try to compute the value from string.
                // This should return null if the new value cannot be parsed from string.
                newProperty = modifiedPropertyClass.fromString(storedProperty.toText());
            }
            if (newProperty != null) {
                newProperty.setId(storedProperty.getId());
                newProperty.setName(storedProperty.getName());
            } else {
                // The stored value couldn't be converted to the new property type.
                this.logger.warn("Incompatible data migration when changing field [{}] of class [{}]",
                    modifiedPropertyClass.getName(), modifiedPropertyClass.getClassName());
            }
        } else {
            // If the new value is null then it means the property is not set (it can be removed).
        }
        return newProperty;
    }

    private Object convertPropertyValue(Object storedValue, PropertyClass modifiedPropertyClass)
    {
        if (modifiedPropertyClass instanceof ListClass) {
            return convertPropertyValue(storedValue, (ListClass) modifiedPropertyClass);
        } else if (modifiedPropertyClass instanceof NumberClass) {
            return convertPropertyValue(storedValue, (NumberClass) modifiedPropertyClass);
        } else {
            // Return the stored value if no specific converter has been found. We will attempt to convert the stored
            // value through string deserialization later.
            return storedValue;
        }
    }

    private Object convertPropertyValue(Object storedValue, ListClass modifiedListClass)
    {
        if (modifiedListClass.isMultiSelect() && !(storedValue instanceof List) && storedValue != null) {
            // The property has multiple selection so the value must be a list.
            return Arrays.asList(storedValue);
        } else if (!modifiedListClass.isMultiSelect() && storedValue instanceof List) {
            // The property has single selection so the value must be a string.
            @SuppressWarnings("unchecked")
            List<String> oldValues = (List<String>) storedValue;
            return oldValues.isEmpty() ? null : oldValues.get(0);
        }
        // The stored value doesn't have to be updated.
        return storedValue;
    }

    private Object convertPropertyValue(Object storedValue, NumberClass modifiedNumberClass)
    {
        Object newValue = storedValue;
        if (storedValue instanceof Number) {
            // Convert the stored value to the new number type.
            Number storedNumber = (Number) storedValue;
            String newNumberType = modifiedNumberClass.getNumberType();
            if ("integer".equals(newNumberType)) {
                newValue = Integer.valueOf(storedNumber.intValue());
            } else if ("float".equals(newNumberType)) {
                newValue = Float.valueOf(storedNumber.floatValue());
            } else if ("double".equals(newNumberType)) {
                newValue = Double.valueOf(storedNumber.doubleValue());
            } else if ("long".equals(newNumberType)) {
                newValue = Long.valueOf(storedNumber.longValue());
            }
        } else {
            // If we get here then either the stored value is null or the property type has changed. This can happen for
            // instance if you remove a property and then add a new one with the same name but with a different type. We
            // return the stored value here but we'll try later to convert it through string deserialization.
        }
        return newValue;
    }
}
