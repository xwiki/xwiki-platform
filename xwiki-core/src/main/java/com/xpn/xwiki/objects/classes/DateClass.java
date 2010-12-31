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

package com.xpn.xwiki.objects.classes;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ecs.xhtml.input;
import org.dom4j.Element;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.plugin.query.XWikiQuery;
import com.xpn.xwiki.web.XWikiMessageTool;

public class DateClass extends PropertyClass
{
    private static final Log LOG = LogFactory.getLog(DateClass.class);

    public DateClass(PropertyMetaClass wclass)
    {
        super("date", "Date", wclass);
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

    @Override
    public void displaySearch(StringBuffer buffer, String name, String prefix, XWikiCriteria criteria,
        XWikiContext context)
    {
        input input1 = new input();
        input1.setType("text");
        input1.setName(prefix + name + "_morethan");
        input1.setID(prefix + name);
        input1.setSize(getSize());
        String fieldFullName = getFieldFullName();

        Date value = (Date) criteria.getParameter(fieldFullName + "_morethan");
        if (value != null) {
            DateProperty dprop = new DateProperty();
            dprop.setValue(value);
            input1.setValue(toFormString(dprop));
        }

        input input2 = new input();

        input2.setType("text");
        input2.setName(prefix + name + "_lessthan");
        input2.setID(prefix + name);
        input2.setSize(getSize());
        value = (Date) criteria.getParameter(fieldFullName + "_lessthan");
        if (value != null) {
            DateProperty dprop = new DateProperty();
            dprop.setValue(value);
            input2.setValue(toFormString(dprop));
        }

        XWikiMessageTool msg = context.getMessageTool();
        buffer.append((msg == null) ? "from" : msg.get("from"));
        buffer.append(input1.toString());
        buffer.append((msg == null) ? "from" : msg.get("to"));
        buffer.append(input2.toString());
    }

    @Override
    public void fromSearchMap(XWikiQuery query, Map<String, String[]> map)
    {
        String[] data = map.get("");
        if ((data != null) && (data.length == 1)) {
            query.setParam(getObject().getName() + "_" + getName(), fromString(data[0]).getValue());
        } else {
            data = map.get("lessthan");
            if ((data != null) && (data.length == 1)) {
                query.setParam(getObject().getName() + "_" + getName() + "_lessthan", fromString(data[0]).getValue());
            }
            data = map.get("morethan");
            if ((data != null) && (data.length == 1)) {
                query.setParam(getObject().getName() + "_" + getName() + "_morethan", fromString(data[0]).getValue());
            }

        }
    }
}
