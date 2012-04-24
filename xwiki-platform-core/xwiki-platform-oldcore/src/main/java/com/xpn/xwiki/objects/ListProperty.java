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
package com.xpn.xwiki.objects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.hibernate.collection.PersistentCollection;
import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.internal.merge.MergeUtils;

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

    @Override
    public Object getValue()
    {
        return getList();
    }

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

    @Override
    public String toFormString()
    {
        List<String> list = getList();
        StringBuilder result = new StringBuilder();
        for (String item : list) {
            result.append(XMLUtils.escape(item).replace(this.formStringSeparator, "\\" + this.formStringSeparator));
            result.append(this.formStringSeparator);
        }

        return StringUtils.chomp(result.toString(), this.formStringSeparator);
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

    @Override
    public ListProperty clone()
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

    @Override
    protected void mergeValue(Object previousValue, Object newValue, MergeResult mergeResult)
    {
        MergeUtils.mergeCollection((List<String>) previousValue, (List<String>) newValue, this.list, mergeResult);
    }
}
