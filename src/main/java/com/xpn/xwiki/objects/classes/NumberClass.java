/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author ludovic
 * @author sdumitriu
 */

package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.*;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.plugin.query.XWikiQuery;
import com.xpn.xwiki.web.XWikiMessageTool;
import org.apache.ecs.xhtml.input;

import java.util.List;
import java.util.Map;

public class NumberClass  extends PropertyClass {

    public NumberClass(PropertyMetaClass wclass) {
        super("number", "Number", wclass);
        setSize(30);
        setNumberType("long");
    }

    public NumberClass() {
        this(null);
    }

    public int getSize() {
        return getIntValue("size");
    }

    public void setSize(int size) {
        setIntValue("size", size);
    }

    public String getNumberType() {
        return getStringValue("numberType");
    }

    public void setNumberType(String ntype) {
        setStringValue("numberType", ntype);
    }

    public BaseProperty newProperty() {
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


    public BaseProperty fromString(String value) {
        BaseProperty property = newProperty();
        String ntype = getNumberType();
        Number nvalue = null;
        if (ntype.equals("integer")) {
            if ((value!=null)&&(!value.equals("")))
                nvalue = new Integer(value);
        } else if (ntype.equals("float")) {
            if ((value!=null)&&(!value.equals("")))
                nvalue = new Float(value);
        } else if (ntype.equals("double")) {
            if ((value!=null)&&(!value.equals("")))
                nvalue = new Double(value);
        } else {
            if ((value!=null)&&(!value.equals("")))
                nvalue = new Long(value);
        }
        property.setValue(nvalue);
        return property;
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        input input = new input();

        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop!=null) input.setValue(prop.toFormString());

        input.setType("text");
        input.setName(prefix + name);
        input.setID(prefix + name);
        input.setSize(getSize());
        buffer.append(input.toString());
    }

    public void displaySearch(StringBuffer buffer, String name, String prefix, XWikiCriteria criteria, XWikiContext context) {
        input input1 = new input();
        input1.setType("text");
        input1.setName(prefix + name + "_from");
        input1.setID(prefix + name);
        input1.setSize(getSize());
        String fieldFullName = getFieldFullName();
        String value = criteria.getParameter(fieldFullName + "_lessthan");
        if (value!=null)
         input1.setValue(value);

        input input2 = new input();

        input2.setType("text");
        input2.setName(prefix + name+ "_to");
        input2.setID(prefix + name);
        input2.setSize(getSize());
        value = criteria.getParameter(fieldFullName + "_morethan");
        if (value!=null)
         input2.setValue(value);

        XWikiMessageTool msg = ((XWikiMessageTool)context.get("msg"));
        buffer.append((msg==null) ? "from" : msg.get("from"));
        buffer.append(input1.toString());
        buffer.append((msg==null) ? "from" : msg.get("to"));
        buffer.append(input2.toString());
    }
    
    public void makeQuery(Map map, String prefix, XWikiCriteria query, List criteriaList) {
        Number value = (Number)map.get(prefix);
        if ((value!=null)&&(!value.equals(""))) {
         criteriaList.add("@f:" + getName() + "=" + value.toString());
         return;
        }

        value = (Number)map.get(prefix + "lessthan");
        if ((value!=null)&&(!value.equals(""))) {
         criteriaList.add("@f:" + getName() + "<" + value.toString());
         return;
        }

        value = (Number)map.get(prefix + "morethan");
        if ((value!=null)&&(!value.equals(""))) {
         criteriaList.add("@f:" + getName() + ">" + value.toString());
         return;
        }
    }

    public void fromSearchMap(XWikiQuery query, Map map) {
        String data[]  = (String[])map.get("");
        if ((data!=null)&&(data.length==1))
            query.setParam(getObject().getName() + "_" + getName(), fromString(data[0]).getValue());
        else {
            data  = (String[])map.get("lessthan");
            if ((data!=null)&&(data.length==1))
                query.setParam(getObject().getName() + "_" + getName() + "_lessthan", fromString(data[0]).getValue());
            data  = (String[])map.get("morethan");
            if ((data!=null)&&(data.length==1))
                query.setParam(getObject().getName() + "_" + getName() + "_morethan", fromString(data[0]).getValue());

        }
    }
}
