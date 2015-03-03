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

import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.hibernate.collection.PersistentCollection;
import org.xwiki.xar.internal.property.ListXarObjectPropertySerializer;

import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.internal.AbstractNotifyOnUpdateList;
import com.xpn.xwiki.internal.merge.MergeUtils;
import com.xpn.xwiki.internal.objects.ListPropertyPersistentList;
import com.xpn.xwiki.objects.classes.ListClass;

public class ListProperty extends BaseProperty implements Cloneable
{
    /**
     * We make this a notifying list, because we must propagate any value updates to the owner document.
     */
    protected transient List<String> list;

    /**
     * @deprecated since 7.0M2. This was never used, since it is not the right place to handle separators. They are
     *             defined in {@link ListClass} and that is where they are now handled through
     *             {@link ListClass#toFormString(BaseProperty)}.
     */
    @Deprecated
    private String formStringSeparator = ListClass.DEFAULT_SEPARATOR;

    /**
     * This is the actual list. It will be used during serialization/deserialization.
     */
    private List<String> actualList = new ArrayList<String>();

    {
        this.list = new NotifyList(this.actualList, this);
    }

    /**
     * @deprecated since 7.0M2. This was never used, since it is not the right place to handle separators. They are
     *             defined in {@link ListClass} and that is where they are now handled through
     *             {@link ListClass#toFormString(BaseProperty)}.
     */
    @Deprecated
    public String getFormStringSeparator()
    {
        return this.formStringSeparator;
    }

    /**
     * @deprecated since 7.0M2. This was never used, since it is not the right place to handle separators. They are
     *             defined in {@link ListClass} and that is where they are now handled through
     *             {@link ListClass#toFormString(BaseProperty)}.
     */
    @Deprecated
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
        // Always use the default separator because this is the value that is stored in the database (for non-relational
        // lists).
        String result = ListClass.getStringFromList(this.getList(), ListClass.DEFAULT_SEPARATOR);
        return result;
    }

    /**
     * @deprecated Since 7.0M2. This method is here for a long time but it does not seem to have ever been used and it
     *             does not bring any value compared to the existing {@link #toFormString()} method.
     */
    @Deprecated
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
        property.actualList = new ArrayList<String>();
        for (String entry : getList()) {
            property.actualList.add(entry);
        }
        property.list = new NotifyList(property.actualList, property);
    }

    public List<String> getList()
    {
        // Hibernate will not set the owner of the notify list, so we must make sure this has been done before returning
        // the list.
        if (this.list instanceof NotifyList) {
            ((NotifyList) this.list).setOwner(this);
        } else if (this.list instanceof ListPropertyPersistentList) {
            ((ListPropertyPersistentList) this.list).setOwner(this);
        }

        return this.list;
    }

    /**
     * Starting from 4.3M2, this method will copy the list passed as parameter. Due to XWIKI-8398 we must be able to
     * detect when the values in the list changes, so we cannot store the values in any type of list.
     *
     * @param list The list to copy.
     */
    public void setList(List<String> list)
    {
        if (list == this.list || list == this.actualList) {
            // Accept a caller that sets the already existing list instance.
            return;
        }

        if (this.list instanceof ListPropertyPersistentList) {
            ListPropertyPersistentList persistentList = (ListPropertyPersistentList) this.list;
            if (persistentList.isWrapper(list)) {
                // Accept a caller that sets the already existing list instance.
                return;
            }
        }

        if (list instanceof ListPropertyPersistentList) {
            // This is the list wrapper we are using for hibernate.
            ListPropertyPersistentList persistentList = (ListPropertyPersistentList) list;
            this.list = persistentList;
            persistentList.setOwner(this);
            return;
        }

        if (list == null) {
            setValueDirty(true);
            this.actualList = new ArrayList();
            this.list = new NotifyList(this.actualList, this);
        } else {
            this.list.clear();
            this.list.addAll(list);
        }

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
                Element vel = new DOMElement(ListXarObjectPropertySerializer.ELEMENT_VALUE);
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
     * List implementation for updating dirty flag when updated. This will be accessed from ListPropertyUserType.
     */
    public static class NotifyList extends AbstractNotifyOnUpdateList<String>
    {

        /** The owner list property. */
        private ListProperty owner;

        /** The dirty flag. */
        private boolean dirty;

        private List<String> actualList;

        /**
         * @param list {@link AbstractNotifyOnUpdateList}.
         */
        public NotifyList(List<String> list)
        {
            super(list);
            this.actualList = list;
        }

        private NotifyList(List<String> list, ListProperty owner)
        {
            this(list);

            this.owner = owner;
        }

        @Override
        public void onUpdate()
        {
            setDirty();
        }

        /**
         * @param owner The owner list property.
         */
        public void setOwner(ListProperty owner)
        {
            if (this.dirty) {
                owner.setValueDirty(true);
            }
            this.owner = owner;
            owner.actualList = this.actualList;
        }

        /**
         * @return {@literal true} if the given argument is the instance that this list wraps.
         */
        public boolean isWrapper(Object collection)
        {
            return this.actualList == collection;
        }

        /**
         * Set the dirty flag.
         */
        private void setDirty()
        {
            if (this.owner != null) {
                this.owner.setValueDirty(true);
            }
            this.dirty = true;
        }
    }
}
