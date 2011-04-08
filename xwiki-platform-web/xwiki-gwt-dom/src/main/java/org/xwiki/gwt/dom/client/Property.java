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
package org.xwiki.gwt.dom.client;

/**
 * Style property.
 * 
 * @version $Id$
 */
public class Property
{
    /**
     * The name of this property, used in style sheets. The common CSS syntax is {@code propertyCSSName: value;}.
     */
    private final String cssName;

    /**
     * The name of this property, used in scripts. The common JavaScript syntax is {@code object.style.propertyJSName =
     * value}.
     */
    private final String jsName;

    /**
     * Flag that specifies if this property is inherited by child nodes.
     */
    private final boolean inheritable;

    /**
     * Flag that specifies if this property can have multiple values.
     */
    private final boolean multipleValue;

    /**
     * The default value of this property.
     */
    private final String defaultValue;

    /**
     * Creates a new style property with the given description.
     * 
     * @param cssName the name of property, used in style sheets
     * @param inheritable whether the property is inherited by child nodes or not
     * @param multipleValue whether the property can have multiple values or not
     * @param defaultValue the default value of the property
     */
    public Property(String cssName, boolean inheritable, boolean multipleValue, String defaultValue)
    {
        this(cssName, cssName, inheritable, multipleValue, defaultValue);
    }

    /**
     * Creates a new style property with the given description.
     * 
     * @param cssName the name of property, used in style sheets
     * @param jsName the name of this property, used in scripts
     * @param inheritable whether the property is inherited by child nodes or not
     * @param multipleValue whether the property can have multiple values or not
     * @param defaultValue the default value of the property
     */
    public Property(String cssName, String jsName, boolean inheritable, boolean multipleValue, String defaultValue)
    {
        this.cssName = cssName;
        this.jsName = jsName;
        this.inheritable = inheritable;
        this.multipleValue = multipleValue;
        this.defaultValue = defaultValue;
    }

    /**
     * @return the name of this property used in style sheets
     */
    public String getCSSName()
    {
        return cssName;
    }

    /**
     * @return the name of this property used in scripts
     */
    public String getJSName()
    {
        return jsName;
    }

    /**
     * @return {@code true} if this property is inherited by child nodes, {@code false} otherwise
     */
    public boolean isInheritable()
    {
        return inheritable;
    }

    /**
     * @return {@code true} if this property can have multiple values, {@code false} otherwise
     */
    public boolean isMultipleValue()
    {
        return multipleValue;
    }

    /**
     * @return the default value of this property
     */
    public String getDefaultValue()
    {
        return defaultValue;
    }
}
