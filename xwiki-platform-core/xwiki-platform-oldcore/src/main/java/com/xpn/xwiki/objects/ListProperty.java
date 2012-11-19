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
import org.hibernate.collection.PersistentList;
import org.xwiki.diff.DiffManager;
import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.util.AbstractNotifyOnUpdateList;
import com.xpn.xwiki.internal.merge.MergeUtils;
import com.xpn.xwiki.web.Utils;

public class ListProperty extends BaseProperty implements Cloneable
{
    /**
     * Used to do the actual merge.
     */
    private static DiffManager diffManager = Utils.getComponent(DiffManager.class);

    /**
     * We make this a notifying list, because we must propagate any value updates to the owner document.
     */
    protected transient List<String> list;

    /**
     * The notify list wrapper.
     */
    private transient NotifyList notifyList;

    private String formStringSeparator = "|";

    /**
     * This is the actual list.  It will be used during serialization/deserialization.
     */
    private List<String> actualList = new ArrayList<String>();

    {
        notifyList = new NotifyList(actualList);
        list = notifyList;
    }

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
        return (ListProperty) super.clone();
    }

    @Override
    protected void cloneInternal(BaseProperty clone)
    {
        ListProperty property = (ListProperty) clone;
        property.actualList = new ArrayList<String>();
        for (String entry : getList()) {
            property.actualList.add(entry);
        }
        property.list = new NotifyList(property.actualList);
    }

    public List<String> getList()
    {
        return this.list;
    }

    /**
     * Starting from 4.3M2, this method will copy the list passed as parameter.  Due to XWIKI-8398 we must be able to
     * detect when the values in the list changes, so we cannot store the values in any type of list.
     * 
     * @param list The list to copy.
     */
    public void setList(List<String> list)
    {
        if (list == notifyList) {
            return;
        } 

        if (list instanceof PersistentList) {
            PersistentList persistentList = (PersistentList) list;
            if (persistentList.isWrapper(notifyList)) {
                // Accept hibernate setting the persistent list wrapper.
                this.list = list;
                return;
            }
        }

        if (list == null) {
            actualList = new ArrayList();
        } else {
            actualList = list;
        }

        notifyList = new NotifyList(actualList);
        this.list = notifyList;

        // In Oracle, empty string are converted to NULL. Since an undefined property is not found at all, it is
        // safe to assume that a retrieved NULL value should actually be an empty string.
        for (Iterator<String> it = this.list.iterator(); it.hasNext();) {
            if (it.next() == null) {
                it.remove();
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
        MergeUtils.mergeList((List<String>) previousValue, (List<String>) newValue, this.list, mergeResult);
    }

    /**
     * List implementation for updating dirty flag when updated.
     */
    private class NotifyList extends AbstractNotifyOnUpdateList<String>
    {

        /**
         * @param list {@see AbstractNotifyOnUpdateList}.
         */
        private NotifyList(List<String> list)
        {
            super(list);
        }

        @Override
        public void onUpdate()
        {
            setValueDirty(true);
        }
    }
}
