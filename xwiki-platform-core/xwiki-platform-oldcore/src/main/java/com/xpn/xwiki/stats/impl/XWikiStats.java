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
package com.xpn.xwiki.stats.impl;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.stats.impl.StatsUtil.PeriodType;
import com.xpn.xwiki.web.Utils;

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

    private static final long serialVersionUID = 1L;

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

    /** Resolve names into reference for uid string serialization. */
    private final EntityReferenceResolver<String> resolver = Utils.getComponent(EntityReferenceResolver.TYPE_STRING);

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

    @Override
    protected String getLocalKey()
    {
        StringBuilder sb = new StringBuilder(64);

        // The R40000XWIKI6990DataMigration use a stubbed class to provide the above two values. If the ids depends
        // on other non-static requirements, it should be adapted accordingly.
        String name = getName();
        int nb = getNumber();

        if (!StringUtils.isEmpty(name)) {
            // TODO: Refactor to get the original reference and fix the confusion when a space contains escaped chars
            EntityReference ref = this.resolver.resolve(name, EntityType.DOCUMENT);
            if (ref.getName().equals(name)) {
                ref = new EntityReference(name, EntityType.SPACE);
            }
            sb.append(getLocalUidStringEntityReferenceSerializer().serialize(ref));
        }

        // if number used, serialize it as well. It may happened that the hash is 0, but this is really unlikely
        // and it will not hurt anyway.
        if (nb != 0) {
            // TODO: Avoid the hashed number, and use the original info (referer, period, etc...)
            String str = Integer.toString(nb);
            sb.append(str.length()).append(':').append(str);
        }

        return sb.toString();
    }

    // Satisfy checkstyle !
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

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

    @Override
    // TODO: implement an EntityEventGenerator for XWikiStats
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

        for (Iterator<?> it = getFieldList().iterator(); it.hasNext();) {
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
    @Override
    public void fromXML(Element oel) throws XWikiException
    {
        Element cel = oel.element("class");
        BaseClass bclass = new BaseClass();
        if (cel != null) {
            bclass.fromXML(cel);
            setClassName(bclass.getName());
        }

        setName(oel.element(XMLNODE_NAME).getText());
        List<?> list = oel.elements(XMLNODE_PROPERTY);
        for (int i = 0; i < list.size(); i++) {
            Element pcel = ((Element) list.get(i)).elements().get(0);
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
