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

package com.xpn.xwiki.objects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.ecs.filter.CharacterFilter;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.hibernate.collection.PersistentCollection;

public class ListProperty extends BaseProperty implements Cloneable
{
    protected List<String> list = new ArrayList<String>();

    private String formStringSeparator = "|";

    public String getFormStringSeparator()
    {
        return this.formStringSeparator;
    }

    public void setFormStringSeparator(String formStringSeparator)
    {
        this.formStringSeparator = formStringSeparator;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseProperty#getValue()
     */
    @Override
    public Object getValue()
    {
        return getList();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseProperty#setValue(java.lang.Object)
     */
    @Override
    public void setValue(Object value)
    {
        this.setList((List<String>) value);
    }

    public String getTextValue()
    {
        return toFormString();
    }

    @Override
    public String toText()
    {
        if ((getList() instanceof PersistentCollection) && (!((PersistentCollection) getList()).wasInitialized())) {
            return "";
        }

        return StringUtils.join(getList().toArray(), " ");
    }

    public String toSingleFormString()
    {
        return super.toFormString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseProperty#toFormString()
     */
    @Override
    public String toFormString()
    {
        CharacterFilter filter = new CharacterFilter();
        filter.addAttribute(this.formStringSeparator, "\\" + this.formStringSeparator);

        List<String> list = getList();
        Iterator<String> it = list.iterator();
        if (!it.hasNext()) {
            return "";
        }
        StringBuffer result = new StringBuffer();
        result.append(it.next());
        while (it.hasNext()) {
            result.append(this.formStringSeparator);
            result.append(filter.process(it.next()));
        }

        return result.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseProperty#equals(java.lang.Object)
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

        List<String> list1 = getList();
        List<String> list2 = (List<String>) ((BaseProperty) obj).getValue();

        // If the collection was not yet initialized by Hibernate
        // Let's use the super result..
        if ((list1 instanceof PersistentCollection) && (!((PersistentCollection) list1).wasInitialized())) {
            return true;
        }

        if ((list2 instanceof PersistentCollection) && (!((PersistentCollection) list2).wasInitialized())) {
            return true;
        }

        if (list1.size() != list2.size()) {
            return false;
        }

        for (int i = 0; i < list1.size(); i++) {
            Object obj1 = list1.get(i);
            Object obj2 = list2.get(i);

            if (!obj1.equals(obj2)) {
                return false;
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseProperty#clone()
     */
    @Override
    public Object clone()
    {
        ListProperty property = (ListProperty) super.clone();
        List<String> list = new ArrayList<String>();
        for (String entry : getList()) {
            list.add(entry);
        }
        property.setValue(list);

        return property;
    }

    public List<String> getList()
    {
        return this.list;
    }

    public void setList(List<String> list)
    {
        if (list == null) {
            this.list = new ArrayList<String>();
        } else {
            this.list = list;
            // In Oracle, empty string are converted to NULL. Since an undefined property is not found at all, it is
            // safe to assume that a retrieved NULL value should actually be an empty string.
            for (Iterator<String> it = this.list.iterator(); it.hasNext();) {
                if (it.next() == null) {
                    it.remove();
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseProperty#toXML()
     */
    @Override
    public Element toXML()
    {
        Element el = new DOMElement(getName());
        List<String> list = getList();
        for (String value : list) {
            if (value != null) {
                Element vel = new DOMElement("value");
                vel.setText(value);
                el.add(vel);
            }
        }

        return el;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This is important.. Otherwise we can get a stackoverflow calling toXML()
     * 
     * @see com.xpn.xwiki.objects.BaseProperty#toString()
     */
    @Override
    public String toString()
    {
        if ((getList() instanceof PersistentCollection) && (!((PersistentCollection) getList()).wasInitialized())) {
            return "";
        }

        return toXMLString();
    }
}
