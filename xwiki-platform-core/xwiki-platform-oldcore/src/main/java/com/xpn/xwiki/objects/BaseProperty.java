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

import java.util.Objects;

import org.apache.commons.lang3.ObjectUtils;
import org.dom4j.Element;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.store.merge.MergeManagerResult;
import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * @version $Id$
 */
// TODO: shouldn't this be abstract? toFormString and toText
// will never work unless getValue is overriden
public class BaseProperty<R extends EntityReference> extends BaseElement<R> implements PropertyInterface, Cloneable
{
    private static final long serialVersionUID = 1L;

    private static final String MERGE_CONFLICT_LOG = "Collision found on property [{}] between from value [{}] and to"
        + " [{}]";

    private BaseCollection object;

    private long id;

    @Override
    protected R createReference()
    {
        R reference;
        if (this.object.getReference() instanceof ObjectReference) {
            reference = (R) new ObjectPropertyReference(getName(), (ObjectReference) this.object.getReference());
        } else {
            reference = super.createReference();
        }

        return reference;
    }

    @Override
    public BaseCollection getObject()
    {
        return this.object;
    }

    @Override
    public void setObject(BaseCollection object)
    {
        if (this.object != object) {
            this.object = object;

            if (this.object != null) {
                setOwnerDocument(object.getOwnerDocument());

                if (isDirty()) {
                    this.object.setDirty(true);
                }
            }
        }
    }

    @Override
    public boolean equals(Object el)
    {
        // Same Java object, they sure are equal
        if (this == el) {
            return true;
        }

        if (el == null) {
            return false;
        }

        // I hate this.. needed for hibernate to find the object
        // when loading the collections..
        if ((this.object == null) || ((BaseProperty) el).getObject() == null) {
            return (hashCode() == el.hashCode());
        }

        if (!super.equals(el)) {
            return false;
        }

        return (getId() == ((BaseProperty) el).getId());
    }

    @Override
    public long getId()
    {
        // I hate this.. needed for hibernate to find the object
        // when loading the collections..
        if (this.object == null) {
            return this.id;
        } else {
            return getObject().getId();
        }
    }

    @Override
    public void setId(long id)
    {
        if (id != this.id) {
            // I hate this.. needed for hibernate to find the object
            // when loading the collections..
            this.id = id;

            setDirty(true);
        }
    }

    @Override
    public int hashCode()
    {
        // I hate this.. needed for hibernate to find the object
        // when loading the collections..
        return ("" + getId() + getName()).hashCode();
    }

    public String getClassType()
    {
        return getClass().getName();
    }

    public void setClassType(String type)
    {
    }

    @Override
    protected void detachOwner()
    {
        super.detachOwner();

        setObject(null);
    }

    @Override
    protected void cloneOwner()
    {
        super.cloneOwner();

        // Get the object from the cloned owner
        if (getOwnerDocument() != null && getObject() != null) {
            setObject(this.ownerDocument.getXObject(this.object.getReference()));
        }
    }

    @Override
    public BaseProperty<R> clone()
    {
        return (BaseProperty<R>) super.clone();
    }

    @Override
    public BaseProperty<R> clone(boolean detach)
    {
        return (BaseProperty<R>) super.clone(detach);
    }

    /**
     * Subclasses override this to copy values during cloning.
     *
     * @param clone The cloned value.
     */
    protected void cloneInternal(BaseProperty clone)
    {
    }

    @Override
    protected void cloneContent(BaseElement<R> element)
    {
        super.cloneContent(element);

        cloneInternal((BaseProperty) element);
    }

    public Object getValue()
    {
        return null;
    }

    public void setValue(Object value)
    {
    }

    @Override
    public Element toXML()
    {
        return super.toXML();
    }

    @Override
    public String toFormString()
    {
        return XMLUtils.escape(toText());
    }

    public String toText()
    {
        Object value = getValue();

        return (value == null) ? "" : value.toString();
    }

    /**
     * Return a XML version of this collection.
     * <p>
     * The XML is not formated. to get formatted XML you can use {@link #toXMLString(boolean)} instead.
     * 
     * @return the XML as a String
     */
    public String toXMLString()
    {
        return super.toXMLString(true);
    }

    public Object getCustomMappingValue()
    {
        return getValue();
    }

    @Override
    public void merge(ElementInterface previousElement, ElementInterface newElement, MergeConfiguration configuration,
        XWikiContext context, MergeResult mergeResult)
    {
        MergeManagerResult<ElementInterface, Object> mergeManagerResult =
            this.merge(previousElement, newElement, configuration, context);
        mergeResult.setModified(mergeResult.isModified() || mergeManagerResult.isModified());
        mergeResult.getLog().addAll(mergeManagerResult.getLog());
        // this method used to always set the value, no matter the result of
        // MergeConfiguration#isProvidedVersionsModifiables.
        setValue(((BaseProperty<R>)mergeManagerResult.getMergeResult()).getValue());
    }

    @Override
    public MergeManagerResult<ElementInterface, Object> merge(ElementInterface previousElement,
        ElementInterface newElement, MergeConfiguration configuration, XWikiContext context)
    {
        MergeManagerResult<ElementInterface, Object> mergeResult =
            super.merge(previousElement, newElement, configuration, context);

        // We don't change current result, but the one in the mergeResult so that we modify either current instance
        // or a clone depending on the given configuration.
        BaseProperty<R> modifiableResult = (BaseProperty<R>) mergeResult.getMergeResult();
        // Value
        Object previousValue = ((BaseProperty<R>) previousElement).getValue();
        Object newValue = ((BaseProperty<R>) newElement).getValue();
        if (previousValue == null) {
            if (newValue != null) {
                if (getValue() == null) {
                    modifiableResult.setValue(newValue);
                    mergeResult.setModified(true);
                } else {
                    // collision between current and new
                    if (configuration.getConflictFallbackVersion() == MergeConfiguration.ConflictFallbackVersion.NEXT) {
                        modifiableResult.setValue(newValue);
                        mergeResult.setModified(true);
                    }
                    mergeResult.getLog().error(MERGE_CONFLICT_LOG, getName(), getValue(), newValue);
                }
            }
        } else if (newValue == null) {
            if (Objects.equals(previousValue, getValue())) {
                modifiableResult.setValue(null);
                mergeResult.setModified(true);
            } else {
                // collision between current and new
                // We don't remove the value in fallback
                mergeResult.getLog().error(MERGE_CONFLICT_LOG, getName(), getValue(), newValue);
            }
        } else {
            if (Objects.equals(previousValue, getValue())) {
                modifiableResult.setValue(newValue);
                mergeResult.setModified(true);
            } else if (previousValue.getClass() != newValue.getClass()) {
                // collision between current and new
                mergeResult.getLog().error(MERGE_CONFLICT_LOG, getName(), getValue(), newValue);
            } else if (!Objects.equals(newValue, getValue())) {
                MergeManagerResult<Object, Object> mergeValueResult =
                    mergeValue(previousValue, newValue, configuration);
                mergeResult.getLog().addAll(mergeValueResult.getLog());
                if (mergeValueResult.isModified()) {
                    modifiableResult.setValue(mergeValueResult.getMergeResult());
                    mergeResult.setModified(true);
                    mergeResult.addConflicts(mergeValueResult.getConflicts());
                }
            }
        }
        return mergeResult;
    }

    /**
     * Try to apply 3 ways merge on property value.
     * Note that this method modifies the internal value of the property.
     *
     * @param previousValue the previous version of the value
     * @param newValue the new version of the value
     * @param mergeResult merge report
     * @since 3.2M1
     * @deprecated now use {@link #mergeValue(Object, Object, MergeConfiguration)}
     */
    @Deprecated(since = "14.10.7,15.2RC1")
    protected void mergeValue(Object previousValue, Object newValue, MergeResult mergeResult)
    {
        MergeManagerResult<Object, Object> result = this.mergeValue(previousValue, newValue, new MergeConfiguration());
        mergeResult.setModified(mergeResult.isModified() || result.isModified());
        mergeResult.getLog().addAll(result.getLog());
        if (result.isModified()) {
            setValue(result.getMergeResult());
        }
    }

    /**
     * Try to apply 3 ways merge on property value.
     * Note that this method does not modify the internal value of the property.
     *
     * @param previousValue the previous version of the value
     * @param newValue the new version of the value
     * @param mergeConfiguration the merge configuration to use
     * @since 15.2RC1
     * @since 14.10.7
     */
    protected MergeManagerResult<Object, Object> mergeValue(Object previousValue, Object newValue,
        MergeConfiguration mergeConfiguration)
    {
        MergeManagerResult<Object, Object> result = new MergeManagerResult<>();
        result.setMergeResult(getValue());
        // collision between current and new: don't know how to apply 3 way merge on unknown type
        result.getLog().error(MERGE_CONFLICT_LOG, getName(), getValue(), newValue);
        return result;
    }

    @Override
    public boolean apply(ElementInterface newProperty, boolean clean)
    {
        boolean modified = super.apply(newProperty, clean);

        BaseProperty<R> newBaseProperty = (BaseProperty<R>) newProperty;

        // Value
        if (ObjectUtils.notEqual(newBaseProperty.getValue(), getValue())) {
            setValue(newBaseProperty.getValue());
            modified = true;
        }

        return modified;
    }

    /**
     * @return {@literal true} if the property value doesn't match the value in the database.
     * @since 4.3M2
     * @deprecated use {@link #isDirty()} instead
     */
    @Deprecated(since = "17.1.0RC1")
    public boolean isValueDirty()
    {
        return isDirty();
    }

    /**
     * Set the dirty flag if the new value isn't equal to the old value.
     *
     * @param newValue The new value.
     */
    protected void setValueDirty(Object newValue)
    {
        if (!isDirty() && !Objects.equals(newValue, getValue())) {
            setValueDirty(true);
        }
    }

    /**
     * @param valueDirty Indicate if the dirty flag should be set or cleared.
     * @since 4.3M2
     * @deprecated use {@link #setDirty(boolean)} instead
     */
    @Deprecated(since = "17.1.0RC1")
    public void setValueDirty(boolean valueDirty)
    {
        setDirty(valueDirty);
    }

    @Override
    public void setDirty(boolean dirty)
    {
        super.setDirty(dirty);

        if (dirty && this.object != null) {
            this.object.setDirty(true);
        }
    }

    /**
     * @param xcontext the XWiki Context
     * @return the definition of the property
     * @since 8.3M1
     */
    public PropertyClass getPropertyClass(XWikiContext xcontext)
    {
        if (getObject() instanceof BaseObject) {
            XWikiDocument document = getOwnerDocument();
            if (document != null) {
                BaseObject xobject = document.getXObject(getReference().getParent());
                if (xobject != null) {
                    BaseClass xclass = xobject.getXClass(xcontext);

                    return (PropertyClass) xclass.get(getName());
                }
            }
        }

        return null;
    }
}
