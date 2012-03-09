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

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;

import org.apache.commons.lang3.ObjectUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.merge.CollisionException;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.web.Utils;

/**
 * @version $Id$
 */
// TODO: shouldn't this be abstract? toFormString and toText
// will never work unless getValue is overriden
public class BaseProperty<R extends EntityReference> extends BaseElement<R> implements PropertyInterface, Serializable,
    Cloneable
{
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
        this.object = object;
    }

    @Override
    public boolean equals(Object el)
    {
        // Same Java object, they sure are equal
        if (this == el) {
            return true;
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
    public BaseProperty clone()
    {
        BaseProperty<R> property = (BaseProperty<R>) super.clone();

        property.setObject(getObject());

        return property;
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
        Element el = new DOMElement(getName());
        Object value = getValue();
        el.setText((value == null) ? "" : value.toString());

        return el;
    }

    @Override
    public String toFormString()
    {
        return Utils.formEncode(toText());
    }

    public String toText()
    {
        Object value = getValue();

        return (value == null) ? "" : value.toString();
    }

    public String toXMLString()
    {
        Document doc = new DOMDocument();
        doc.setRootElement(toXML());
        OutputFormat outputFormat = new OutputFormat("", true);
        StringWriter out = new StringWriter();
        XMLWriter writer = new XMLWriter(out, outputFormat);
        try {
            writer.write(doc);

            return out.toString();
        } catch (IOException e) {
            e.printStackTrace();

            return "";
        }
    }

    @Override
    public String toString()
    {
        return toXMLString();
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
                    // XXX: collision between current and new
                    mergeResult.error(new CollisionException("Collision found on property [" + getName()
                        + "] between from value [" + getValue() + "] and to [" + newValue + "]"));
                }
            }
        } else if (newValue == null) {
            if (ObjectUtils.equals(previousValue, getValue())) {
                setValue(null);
            } else {
                // XXX: collision between current and new
                mergeResult.error(new CollisionException("Collision found on property [" + getName()
                    + "] between from value [" + getValue() + "] and to [" + newValue + "]"));
            }
        } else {
            if (ObjectUtils.equals(previousValue, getValue())) {
                setValue(newValue);
            } else if (previousValue.getClass() != newValue.getClass()) {
                // XXX: collision between current and new
                mergeResult.error(new CollisionException("Collision found on property [" + getName()
                    + "] between from value [" + getValue() + "] and to [" + newValue + "]"));
            } else if (ObjectUtils.equals(previousValue, getValue())) {
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
        // XXX: collision between current and new: don't know how to apply 3 way merge on unknown type
        mergeResult.error(new CollisionException("Collision found on property [" + getName() + "] between from value ["
            + getValue() + "] and to [" + newValue + "]"));
    }
}
