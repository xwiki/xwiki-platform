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
package com.xpn.xwiki.objects.classes;

import org.apache.ecs.xhtml.input;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.xml.XMLAttributeValueFilter;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DoubleProperty;
import com.xpn.xwiki.objects.FloatProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.LongProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

/**
 * Class for providing fields holding number values.
 *
 * @version $Id$
 */
// FIXME: CyclomaticComplexity is 13 while it should be 10.
@SuppressWarnings("checkstyle:CyclomaticComplexity")
public class NumberClass extends PropertyClass
{
    /**
     * The type of the number is integer.
     * 
     * @since 15.4RC1
     */
    public static final String TYPE_INTEGER = "integer";

    /**
     * The type of the number is float.
     * 
     * @since 15.4RC1
     */
    public static final String TYPE_FLOAT = "float";

    /**
     * The type of the number is double.
     * 
     * @since 15.4RC1
     */
    public static final String TYPE_DOUBLE = "double";

    /**
     * The type of the number is long.
     * 
     * @since 15.4RC1
     */
    public static final String TYPE_LONG = "long";

    /**
     * The type used as a hint to find the class.
     * @since 18.2.0RC1
     */
    @Unstable
    public static final String PROPERTY_TYPE = "Number";

    private static final long serialVersionUID = 1L;

    private static final String XCLASSNAME = "number";
    private static final String SIZE = "size";
    private static final String NUMBER_TYPE = "numberType";

    /** Logging helper object. */
    private static final Logger LOG = LoggerFactory.getLogger(NumberClass.class);

    /**
     * Constructor with a meta class.
     * @param wclass the meta class to be used.
     */
    public NumberClass(PropertyMetaClass wclass)
    {
        super(XCLASSNAME, PROPERTY_TYPE, wclass);
        setSize(30);
        setNumberType(TYPE_LONG);
    }

    /**
     * Empty constructor with a null metaclass.
     */
    public NumberClass()
    {
        this(null);
    }

    /**
     * @return the size of the field.
     */
    public int getSize()
    {
        return getIntValue(SIZE);
    }

    /**
     * Set the size of the field.
     * @param size the requested size.
     */
    public void setSize(int size)
    {
        setIntValue(SIZE, size);
    }

    /**
     * @return the type of the number (check the constants for supported types)
     */
    public String getNumberType()
    {
        return getStringValue(NUMBER_TYPE);
    }

    /**
     * Set the type of the number, see the constants for supported types.
     * @param ntype the type of the number
     */
    public void setNumberType(String ntype)
    {
        setStringValue(NUMBER_TYPE, ntype);
    }

    @Override
    public BaseProperty newProperty()
    {
        String ntype = getNumberType();
        BaseProperty property;
        if (TYPE_INTEGER.equals(ntype)) {
            property = new IntegerProperty();
        } else if (TYPE_FLOAT.equals(ntype)) {
            property = new FloatProperty();
        } else if (TYPE_DOUBLE.equals(ntype)) {
            property = new DoubleProperty();
        } else {
            property = new LongProperty();
        }
        property.setName(getName());
        return property;
    }

    @Override
    public String getPropertyType()
    {
        return PROPERTY_TYPE;
    }

    @Override
    public BaseProperty fromString(String value) throws XWikiException
    {
        BaseProperty property = newProperty();
        String ntype = getNumberType();
        Number nvalue = null;

        try {
            if (TYPE_INTEGER.equals(ntype)) {
                if ((value != null) && (!value.isEmpty())) {
                    nvalue = Integer.valueOf(value);
                }
            } else if (TYPE_FLOAT.equals(ntype)) {
                if ((value != null) && (!value.isEmpty())) {
                    nvalue = Float.valueOf(value);
                }
            } else if (TYPE_DOUBLE.equals(ntype)) {
                if ((value != null) && (!value.isEmpty())) {
                    nvalue = Double.valueOf(value);
                }
            } else {
                if ((value != null) && (!value.isEmpty())) {
                    nvalue = Long.valueOf(value);
                }
            }
        } catch (NumberFormatException e) {
            throw new XWikiException(String.format("Error when parsing [%s] to type [%s]", value, ntype), e);
        }

        property.setValue(nvalue);
        return property;
    }

    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        input input = new input();
        input.setAttributeFilter(new XMLAttributeValueFilter());

        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop != null) {
            input.setValue(prop.toText());
        }

        input.setType("text");
        input.setName(prefix + name);
        input.setID(prefix + name);
        input.setSize(getSize());
        input.setDisabled(isDisabled());
        buffer.append(input.toString());
    }

}
