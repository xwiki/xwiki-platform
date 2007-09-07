/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
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
 */

package com.xpn.xwiki.stats.impl;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

import java.util.*;

public class XWikiStats extends BaseCollection {
    public static int PERIOD_MONTH = 0;
    public static int PERIOD_DAY = 1;

    public XWikiStats() {
        super();
    }

    public XWikiStats(Date period, int periodtype) {
        setPeriod(getPeriodAsInt(period, periodtype));
    }

    public int getPeriod() {
        return getIntValue("period");
    }

    public void setPeriod(int period) {
        setIntValue("period", period);
    }

    public int getPeriodAsInt(Date date, int type) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        if (type == PERIOD_MONTH)
         return cal.get(Calendar.YEAR) * 100 + (cal.get(Calendar.MONTH)+1);
       else
         return cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH)+1) * 100 + (cal.get(Calendar.DAY_OF_MONTH)+1);
    }

    public int getPageViews() {
        return getIntValue("pageViews");
    }

    public void  setPageViews(int pageViews) {
        setIntValue("pageViews", pageViews);
    }

    public void  incPageViews() {
        setIntValue("pageViews", getPageViews() + 1);
    }

    public int hashCode() {
        String str = getName()+getClassName();
        int nb = getNumber();
        if (nb>0)
            str += "_" + nb;
        return str.hashCode();
    }

    public void setId(int id) {
    }

    public Object clone() {
        BaseCollection object = (BaseCollection) super.clone();
        return object;
    }

    public boolean equals(Object obj) {
        if (!super.equals(obj))
            return false;

        if (getNumber()!=((BaseCollection)obj).getNumber())
            return false;

        return true;
    }

    public Element toXML(BaseClass bclass) {
        Element oel = new DOMElement("object");

        // Add Class
        if (bclass!=null) {
        Collection fields = bclass.getFieldList();
        if (fields.size()>0) {
            oel.add(bclass.toXML());
          }
        }

        Element el = new DOMElement("name");
        el.addText(getName());
        oel.add(el);

        el = new DOMElement("number");
        el.addText(getNumber() + "");
        oel.add(el);

        el = new DOMElement("className");
        el.addText(getClassName());
        oel.add(el);

        Iterator it = getFieldList().iterator();
        while (it.hasNext()) {
            Element pel = new DOMElement("property");
            PropertyInterface bprop = (PropertyInterface)it.next();
            pel.add(bprop.toXML());
            oel.add(pel);
        }
        return oel;
    }

    public void fromXML(Element oel) throws XWikiException {
        Element cel = oel.element("class");
        BaseClass bclass = new BaseClass();
        if (cel!=null) {
            bclass.fromXML(cel);
            setClassName(bclass.getName());
        }

        setName(oel.element("name").getText());
        List list = oel.elements("property");
        for (int i=0;i<list.size();i++) {
            Element pcel = (Element)((Element) list.get(i)).elements().get(0);
            String name = pcel.getName();
            PropertyClass pclass = (PropertyClass) bclass.get(name);
            if (pclass!=null) {
                BaseProperty property = pclass.newPropertyfromXML(pcel);
                property.setName(name);
                property.setObject(this);
                safeput(name, property);
            }
        }
    }
}
