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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.web.Utils;

public class BaseObject extends BaseCollection<BaseObjectReference> implements ObjectInterface, Serializable, Cloneable
{
    private String guid = UUID.randomUUID().toString();

    /**
     * Used to resolve a string into a proper Document Reference using the current document's reference to fill the
     * blanks, except for the page name for which the default page name is used instead and for the wiki name for which
     * the current wiki is used instead of the current document reference's wiki.
     */
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.TYPE_STRING, "currentmixed");

    /**
     * {@inheritDoc}
     * <p>
     * Note: This method is overridden to add the deprecation warning so that code using it can see it's deprecated.
     * 
     * @deprecated since 2.2M2 use {@link #getDocumentReference()}
     */
    @Deprecated
    @Override
    public String getName()
    {
        return super.getName();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note: BaseElement.setName() does not support setting reference anymore since 2.4M2.
     * 
     * @deprecated since 2.2M2 use {@link #setDocumentReference(org.xwiki.model.reference.DocumentReference)}
     */
    @Deprecated
    @Override
    public void setName(String name)
    {
        DocumentReference reference = getDocumentReference();

        if (reference != null) {
            EntityReference relativeReference = this.relativeEntityReferenceResolver.resolve(name, EntityType.DOCUMENT);
            reference =
                new DocumentReference(relativeReference.extractReference(EntityType.DOCUMENT).getName(),
                    new SpaceReference(relativeReference.extractReference(EntityType.SPACE).getName(), reference
                        .getParent().getParent()));
        } else {
            reference = this.currentMixedDocumentReferenceResolver.resolve(name);
        }
        setDocumentReference(reference);
    }

    @Override
    protected BaseObjectReference createReference()
    {
        return new BaseObjectReference(getXClassReference(), getNumber(), getDocumentReference());
    }

    @Override
    public void setNumber(int number)
    {
        super.setNumber(number);

        // Reset reference cache
        this.referenceCache = null;
    }

    @Override
    public void setXClassReference(EntityReference xClassReference)
    {
        super.setXClassReference(xClassReference);

        // Reset reference cache
        this.referenceCache = null;
    }

    public void displayHidden(StringBuffer buffer, String name, String prefix, XWikiContext context)
    {
        ((PropertyClass) getXClass(context).get(name)).displayHidden(buffer, name, prefix, this, context);
    }

    public void displayView(StringBuffer buffer, String name, String prefix, XWikiContext context)
    {
        ((PropertyClass) getXClass(context).get(name)).displayView(buffer, name, prefix, this, context);
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix, XWikiContext context)
    {
        ((PropertyClass) getXClass(context).get(name)).displayEdit(buffer, name, prefix, this, context);
    }

    public String displayHidden(String name, String prefix, XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        ((PropertyClass) getXClass(context).get(name)).displayHidden(buffer, name, prefix, this, context);

        return buffer.toString();
    }

    public String displayView(String name, String prefix, XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        ((PropertyClass) getXClass(context).get(name)).displayView(buffer, name, prefix, this, context);

        return buffer.toString();
    }

    public String displayEdit(String name, String prefix, XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        ((PropertyClass) getXClass(context).get(name)).displayEdit(buffer, name, prefix, this, context);

        return buffer.toString();
    }

    public String displayHidden(String name, XWikiContext context)
    {
        return displayHidden(name, "", context);
    }

    public String displayView(String name, XWikiContext context)
    {
        return displayView(name, "", context);
    }

    public String displayEdit(String name, XWikiContext context)
    {
        return displayEdit(name, "", context);
    }

    @Override
    public BaseObject clone()
    {
        BaseObject object = (BaseObject) super.clone();
        object.setGuid(getGuid());

        return object;
    }

    /**
     * Similar to {@link #clone()} but whereas a clone is an exact copy (with the same GUID), a duplicate keeps the same
     * data but with a different identity.
     * 
     * @since 2.2.3
     */
    public BaseObject duplicate()
    {
        BaseObject object = clone();
        // Set a new GUID for the duplicate
        object.setGuid(UUID.randomUUID().toString());

        return object;
    }

    /**
     * @since 2.2.3
     */
    public BaseObject duplicate(DocumentReference documentReference)
    {
        BaseObject object = duplicate();
        object.setDocumentReference(documentReference);
        return object;
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

        if (getNumber() != ((BaseObject) obj).getNumber()) {
            return false;
        }

        return true;
    }

    @Override
    public Element toXML(BaseClass bclass)
    {
        Element oel = new DOMElement("object");

        // Add Class
        if (bclass != null) {
            // If the class has fields, add field information to XML
            Collection fields = bclass.getFieldList();
            if (fields.size() > 0) {
                oel.add(bclass.toXML());
            }
        }

        oel.add(exportProperty("name", getName()));
        oel.add(exportProperty("number", String.valueOf(getNumber())));
        oel.add(exportProperty("className", getClassName()));
        oel.add(exportProperty("guid", getGuid()));

        // Iterate over values/properties sorted by field name so that the values are
        // exported to XML in a consistent order.
        Iterator it = getSortedIterator();
        while (it.hasNext()) {
            Element pel = new DOMElement("property");
            PropertyInterface bprop = (PropertyInterface) it.next();

            String pname = bprop.getName();
            if (pname != null && !pname.trim().equals("")) {
                pel.add(bprop.toXML());
                oel.add(pel);
            }
        }
        return oel;
    }

    private Element exportProperty(String propertyName, String propertyValue)
    {
        Element propertyElement = new DOMElement(propertyName);
        propertyElement.addText(StringUtils.defaultString(propertyValue));
        return propertyElement;
    }

    public void fromXML(Element oel) throws XWikiException
    {
        Element cel = oel.element("class");
        BaseClass bclass = new BaseClass();
        if (cel != null) {
            bclass.fromXML(cel);
            setClassName(bclass.getName());
        } else {
            // We need at least to set the class name to avoid some NullPointerExceptions
            setClassName(oel.elementText("className"));
        }

        setName(oel.element("name").getText());
        String number = oel.element("number").getText();
        if (number != null) {
            setNumber(Integer.parseInt(number));
        }

        // If no GUID exists in the XML, then use the one randomly generated at initialization
        Element guidElement = oel.element("guid");
        if (guidElement != null && guidElement.getText() != null) {
            this.guid = guidElement.getText();
        }

        List list = oel.elements("property");
        for (int i = 0; i < list.size(); i++) {
            Element pcel = (Element) ((Element) list.get(i)).elements().get(0);
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

    @Override
    public List<ObjectDiff> getDiff(Object oldEntity, XWikiContext context)
    {
        ArrayList<ObjectDiff> difflist = new ArrayList<ObjectDiff>();
        BaseObject oldObject = (BaseObject) oldEntity;
        // Iterate over the new properties first, to handle changed and added objects
        for (String propertyName : this.getPropertyList()) {
            BaseProperty newProperty = (BaseProperty) this.getField(propertyName);
            BaseProperty oldProperty = (BaseProperty) oldObject.getField(propertyName);
            BaseClass bclass = getXClass(context);
            PropertyClass pclass = (PropertyClass) ((bclass == null) ? null : bclass.getField(propertyName));
            String propertyType = (pclass == null) ? "" : StringUtils.substringAfterLast(pclass.getClassType(), ".");

            if (oldProperty == null) {
                // The property exist in the new object, but not in the old one
                if ((newProperty != null) && (!newProperty.toText().equals(""))) {
                    String newPropertyValue =
                        (newProperty.getValue() instanceof String || pclass == null) ? newProperty.toText() : pclass
                            .displayView(propertyName, this, context);
                    difflist.add(new ObjectDiff(getXClassReference(), getNumber(), getGuid(),
                        ObjectDiff.ACTION_PROPERTYADDED, propertyName, propertyType, "", newPropertyValue));
                }
            } else if (!oldProperty.toText().equals(((newProperty == null) ? "" : newProperty.toText()))) {
                // The property exists in both objects and is different
                if (pclass != null) {
                    // Put the values as they would be displayed in the interface
                    String newPropertyValue =
                        (newProperty.getValue() instanceof String || pclass == null) ? newProperty.toText() : pclass
                            .displayView(propertyName, this, context);
                    String oldPropertyValue =
                        (oldProperty.getValue() instanceof String || pclass == null) ? oldProperty.toText() : pclass
                            .displayView(propertyName, oldObject, context);
                    difflist.add(new ObjectDiff(getXClassReference(), getNumber(), getGuid(),
                        ObjectDiff.ACTION_PROPERTYCHANGED, propertyName, propertyType, oldPropertyValue,
                        newPropertyValue));
                } else {
                    // Cannot get property definition, so use the plain value
                    difflist.add(new ObjectDiff(getXClassReference(), getNumber(), getGuid(),
                        ObjectDiff.ACTION_PROPERTYCHANGED, propertyName, propertyType, oldProperty.toText(),
                        newProperty.toText()));
                }
            }
        }

        // Iterate over the old properties, in case there are some removed properties
        for (String propertyName : oldObject.getPropertyList()) {
            BaseProperty newProperty = (BaseProperty) this.getField(propertyName);
            BaseProperty oldProperty = (BaseProperty) oldObject.getField(propertyName);
            BaseClass bclass = getXClass(context);
            PropertyClass pclass = (PropertyClass) ((bclass == null) ? null : bclass.getField(propertyName));
            String propertyType = (pclass == null) ? "" : StringUtils.substringAfterLast(pclass.getClassType(), ".");

            if (newProperty == null) {
                // The property exists in the old object, but not in the new one
                if ((oldProperty != null) && (!oldProperty.toText().equals(""))) {
                    if (pclass != null) {
                        // Put the values as they would be displayed in the interface
                        String oldPropertyValue =
                            (oldProperty.getValue() instanceof String) ? oldProperty.toText() : pclass.displayView(
                                propertyName, oldObject, context);
                        difflist.add(new ObjectDiff(oldObject.getXClassReference(), oldObject.getNumber(), oldObject
                            .getGuid(), ObjectDiff.ACTION_PROPERTYREMOVED, propertyName, propertyType,
                            oldPropertyValue, ""));
                    } else {
                        // Cannot get property definition, so use the plain value
                        difflist.add(new ObjectDiff(oldObject.getXClassReference(), oldObject.getNumber(), oldObject
                            .getGuid(), ObjectDiff.ACTION_PROPERTYREMOVED, propertyName, propertyType, oldProperty
                            .toText(), ""));
                    }
                }
            }
        }

        return difflist;
    }

    public com.xpn.xwiki.api.Object newObjectApi(BaseObject obj, XWikiContext context)
    {
        return new com.xpn.xwiki.api.Object(obj, context);
    }

    public void set(String fieldname, java.lang.Object value, XWikiContext context)
    {
        BaseClass bclass = getXClass(context);
        PropertyClass pclass = (PropertyClass) bclass.get(fieldname);
        BaseProperty prop = (BaseProperty) safeget(fieldname);
        if ((value instanceof String) && (pclass != null)) {
            prop = pclass.fromString((String) value);
        } else {
            if ((prop == null) && (pclass != null)) {
                prop = pclass.newProperty();
            }
            if (prop != null) {
                prop.setValue(value);
            }
        }

        if (prop != null) {
            safeput(fieldname, prop);
        }
    }

    public String getGuid()
    {
        return this.guid;
    }

    public void setGuid(String guid)
    {
        this.guid = guid;
    }
}
