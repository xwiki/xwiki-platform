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

package com.xpn.xwiki.stats.impl;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.stats.impl.StatsUtil.PeriodType;

/**
 * Base class for all stored statistics object.
 * 
 * @version $Id$
 */
public class XWikiStats extends BaseCollection
{
    /**
     * The properties of statistics object.
     * 
     * @version $Id$
     */
    public enum Property
    {
        /**
         * The name of the object database property <code>period</code>.
         */
        period,

        /**
         * The name of the object database property <code>pageViews</code>.
         */
        pageViews
    }

    /**
     * The name of the XML node <code>object</code>.
     */
    private static final String XMLNODE_OBJECT = "object";

    /**
     * The name of the XML node <code>name</code>.
     */
    private static final String XMLNODE_NAME = "name";

    /**
     * The name of the XML node <code>number</code>.
     */
    private static final String XMLNODE_NUMBER = "number";

    /**
     * The name of the XML node <code>className</code>.
     */
    private static final String XMLNODE_CLASSNAME = "className";

    /**
     * The name of the XML node <code>property</code>.
     */
    private static final String XMLNODE_PROPERTY = "property";

    /**
     * Default constructor.
     */
    public XWikiStats()
    {
    }

    /**
     * @param periodDate the period date.
     * @param periodtype the period type.
     */
    public XWikiStats(Date periodDate, PeriodType periodtype)
    {
        setPeriod(StatsUtil.getPeriodAsInt(periodDate, periodtype));
    }

    /**
     * @return the time when statistic was stored.
     */
    public int getPeriod()
    {
        return getIntValue(Property.period.toString());
    }

    /**
     * @param period the time when statistic was stored.
     */
    public void setPeriod(int period)
    {
        setIntValue(Property.period.toString(), period);
    }

    /**
     * @return the counter of view action of this statistic.
     */
    public int getPageViews()
    {
        return getIntValue(Property.pageViews.toString());
    }

    /**
     * @param pageViews the counter of view action of this statistic.
     */
    public void setPageViews(int pageViews)
    {
        setIntValue(Property.pageViews.toString(), pageViews);
    }

    /**
     * Add 1 to the counter of view action of this statistic.
     */
    public void incPageViews()
    {
        setIntValue(Property.pageViews.toString(), getPageViews() + 1);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseCollection#hashCode()
     */
    @Override
    public int hashCode()
    {
        return (getName() + getClassName() + "_" + getNumber()).hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseCollection#setId(int)
     */
    @Override
    public void setId(int id)
    {
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseCollection#clone()
     */
    @Override
    public Object clone()
    {
        BaseCollection object = (BaseCollection) super.clone();
        return object;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseCollection#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        // Same Java object, they sure are equal
        if (this == obj) {
            return true;
        }

        if (!super.equals(obj)) {
            return false;
        }

        if (getNumber() != ((BaseCollection) obj).getNumber()) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseCollection#toXML(com.xpn.xwiki.objects.classes.BaseClass)
     */
    @Override
    public Element toXML(BaseClass bclass)
    {
        Element oel = new DOMElement(XMLNODE_OBJECT);

        // Add Class
        if (bclass != null) {
            if (bclass.getFieldList().size() > 0) {
                oel.add(bclass.toXML());
            }
        }

        Element el = new DOMElement(XMLNODE_NAME);
        el.addText(getName());
        oel.add(el);

        el = new DOMElement(XMLNODE_NUMBER);
        el.addText(getNumber() + "");
        oel.add(el);

        el = new DOMElement(XMLNODE_CLASSNAME);
        el.addText(getClassName());
        oel.add(el);

        for (Iterator< ? > it = getFieldList().iterator(); it.hasNext();) {
            Element pel = new DOMElement(XMLNODE_PROPERTY);
            PropertyInterface bprop = (PropertyInterface) it.next();
            pel.add(bprop.toXML());
            oel.add(pel);
        }

        return oel;
    }

    /**
     * Initialize statistics object from XML schema.
     * 
     * @param oel the XML root node containing statistics datas.
     * @throws XWikiException error when parsing XML schema.
     */
    public void fromXML(Element oel) throws XWikiException
    {
        Element cel = oel.element("class");
        BaseClass bclass = new BaseClass();
        if (cel != null) {
            bclass.fromXML(cel);
            setClassName(bclass.getName());
        }

        setName(oel.element(XMLNODE_NAME).getText());
        List< ? > list = oel.elements(XMLNODE_PROPERTY);
        for (int i = 0; i < list.size(); i++) {
            Element pcel = (Element) ((Element) list.get(i)).elements().get(0);
            String name = pcel.getName();
            PropertyClass pclass = (PropertyClass) bclass.get(name);
            if (pclass != null) {
                BaseProperty property = pclass.newPropertyfromXML(pcel);
                property.setName(name);
                property.setObject(this);
                safeput(name, property);
            }
        }
    }
}
