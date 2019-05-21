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
package org.xwiki.rest.model.jaxb;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Holds the value of a class property and its associated meta data (e.g. label, icon, count).
 * <p>
 * We don't generate this class from the schema because we need the {@link XmlJavaTypeAdapter} annotation on the
 * {@link #metaData} field. We tried various ways to add the {@link XmlJavaTypeAdapter} annotation to the
 * schema-generated class from the separate bindings file but none succeeded.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PropertyValue", propOrder = {"value", "metaData"})
public class PropertyValue
{
    @XmlElement(required = true)
    protected java.lang.Object value;

    @XmlElement(required = false)
    @XmlJavaTypeAdapter(MapAdapter.class)
    protected java.util.Map<String, java.lang.Object> metaData = new HashMap<>();

    /**
     * Default constructor.
     */
    public PropertyValue()
    {
    }

    /**
     * Creates a new instance with the specified value.
     * 
     * @param value the actual property value
     */
    public PropertyValue(java.lang.Object value)
    {
        this.value = value;
    }

    /**
     * @return the property value
     */
    public java.lang.Object getValue()
    {
        return value;
    }

    /**
     * Sets the property value.
     * 
     * @param value the new property value
     */
    public void setValue(java.lang.Object value)
    {
        this.value = value;
    }

    /**
     * @return the meta data associated with the property value
     */
    public java.util.Map<String, java.lang.Object> getMetaData()
    {
        return metaData;
    }

    /**
     * Sets the meta data associated with the property value.
     * 
     * @param value the new meta data
     */
    public void setMetaData(java.util.Map<String, java.lang.Object> value)
    {
        this.metaData = value;
    }

    @Override
    public boolean equals(java.lang.Object o)
    {
        if (o instanceof PropertyValue) {
            PropertyValue other = (PropertyValue) o;

            boolean equals;
            if (this.getMetaData() != null) {
                equals = this.getMetaData().equals(other.getMetaData());
            } else {
                equals = other.getMetaData() == null;
            }

            if (this.getValue() != null) {
                equals &= this.getValue().equals(other.getValue());
            } else {
                equals &= other.getValue() == null;
            }
            return equals;
        }
        return false;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("PropertyValue[");
        if (metaData != null) {
            builder.append("metadata=");
            builder.append(metaData.toString());
            builder.append(",");
        }
        if (value != null) {
            builder.append("value=");
            builder.append(value.toString());
        }
        builder.append("]");
        return builder.toString();
    }
}
