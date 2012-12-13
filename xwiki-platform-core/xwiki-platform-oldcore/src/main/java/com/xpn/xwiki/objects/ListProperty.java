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
import org.xwiki.diff.DiffManager;

import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.internal.merge.MergeUtils;
import com.xpn.xwiki.util.AbstractNotifyOnUpdateList;
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
    protected List<String> list = new NotifyList(this);


    private String formStringSeparator = "|";

    /** Indicate that hibernate workaround for getList should be enabled. */
    private boolean useHibernateWorkaround = false;

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

    /**
     * This method is called by Hibernate to get the raw value to store in the database. Check the xwiki.hbm.xml file.
     * 
     * @return the string value that is saved in the database
     */
    public String getTextValue()
    {
        return toText();
    }

    @Override
    public String toText()
    {
        if ((getList() instanceof PersistentCollection) && (!((PersistentCollection) getList()).wasInitialized())) {
            return "";
        }

        List<String> escapedValues = new ArrayList<String>();
        for (String value : getList()) {
            escapedValues.add(value.replace(this.formStringSeparator, "\\" + this.formStringSeparator));
        }
        return StringUtils.join(escapedValues, this.formStringSeparator);
    }

    public String toSingleFormString()
    {
        return super.toFormString();
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

        property.list = new NotifyList(property);

        property.list.addAll(list);
    }

    public List<String> getList()
    {
        if (useHibernateWorkaround) {
            // FIXME: Hibernate does not like the
            // AbstractNotifyOnUpdateList, so we must use a workaround
            // when saving this property.  Try removing this
            // workaround after we have upgraded hibernate.
            List<String> arrayList = new ArrayList<String>();
            arrayList.addAll(list);
            return arrayList;
        } else {
            return this.list;
        }
    }

    /**
     * Starting from 4.3M2, this method will copy the list passed as parameter.  Due to XWIKI-8398 we must be able to
     * detect when the values in the list changes, so we cannot store the values in any type of list.
     * 
     * @param list The list to copy.
     */
    public void setList(List<String> list)
    {
        this.list.clear();
        if (list != null) {
            this.list.addAll(list);
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
        MergeUtils.mergeList((List<String>) previousValue, (List<String>) newValue, this.list, mergeResult);
    }

    /**
     * If getList returns a list of type AbstractNotifyOnUpdateList hibernate will for some reason always store an empty
     * lits.  As a workaround, we let getList return an ordinary ArrayList when in hibernate.  This method is used for
     * enabling/disabling this workaround.  FIXME: Try removing this workaround after we have upgraded hibernate.
     * 
     * @param useHibernateWorkaround {@literal true} if hibernate workaround for getList should be enabled.
     * @since 4.3M2
     */
    public void setUseHibernateWorkaround(boolean useHibernateWorkaround)
    {
        this.useHibernateWorkaround = useHibernateWorkaround;
    }

    private static class NotifyList extends AbstractNotifyOnUpdateList<String>
    {

        /** The owning list property. */
        private final ListProperty owner;

        /**
         * Construct a wrapper list for a list property.
         *
         * @param owner The owning list property.
         */
        public NotifyList(ListProperty owner)
        {
            this.owner = owner;
        }

        @Override
        public void onUpdate()
        {
            owner.setValueDirty(true);
        }
    }
}
