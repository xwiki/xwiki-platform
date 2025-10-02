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
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dom4j.Element;
import org.xwiki.evaluation.ObjectEvaluator;
import org.xwiki.evaluation.ObjectEvaluatorException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.web.Utils;

/**
 * Java abstraction of an XObject.
 *
 * @version $Id$
 */
@SuppressWarnings({"checkstyle:ClassFanOutComplexity", "checkstyle:CyclomaticComplexity", "checkstyle:NPathComplexity"})
public class BaseObject extends BaseCollection<BaseObjectReference> implements ObjectInterface, Cloneable
{
    private static final long serialVersionUID = 1L;

    private String guid;

    /**
     * Used to resolve a string into a proper Document Reference using the current document's reference to fill the
     * blanks, except for the page name for which the default page name is used instead and for the wiki name for which
     * the current wiki is used instead of the current document reference's wiki.
     */
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

    private DocumentReferenceResolver<String> getCurrentMixedDocumentReferenceResolver()
    {
        if (this.currentMixedDocumentReferenceResolver == null) {
            this.currentMixedDocumentReferenceResolver =
                Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");
        }

        return this.currentMixedDocumentReferenceResolver;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note: This method is overridden to add the deprecation warning so that code using it can see it's deprecated.
     * </p>
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
     * </p>
     *
     * @deprecated since 2.2M2 use {@link #setDocumentReference(org.xwiki.model.reference.DocumentReference)}
     */
    @Deprecated
    @Override
    public void setName(String name)
    {
        DocumentReference reference = getDocumentReference();

        if (reference != null) {
            EntityReference relativeReference = getRelativeEntityReferenceResolver().resolve(name, EntityType.DOCUMENT);
            reference = new DocumentReference(relativeReference.extractReference(EntityType.DOCUMENT).getName(),
                new SpaceReference(relativeReference.extractReference(EntityType.SPACE).getName(),
                    reference.getParent().getParent()));
        } else {
            reference = getCurrentMixedDocumentReferenceResolver().resolve(name);
        }
        setDocumentReference(reference);
    }

    @Override
    protected BaseObjectReference createReference()
    {
        BaseObjectReference reference;

        if (getXClassReference() != null && getDocumentReference() != null) {
            reference = new BaseObjectReference(getXClassReference(), getNumber(), getDocumentReference());
        } else {
            reference = null;
        }

        return reference;
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

    /**
     * Display a hidden input for the given property in the given buffer.
     * @param buffer where to write the output
     * @param name the name of the property to display
     * @param prefix the prefix to use for the name of the field
     * @param context the wiki context to use for computing the values
     * @see com.xpn.xwiki.objects.classes.PropertyClassInterface#displayHidden(StringBuffer, String, String,
     * BaseCollection, XWikiContext)
     */
    public void displayHidden(StringBuffer buffer, String name, String prefix, XWikiContext context)
    {
        ((PropertyClass) getXClass(context).get(name)).displayHidden(buffer, name, prefix, this, context);
    }

    /**
     * Display the value of the given property in the given buffer.
     * @param buffer where to write the output
     * @param name the name of the property to display
     * @param prefix the prefix to use for the name of the field
     * @param context the wiki context to use for computing the values
     * @see com.xpn.xwiki.objects.classes.PropertyClassInterface#displayView(StringBuffer, String, String,
     * BaseCollection, XWikiContext)
     */
    public void displayView(StringBuffer buffer, String name, String prefix, XWikiContext context)
    {
        ((PropertyClass) getXClass(context).get(name)).displayView(buffer, name, prefix, this, context);
    }

    /**
     * Display an edit input of the given property in the given buffer.
     * @param buffer where to write the output
     * @param name the name of the property to display
     * @param prefix the prefix to use for the name of the field
     * @param context the wiki context to use for computing the values
     * @see com.xpn.xwiki.objects.classes.PropertyClassInterface#displayEdit(StringBuffer, String, String,
     * BaseCollection, XWikiContext)
     */
    public void displayEdit(StringBuffer buffer, String name, String prefix, XWikiContext context)
    {
        ((PropertyClass) getXClass(context).get(name)).displayEdit(buffer, name, prefix, this, context);
    }

    /**
     * Display a hidden input for the given property.
     * @param name the name of the property to display
     * @param prefix the prefix to use for the name of the field
     * @param context the wiki context to use for computing the values
     * @return the string containing the display output
     * @see com.xpn.xwiki.objects.classes.PropertyClassInterface#displayHidden(StringBuffer, String, String,
     * BaseCollection, XWikiContext)
     */
    public String displayHidden(String name, String prefix, XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        displayHidden(buffer, name, prefix, context);
        return buffer.toString();
    }

    /**
     * Display the value of the given property.
     * @param name the name of the property to display
     * @param prefix the prefix to use for the name of the field
     * @param context the wiki context to use for computing the values
     * @return the string containing the display output
     * @see com.xpn.xwiki.objects.classes.PropertyClassInterface#displayView(StringBuffer, String, String,
     * BaseCollection, XWikiContext)
     */
    public String displayView(String name, String prefix, XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        displayView(buffer, name, prefix, context);
        return buffer.toString();
    }

    /**
     * Display an edit input of the given property in the given buffer.
     * @param name the name of the property to display
     * @param prefix the prefix to use for the name of the field
     * @param context the wiki context to use for computing the values
     * @return the string containing the display output
     * @see com.xpn.xwiki.objects.classes.PropertyClassInterface#displayEdit(StringBuffer, String, String,
     * BaseCollection, XWikiContext)
     */
    public String displayEdit(String name, String prefix, XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        displayEdit(buffer, name, prefix, context);
        return buffer.toString();
    }

    /**
     * Display a hidden input for the given property.
     * @param name the name of the property to display
     * @param context the wiki context to use for computing the values
     * @return the string containing the display output
     * @see com.xpn.xwiki.objects.classes.PropertyClassInterface#displayHidden(StringBuffer, String, String,
     * BaseCollection, XWikiContext)
     */
    public String displayHidden(String name, XWikiContext context)
    {
        return displayHidden(name, "", context);
    }

    /**
     * Display the value of the given property.
     * @param name the name of the property to display
     * @param context the wiki context to use for computing the values
     * @return the string containing the display output
     * @see com.xpn.xwiki.objects.classes.PropertyClassInterface#displayView(StringBuffer, String, String,
     * BaseCollection, XWikiContext)
     */
    public String displayView(String name, XWikiContext context)
    {
        return displayView(name, "", context);
    }

    /**
     * Display an edit input of the given property in the given buffer.
     * @param name the name of the property to display
     * @param context the wiki context to use for computing the values
     * @return the string containing the display output
     * @see com.xpn.xwiki.objects.classes.PropertyClassInterface#displayEdit(StringBuffer, String, String,
     * BaseCollection, XWikiContext)
     */
    public String displayEdit(String name, XWikiContext context)
    {
        return displayEdit(name, "", context);
    }

    @Override
    public BaseObject clone()
    {
        BaseObject object = (BaseObject) super.clone();
        // We don't use #getGuid() because we actually want the same value and not generate a new guid when null (which
        // is expensive)
        object.setGuid(this.guid);

        object.setDirty(isDirty());

        return object;
    }

    /**
     * Similar to {@link #clone()} but whereas a clone is an exact copy (with the same GUID), a duplicate keeps the same
     * data but with a different identity.
     * @return a duplicate of current instance
     *
     * @since 2.2.3
     */
    public BaseObject duplicate()
    {
        BaseObject object = clone();
        // Reset GUID for the duplicate
        object.setGuid(null);

        return object;
    }

    /**
     * Duplicate the current instance but set the given reference as document reference.
     * @param documentReference the new reference to use
     * @return a duplicate of current instance
     * @see #duplicate()
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
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.appendSuper(super.hashCode());
        builder.append(getNumber());

        return builder.toHashCode();
    }

    @Override
    public void fromXML(Element oel) throws XWikiException
    {
        super.fromXML(oel);
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
            String propertyType = (pclass == null) ? "" : pclass.getClassType();

            if (oldProperty == null) {
                // The property exist in the new object, but not in the old one
                if ((newProperty != null) && (!newProperty.toText().equals(""))) {
                    String newPropertyValue = (newProperty.getValue() instanceof String || pclass == null)
                        ? newProperty.toText() : pclass.displayView(propertyName, this, context);
                    difflist.add(new ObjectDiff(getXClassReference(), getNumber(), getGuid(),
                        ObjectDiff.ACTION_PROPERTYADDED, propertyName, propertyType, "", newPropertyValue));
                }
            } else if (!oldProperty.toText().equals(((newProperty == null) ? "" : newProperty.toText()))) {
                // The property exists in both objects and is different
                if (pclass != null) {
                    // Put the values as they would be displayed in the interface
                    String newPropertyValue = (newProperty.getValue() instanceof String) ? newProperty.toText()
                        : pclass.displayView(propertyName, this, context);
                    String oldPropertyValue = (oldProperty.getValue() instanceof String) ? oldProperty.toText()
                        : pclass.displayView(propertyName, oldObject, context);
                    difflist.add(
                        new ObjectDiff(getXClassReference(), getNumber(), getGuid(), ObjectDiff.ACTION_PROPERTYCHANGED,
                            propertyName, propertyType, oldPropertyValue, newPropertyValue));
                } else {
                    // Cannot get property definition, so use the plain value
                    difflist.add(
                        new ObjectDiff(getXClassReference(), getNumber(), getGuid(), ObjectDiff.ACTION_PROPERTYCHANGED,
                            propertyName, propertyType, oldProperty.toText(), newProperty.toText()));
                }
            }
        }

        // Iterate over the old properties, in case there are some removed properties
        for (String propertyName : oldObject.getPropertyList()) {
            BaseProperty newProperty = (BaseProperty) this.getField(propertyName);
            BaseProperty oldProperty = (BaseProperty) oldObject.getField(propertyName);
            BaseClass bclass = getXClass(context);
            // Bulletproofing: in theory the BaseObject is defined with a xclass reference allowing to resolve it
            // however, it's possible that the reference is not set, in which case we might still find the info
            // in the old object.
            if (bclass == null) {
                bclass = oldObject.getXClass(context);
            }
            PropertyClass pclass = (PropertyClass) ((bclass == null) ? null : bclass.getField(propertyName));
            String propertyType = (pclass == null) ? "" : pclass.getClassType();

            if (newProperty == null) {
                // The property exists in the old object, but not in the new one
                if ((oldProperty != null) && (!oldProperty.toText().equals(""))) {
                    if (pclass != null) {
                        // Put the values as they would be displayed in the interface
                        String oldPropertyValue = (oldProperty.getValue() instanceof String) ? oldProperty.toText()
                            : pclass.displayView(propertyName, oldObject, context);
                        difflist.add(
                            new ObjectDiff(oldObject.getXClassReference(), oldObject.getNumber(), oldObject.getGuid(),
                                ObjectDiff.ACTION_PROPERTYREMOVED, propertyName, propertyType, oldPropertyValue, ""));
                    } else {
                        // Cannot get property definition, so use the plain value
                        difflist.add(new ObjectDiff(oldObject.getXClassReference(), oldObject.getNumber(),
                            oldObject.getGuid(), ObjectDiff.ACTION_PROPERTYREMOVED, propertyName, propertyType,
                            oldProperty.toText(), ""));
                    }
                }
            }
        }

        return difflist;
    }

    /**
     * Wrap the given object in an {@link com.xpn.xwiki.api.Object}.
     * @param obj the object to wrap
     * @param context the context to use for wrapping
     * @return a new instance of the object to be used in scripts
     */
    public com.xpn.xwiki.api.Object newObjectApi(BaseObject obj, XWikiContext context)
    {
        return new com.xpn.xwiki.api.Object(obj, context);
    }

    /**
     * Set the defined property with the given value in the current object.
     * The given value might be a {@link String} or a type supported by the property. If a {@link String} is given
     * then {@link com.xpn.xwiki.objects.classes.PropertyClassInterface#fromString(String)} will be used.
     * @param fieldname the name of the property to set
     * @param value the value to set
     * @param context the context to use for setting the value
     * @throws XWikiException in case of problem when parsing the value
     */
    public void set(String fieldname, java.lang.Object value, XWikiContext context) throws XWikiException
    {
        BaseClass bclass = getXClass(context);
        PropertyClass pclass = (PropertyClass) bclass.get(fieldname);
        BaseProperty prop = (BaseProperty) safeget(fieldname);
        boolean createProp = false;
        if ((value instanceof String) && (pclass != null)) {
            BaseProperty newProp = pclass.fromString((String) value);
            if (prop == null) {
                prop = newProp;
                createProp = true;
            } else {
                prop.setValue(newProp.getValue());
            }
        } else {
            if ((prop == null) && (pclass != null)) {
                prop = pclass.newProperty();
                createProp = true;
            }
            if (prop != null) {
                prop.setValue(value);
            }
        }

        if (prop != null && createProp) {
            prop.setOwnerDocument(getOwnerDocument());
            safeput(fieldname, prop);
        }
    }

    /**
     * @return the unique identifier of the object, never null
     */
    public String getGuid()
    {
        if (this.guid == null) {
            return generateGuid();
        }

        return this.guid;
    }

    private synchronized String generateGuid()
    {
        if (this.guid == null) {
            this.guid = UUID.randomUUID().toString();
        }

        return this.guid;
    }

    /**
     * @param guid the unique identifier of the object, if null a new one will be generated
     */
    public void setGuid(String guid)
    {
        this.guid = guid;
    }

    /**
     * Set the owner document of this base object.
     *
     * @param ownerDocument The owner document.
     * @since 4.3M2
     */
    @Override
    public void setOwnerDocument(XWikiDocument ownerDocument)
    {
        if (this.ownerDocument != ownerDocument) {
            super.setOwnerDocument(ownerDocument);

            if (this.ownerDocument != null) {
                setDocumentReference(this.ownerDocument.getDocumentReference());
            }
        }
    }

    @Override
    protected void mergeField(PropertyInterface currentElement, ElementInterface previousElement,
        ElementInterface newElement, MergeConfiguration configuration, XWikiContext context, MergeResult mergeResult)
    {
        BaseClass baseClass = getXClass(context);
        if (baseClass != null) {
            PropertyClass propertyClass = (PropertyClass) baseClass.get(currentElement.getName());
            if (propertyClass != null) {
                try {
                    propertyClass.mergeProperty((BaseProperty) currentElement, (BaseProperty) previousElement,
                        (BaseProperty) newElement, configuration, context, mergeResult);
                } catch (Exception e) {
                    mergeResult.getLog().error("Failed to merge field [{}]", currentElement.getName(), e);
                }

                return;
            }
        }

        super.mergeField(currentElement, previousElement, newElement, configuration, context, mergeResult);
    }

    /**
     * Evaluates the properties of an object using a matching implementation of {@link ObjectEvaluator}.
     *
     * @return a Map storing the evaluated properties
     * @throws ObjectEvaluatorException if the evaluation fails
     * @since 14.10.21
     * @since 15.5.5
     * @since 15.10.2
     */
    @Unstable
    public Map<String, String> evaluate() throws ObjectEvaluatorException
    {
        ObjectEvaluator objectEvaluator = Utils.getComponent(ObjectEvaluator.class);
        return objectEvaluator.evaluate(this);
    }
}
