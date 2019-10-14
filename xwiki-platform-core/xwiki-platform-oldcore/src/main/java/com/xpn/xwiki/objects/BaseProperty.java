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

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.ObjectUtils;
import org.dom4j.Element;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
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
public class BaseProperty<R extends EntityReference> extends BaseElement<R>
    implements PropertyInterface, Serializable, Cloneable
{
    private static final long serialVersionUID = 1L;

    private BaseCollection object;

    private long id;

    /**
     * Set to true if value is not the same as the database value.
     */
    private boolean isValueDirty = true;

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
        this.object = object;
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
        // I hate this.. needed for hibernate to find the object
        // when loading the collections..
        this.id = id;
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
    public BaseProperty<R> clone()
    {
        BaseProperty<R> property = (BaseProperty<R>) super.clone();

        property.ownerDocument = null;

        cloneInternal(property);

        property.isValueDirty = this.isValueDirty;
        property.ownerDocument = this.ownerDocument;

        property.setObject(getObject());

        return property;
    }

    /**
     * Subclasses override this to copy values during cloning.
     *
     * @param clone The cloned value.
     */
    protected void cloneInternal(BaseProperty clone)
    {
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
        super.merge(previousElement, newElement, configuration, context, mergeResult);

        // Value
        Object previousValue = ((BaseProperty<R>) previousElement).getValue();
        Object newValue = ((BaseProperty<R>) newElement).getValue();
        if (previousValue == null) {
            if (newValue != null) {
                if (getValue() == null) {
                    setValue(newValue);
                } else {
                    // collision between current and new
                    mergeResult.getLog().error("Collision found on property [{}] between from value [{}] and to [{}]",
                        getName(), getValue(), newValue);
                }
            }
        } else if (newValue == null) {
            if (Objects.equals(previousValue, getValue())) {
                setValue(null);
            } else {
                // collision between current and new
                mergeResult.getLog().error("Collision found on property [{}] between from value [{}] and to [{}]",
                    getName(), getValue(), newValue);
            }
        } else {
            if (Objects.equals(previousValue, getValue())) {
                setValue(newValue);
            } else if (previousValue.getClass() != newValue.getClass()) {
                // collision between current and new
                mergeResult.getLog().error("Collision found on property [{}] between from value [] and to []",
                    getName(), getValue(), newValue);
            } else if (!Objects.equals(newValue, getValue())) {
                mergeValue(previousValue, newValue, mergeResult);
            }
        }
    }

    /**
     * Try to apply 3 ways merge on property value.
     *
     * @param previousValue the previous version of the value
     * @param newValue the new version of the value
     * @param mergeResult merge report
     * @since 3.2M1
     */
    protected void mergeValue(Object previousValue, Object newValue, MergeResult mergeResult)
    {
        // collision between current and new: don't know how to apply 3 way merge on unknown type
        mergeResult.getLog().error("Collision found on property [{}] between from value [{}] and to [{}]", getName(),
            getValue(), newValue);
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
     */
    public boolean isValueDirty()
    {
        return this.isValueDirty;
    }

    /**
     * Set the dirty flag if the new value isn't equal to the old value.
     *
     * @param newValue The new value.
     */
    protected void setValueDirty(Object newValue)
    {
        if (!this.isValueDirty && !Objects.equals(newValue, getValue())) {
            setValueDirty(true);
        }
    }

    /**
     * @param valueDirty Indicate if the dirty flag should be set or cleared.
     * @since 4.3M2
     */
    public void setValueDirty(boolean valueDirty)
    {
        this.isValueDirty = valueDirty;
        if (valueDirty && this.ownerDocument != null) {
            this.ownerDocument.setMetaDataDirty(true);
        }
    }

    /**
     * Set the owner document of this base property.
     *
     * @param ownerDocument The owner document.
     * @since 4.3M2
     */
    @Override
    public void setOwnerDocument(XWikiDocument ownerDocument)
    {
        super.setOwnerDocument(ownerDocument);

        if (ownerDocument != null && this.isValueDirty) {
            ownerDocument.setMetaDataDirty(true);
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
