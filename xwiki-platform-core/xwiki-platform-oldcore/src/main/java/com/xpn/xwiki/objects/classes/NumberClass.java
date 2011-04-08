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

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ecs.xhtml.input;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DoubleProperty;
import com.xpn.xwiki.objects.FloatProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.LongProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.plugin.query.XWikiQuery;
import com.xpn.xwiki.web.XWikiMessageTool;

public class NumberClass extends PropertyClass
{
    /** Logging helper object. */
    private static final Log LOG = LogFactory.getLog(NumberClass.class);

    public NumberClass(PropertyMetaClass wclass)
    {
        super("number", "Number", wclass);
        setSize(30);
        setNumberType("long");
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
        if (ntype.equals("integer")) {
            property = new IntegerProperty();
        } else if (ntype.equals("float")) {
            property = new FloatProperty();
        } else if (ntype.equals("double")) {
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

        try {
            if (ntype.equals("integer")) {
                if ((value != null) && (!value.equals(""))) {
                    nvalue = new Integer(value);
                }
            } else if (ntype.equals("float")) {
                if ((value != null) && (!value.equals(""))) {
                    nvalue = new Float(value);
                }
            } else if (ntype.equals("double")) {
                if ((value != null) && (!value.equals(""))) {
                    nvalue = new Double(value);
                }
            } else {
                if ((value != null) && (!value.equals(""))) {
                    nvalue = new Long(value);
                }
            }
        } catch (NumberFormatException e) {
            LOG.warn("Invalid number entered for property " + getName() + " of class " + getObject().getName() + ": "
                + value);
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

        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop != null) {
            input.setValue(prop.toFormString());
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
        Number value = (Number) criteria.getParameter(fieldFullName + "_morethan");
        if (value != null) {
            input1.setValue(value.toString());
        }

        input input2 = new input();

        input2.setType("text");
        input2.setName(prefix + name + "_lessthan");
        input2.setID(prefix + name);
        input2.setSize(getSize());
        value = (Number) criteria.getParameter(fieldFullName + "_lessthan");
        if (value != null) {
            input2.setValue(value.toString());
        }

        XWikiMessageTool msg = context.getMessageTool();
        buffer.append((msg == null) ? "from" : msg.get("from"));
        buffer.append(input1.toString());
        buffer.append((msg == null) ? "from" : msg.get("to"));
        buffer.append(input2.toString());
    }

    @Override
    public void makeQuery(Map<String, Object> map, String prefix, XWikiCriteria query, List<String> criteriaList)
    {
        Number value = (Number) map.get(prefix);
        if ((value != null) && (!value.equals(""))) {
            criteriaList.add(getFullQueryPropertyName() + "=" + value.toString());
            return;
        }

        value = (Number) map.get(prefix + "lessthan");
        if ((value != null) && (!value.equals(""))) {
            criteriaList.add(getFullQueryPropertyName() + "<=" + value.toString());
            return;
        }

        value = (Number) map.get(prefix + "morethan");
        if ((value != null) && (!value.equals(""))) {
            criteriaList.add(getFullQueryPropertyName() + ">=" + value.toString());
            return;
        }
    }

    @Override
    public void fromSearchMap(XWikiQuery query, Map<String, String[]> map)
    {
        String data[] = map.get("");
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
