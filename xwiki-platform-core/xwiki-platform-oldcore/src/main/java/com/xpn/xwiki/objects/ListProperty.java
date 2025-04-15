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
import java.util.List;

import org.hibernate.collection.spi.PersistentCollection;
import org.xwiki.store.merge.MergeManagerResult;

import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.internal.AbstractNotifyOnUpdateList;
import com.xpn.xwiki.objects.classes.ListClass;

public class ListProperty extends BaseProperty implements Cloneable
{
    private static final long serialVersionUID = 1L;

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
    private List<String> actualList = new ArrayList<>();

    /**
     * Wrap the actual list into a {@link NotifyList}.
     */
    public ListProperty()
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
        setList((List<String>) value);
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
        return ListClass.getStringFromList(this.getList(), ListClass.DEFAULT_SEPARATOR);
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
    public ListProperty clone(boolean detach)
    {
        return (ListProperty) super.clone(detach);
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
        // Wrap the list if it was (wrongly) changed by a child class
        if (!(this.list instanceof NotifyList)) {
            return new NotifyList(this.list, this);
        }

        return this.list;
    }

    /**
     * Set the value and also make sure that the exposed list will always be a {@link NotifyList} so that any direct
     * modification to the list will impact its owners dirty flags.
     *
     * @param list the list to copy.
     */
    public void setList(List<String> list)
    {
        if (list == this.list || list == this.actualList) {
            // Accept a caller that sets the already existing list instance.
            return;
        }

        if (list == null) {
            if (!this.actualList.isEmpty()) {
                this.actualList.clear();

                setDirty(true);
            }
        } else if (!this.list.equals(list)) {
            // Clear the current list
            this.actualList.clear();

            // In Oracle, empty strings are converted to NULL. Since an undefined property is not found at all, it is
            // safe to assume that a retrieved NULL value should actually be an empty string.
            list.stream().map(e -> e == null ? "" : e).forEach(this.actualList::add);

            // Update the dirty flag
            setDirty(true);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This is important.. Otherwise we can get a stackoverflow calling toXML().
     * </p>
     *
     * @see com.xpn.xwiki.objects.BaseProperty#toString()
     */
    @Override
    public String toString()
    {
        if ((getList() instanceof PersistentCollection) && (!((PersistentCollection) getList()).wasInitialized())) {
            return "";
        }

        return super.toString();
    }

    @Override
    protected MergeManagerResult<Object, Object> mergeValue(Object previousValue, Object newValue,
        MergeConfiguration configuration)
    {
        MergeManagerResult<List<String>, String> listStringMergeManagerResult = getMergeManager()
            .mergeList((List<String>) previousValue, (List<String>) newValue, this.list, configuration);

        MergeManagerResult<Object, Object> result = new MergeManagerResult<>();
        result.setLog(listStringMergeManagerResult.getLog());
        result.setMergeResult(listStringMergeManagerResult.getMergeResult());
        // We cannot convert a Conflict<String> to Conflict<Object> right now, so we're loosing conflicts info here...
        result.setModified(listStringMergeManagerResult.isModified());
        return result;
    }

    /**
     * List implementation for updating dirty flag when updated. This will be accessed from ListPropertyUserType.
     */
    public static class NotifyList extends AbstractNotifyOnUpdateList<String>
    {
        /**
         * The owner list property.
         */
        private ListProperty owner;

        /**
         * The dirty flag.
         */
        private boolean dirty;

        private final List<String> actualList;

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
            if (this.owner != owner) {
                if (this.dirty) {
                    owner.setDirty(true);
                }
                this.owner = owner;
                owner.actualList = this.actualList;
            }
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
