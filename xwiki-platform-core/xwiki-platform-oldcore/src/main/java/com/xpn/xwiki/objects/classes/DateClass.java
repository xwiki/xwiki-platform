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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.ecs.xhtml.input;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

public class DateClass extends PropertyClass
{
    private static final String XCLASSNAME = "date";

    /** Logging helper object. */
    private static final Logger LOG = LoggerFactory.getLogger(DateClass.class);

    public DateClass(PropertyMetaClass wclass)
    {
        super(XCLASSNAME, "Date", wclass);
        setSize(20);
        setDateFormat("dd/MM/yyyy HH:mm:ss");
        setEmptyIsToday(1);
        setPicker(1);
    }

    public DateClass()
    {
        this(null);
    }

    public int getPicker()
    {
        return getIntValue("picker");
    }

    public void setPicker(int picker)
    {
        setIntValue("picker", picker);
    }

    public int getSize()
    {
        return getIntValue("size");
    }

    public void setSize(int size)
    {
        setIntValue("size", size);
    }

    public int getEmptyIsToday()
    {
        return getIntValue("emptyIsToday");
    }

    public void setEmptyIsToday(int emptyIsToday)
    {
        setIntValue("emptyIsToday", emptyIsToday);
    }

    public String getDateFormat()
    {
        return getStringValue("dateFormat");
    }

    public void setDateFormat(String dformat)
    {
        setStringValue("dateFormat", dformat);
    }

    /*
     * Returns the current default custom displayer for the PropertyClass
     * When it cannot find one for the current class it will call the same
     * function for the super class
     * This function should be implemented by any derivative PropertyClass
     * if this PropertyClass wants to have a default custom displayer
     */
    @Override
    public String getDefaultCustomDisplayer(XWikiContext context) {
        String customDisplayer = getDefaultCustomDisplayer(XCLASSNAME, context);
        if (customDisplayer==null)
            return super.getDefaultCustomDisplayer(context);
        else
            return customDisplayer;
    }

    @Override
    public BaseProperty fromString(String value)
    {
        BaseProperty property = newProperty();

        if ((value == null) || (value.equals(""))) {
            if (getEmptyIsToday() == 1) {
                property.setValue(new Date());
            }
            return property;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(getDateFormat());
            property.setValue(sdf.parse(value));
        } catch (ParseException e) {
            return null;
        }
        return property;
    }

    @Override
    public BaseProperty newProperty()
    {
        BaseProperty property = new DateProperty();
        property.setName(getName());
        return property;
    }

    public String toFormString(BaseProperty property)
    {
        if (property.getValue() == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(getDateFormat());
        return sdf.format(property.getValue());
    }

    @Override
    public BaseProperty newPropertyfromXML(Element ppcel)
    {
        String value = ppcel.getText();
        BaseProperty property = newProperty();

        if (StringUtils.isEmpty(value)) {
            return property;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        try {
            property.setValue(sdf.parse(value));
        } catch (ParseException e) {
            SimpleDateFormat sdf2 = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US);
            try {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to parse date [" + value + "] using format ["
                        + sdf.toString() + "]. Trying again with format ["
                        + sdf2.toString() + "]");
                }
                property.setValue(sdf2.parse(value));
            } catch (ParseException e2) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to parse date [" + value + "] using format ["
                        + sdf2.toString() + "]. Defaulting to the current date.");
                }
                property.setValue(new Date());
            }
        }
        return property;
    }

    @Override
    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context)
    {
        BaseProperty prop = (BaseProperty) object.safeget(name);
        buffer.append(toFormString(prop));
    }

    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context)
    {
        input input = new input();

        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop != null) {
            input.setValue(toFormString(prop));
        }

        input.setType("text");
        input.setName(prefix + name);
        input.setID(prefix + name);
        input.setSize(getSize());
        input.setDisabled(isDisabled());
        buffer.append(input.toString());
    }
}
