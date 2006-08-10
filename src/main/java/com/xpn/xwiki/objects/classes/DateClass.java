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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.ecs.xhtml.input;
import org.dom4j.Element;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

public class DateClass  extends PropertyClass {

    public DateClass(PropertyMetaClass wclass) {
        super("date", "Date", wclass);
        setSize(20);
        setDateFormat("dd/MM/yyyy HH:mm:ss");
        setEmptyIsToday(1);
    }

    public DateClass() {
        this(null);
    }

    public int getSize() {
        return getIntValue("size");
    }

    public void setSize(int size) {
        setIntValue("size", size);
    }

    public int getEmptyIsToday() {
        return getIntValue("emptyIsToday");
    }

    public void setEmptyIsToday(int emptyIsToday) {
        setIntValue("emptyIsToday", emptyIsToday);
    }

    public String getDateFormat() {
        return getStringValue("dateFormat");
    }

    public void setDateFormat(String dformat) {
        setStringValue("dateFormat", dformat);
    }

    public BaseProperty fromString(String value) {
        BaseProperty property = newProperty();

        if ((value==null)||(value.equals(""))) {
            property.setValue(new Date());
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

    public BaseProperty newProperty() {
        BaseProperty property = new DateProperty();
        property.setName(getName());
        return property;
    }

    public String toFormString(BaseProperty property) {
        SimpleDateFormat sdf = new SimpleDateFormat(getDateFormat());
        return sdf.format(property.getValue());
    }

    public BaseProperty newPropertyfromXML(Element ppcel) {
        String value = ppcel.getText();
        BaseProperty property = newProperty();

        if ((value==null)||(value.equals(""))) {
            property.setValue(new Date());
            return property;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
            property.setValue(sdf.parse(value));
        } catch (ParseException e) {
            try {
                e.printStackTrace();
                SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US);
                property.setValue(sdf.parse(value));
            } catch (ParseException e2) {
                e2.printStackTrace();
                property.setValue(new Date());
            }
        }
        return property;
    }

    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        BaseProperty prop = (BaseProperty) object.safeget(name);
        buffer.append(toFormString(prop));
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        input input = new input();

        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop!=null) input.setValue(toFormString(prop));

        input.setType("text");
        input.setName(prefix + name);
        input.setID(prefix + name);
        input.setSize(getSize());
        buffer.append(input.toString());
    }
}
