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

import java.text.NumberFormat;
import java.text.ParseException;

import org.apache.commons.lang3.StringUtils;
import org.apache.ecs.xhtml.input;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.xml.XMLAttributeValueFilter;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DoubleProperty;
import com.xpn.xwiki.objects.FloatProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.LongProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

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

    private static final long serialVersionUID = 1L;

    private static final String XCLASSNAME = "number";

    private static final String PARSING_WARNING_MSG = "Invalid number entered for property {} of class {}: {}";

    /** Logging helper object. */
    private static final Logger LOG = LoggerFactory.getLogger(NumberClass.class);

    public NumberClass(PropertyMetaClass wclass)
    {
        super(XCLASSNAME, "Number", wclass);
        setSize(30);
        setNumberType(TYPE_LONG);
    }

    public NumberClass()
    {
        this(null);
    }

    public int getSize()
    {
        return getIntValue("size");
    }

    public void setSize(int size)
    {
        setIntValue("size", size);
    }

    public String getNumberType()
    {
        return getStringValue("numberType");
    }

    public void setNumberType(String ntype)
    {
        setStringValue("numberType", ntype);
    }

    @Override
    public BaseProperty newProperty()
    {
        String ntype = getNumberType();
        BaseProperty property;
        if (ntype.equals(TYPE_INTEGER)) {
            property = new IntegerProperty();
        } else if (ntype.equals(TYPE_FLOAT)) {
            property = new FloatProperty();
        } else if (ntype.equals(TYPE_DOUBLE)) {
            property = new DoubleProperty();
        } else {
            property = new LongProperty();
        }
        property.setName(getName());
        return property;
    }

    @Override
    public BaseProperty fromString(String value)
    {
        BaseProperty property = newProperty();
        String ntype = getNumberType();
        Number nvalue = null;
        boolean valueBlank = StringUtils.isBlank(value);

        if (!valueBlank) {
            try {
                NumberFormat numberFormat = NumberFormat.getNumberInstance(getOwnerDocument().getLocale());
                Number number = numberFormat.parse(value);

                if (ntype.equals(TYPE_INTEGER) && number.intValue() == Integer.parseInt(number.toString())) {
                    nvalue = number.intValue();
                } else if (ntype.equals(TYPE_FLOAT) && number.floatValue() == Float.parseFloat(number.toString())) {
                    nvalue = number.floatValue();
                } else if (ntype.equals(TYPE_DOUBLE) && number.doubleValue() == Double.parseDouble(number.toString())) {
                    nvalue = number.doubleValue();
                } else if (ntype.equals(TYPE_LONG) && number.longValue() == Long.parseLong(number.toString())) {
                    nvalue = number.longValue();
                }
            } catch (ParseException | NumberFormatException e) {
                // We log a debug with the stack, just in case.
                LOG.debug(PARSING_WARNING_MSG, getName(), getObject(), value, e);
            }
        }

        if (nvalue == null && !valueBlank) {
            LOG.warn(PARSING_WARNING_MSG, getName(), getObject(), value);
            // Returning null makes sure that the old value (if one exists) will not be discarded/replaced
            return null;
        }

        property.setValue(nvalue);
        return property;
    }

    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context)
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
