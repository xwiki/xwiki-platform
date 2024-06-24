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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.store.merge.MergeManagerResult;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.web.Utils;

/**
 * Base class for representing an element having a collection of properties. For example:
 * <ul>
 * <li>an XClass definition (composed of XClass properties)</li>
 * <li>an XObject definition (composed of XObject properties)</li>
 * <li>an XWikiStats object (composed of stats properties)</li>
 * </ul>
 *
 * @version $Id$
 */
public abstract class BaseCollection<R extends EntityReference> extends BaseElement<R>
    implements ObjectInterface, Cloneable
{
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseCollection.class);

    /**
     * The meaning of this reference fields depends on the element represented. Examples:
     * <ul>
     * <li>If this BaseCollection instance represents an XObject then refers to the document where the XObject's XClass
     * is defined.</li>
     * <li>If this BaseCollection instance represents an XClass then it's not used.</li>
     * </ul>
     */
    private EntityReference xClassReference;

    /**
     * Cache the XClass reference resolved as an absolute reference for improved performance (so that we don't have to
     * resolve the relative reference every time getXClassReference() is called.
     */
    private DocumentReference xClassReferenceCache;

    /**
     * List of properties (eg XClass properties, XObject properties, etc).
     */
    protected Map<String, Object> fields = new LinkedHashMap<String, Object>();

    protected List<Object> fieldsToRemove = new ArrayList<>();

    /**
     * The meaning of this reference fields depends on the element represented. Examples:
     * <ul>
     * <li>When the BaseCollection represents an XObject, this number is the position of this XObject in the document
     * where it's located. The first XObject of a given XClass type is at position 0, and other XObject of the same
     * XClass type are at position 1, etc.</li>
     * </ul>
     */
    protected int number;

    /**
     * Used to resolve XClass references in the way they are stored externally (database, xml, etc), ie relative or
     * absolute.
     */
    protected EntityReferenceResolver<String> relativeEntityReferenceResolver;

    /**
     * Used to normalize references.
     */
    protected DocumentReferenceResolver<EntityReference> currentReferenceDocumentReferenceResolver;

    /**
     * @return the component used to resolve XClass references in the way they are stored externally (database, xml,
     *         etc), ie relative or absolute
     */
    protected EntityReferenceResolver<String> getRelativeEntityReferenceResolver()
    {
        if (this.relativeEntityReferenceResolver == null) {
            this.relativeEntityReferenceResolver = Utils.getComponent(EntityReferenceResolver.TYPE_STRING, "relative");
        }

        return this.relativeEntityReferenceResolver;
    }

    /**
     * @return the component used to normalize references
     */
    protected DocumentReferenceResolver<EntityReference> getCurrentReferenceDocumentReferenceResolver()
    {
        if (this.currentReferenceDocumentReferenceResolver == null) {
            this.currentReferenceDocumentReferenceResolver =
                Utils.getComponent(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        }

        return this.currentReferenceDocumentReferenceResolver;
    }

    public int getNumber()
    {
        return this.number;
    }

    public void setNumber(int number)
    {
        this.number = number;
    }

    /**
     * Marks a field as scheduled for removal when saving this entity. Should only be used internally, use
     * {@link #removeField(String)} to actually remove a field.
     *
     * @param field the field to remove, must belong to this entity
     * @see #removeField(String)
     */
    public void addPropertyForRemoval(PropertyInterface field)
    {
        getFieldsToRemove().add(field);
    }

    /**
     * Get the absolute reference of the XClass.
     *
     * @since 2.2M2
     */
    public DocumentReference getXClassReference()
    {
        if (this.xClassReferenceCache == null && getRelativeXClassReference() != null) {
            this.xClassReferenceCache = getCurrentReferenceDocumentReferenceResolver()
                .resolve(getRelativeXClassReference(), getDocumentReference());
        }

        return this.xClassReferenceCache;
    }

    /**
     * Get the actual reference to the XClass as stored in this instance.
     *
     * @since 4.0M2
     */
    public EntityReference getRelativeXClassReference()
    {
        return this.xClassReference;
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for saving an XObject.
     *
     * @deprecated since 2.2M2 use {@link #getXClassReference()} instead
     */
    @Deprecated
    public String getClassName()
    {
        String xClassAsString;
        if (getXClassReference() != null) {
            xClassAsString = getLocalEntityReferenceSerializer().serialize(getXClassReference());
        } else {
            xClassAsString = "";
        }
        return xClassAsString;
    }

    /**
     * Set the reference to the XClass (used for an XObject).
     * <p>
     * Note that absolute reference are not supported for xclasses which mean that the wiki part (whatever the wiki is)
     * of the reference will be systematically removed.
     *
     * @param xClassReference the reference to the XClass of this XObject.
     * @since 2.2.3
     */
    public void setXClassReference(EntityReference xClassReference)
    {
        // Ensure that the reference to the XClass is always relative to the document wiki.
        EntityReference ref = xClassReference;

        if (ref != null) {
            EntityReference wiki = xClassReference.extractReference(EntityType.WIKI);
            if (wiki != null) {
                ref = xClassReference.removeParent(wiki);
            }
        }

        this.xClassReference = ref;
        this.xClassReferenceCache = null;
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for loading an XObject.
     *
     * @deprecated since 2.2.3 use {@link #setXClassReference(EntityReference)} ()} instead
     */
    @Deprecated
    public void setClassName(String name)
    {
        EntityReference xClassReference = null;
        if (!StringUtils.isEmpty(name)) {
            // Handle backward compatibility: In the past, for statistics objects we used to use a special class name
            // of "internal". We now check for a null Class Reference instead wherever we were previously checking for
            // "internal".
            if (!"internal".equals(name)) {
                xClassReference = getRelativeEntityReferenceResolver().resolve(name, EntityType.DOCUMENT);
            }
        }
        setXClassReference(xClassReference);
    }

    @Override
    public PropertyInterface safeget(String name)
    {
        return (PropertyInterface) getFields().get(name);
    }

    @Override
    public PropertyInterface get(String name) throws XWikiException
    {
        return safeget(name);
    }

    @Override
    public void safeput(String name, PropertyInterface property)
    {
        addField(name, property);
        if (property instanceof BaseProperty) {
            ((BaseProperty) property).setObject(this);
            ((BaseProperty) property).setName(name);
        }
    }

    @Override
    public void put(String name, PropertyInterface property) throws XWikiException
    {
        safeput(name, property);
    }

    /**
     * @since 2.2M1
     */
    @Override
    public BaseClass getXClass(XWikiContext context)
    {
        BaseClass baseClass = null;

        if ((context == null) || (context.getWiki() == null)) {
            return baseClass;
        }

        DocumentReference classReference = getXClassReference();

        if (classReference != null) {
            try {
                baseClass = context.getWiki().getXClass(classReference, context);
            } catch (Exception e) {
                LOGGER.error("Failed to get class [" + classReference + "]", e);
            }
        }

        return baseClass;
    }

    public String getStringValue(String name)
    {
        BaseProperty prop = (BaseProperty) safeget(name);
        if (prop == null || prop.getValue() == null) {
            return "";
        } else {
            return prop.getValue().toString();
        }
    }

    public String getLargeStringValue(String name)
    {
        return getStringValue(name);
    }

    public void setStringValue(String name, String value)
    {
        BaseStringProperty property = (BaseStringProperty) safeget(name);

        if (!(property instanceof StringProperty)) {
            if (property != null) {
                // Make sure to delete the property if it's not the right type
                removeField(name);
            }

            property = new StringProperty();
        }

        property.setName(name);
        property.setValue(value);

        safeput(name, property);
    }

    public void setLargeStringValue(String name, String value)
    {
        BaseStringProperty property = (BaseStringProperty) safeget(name);

        if (!(property instanceof LargeStringProperty)) {
            if (property != null) {
                // Make sure to delete the property if it's not the right type
                removeField(name);
            }

            property = new LargeStringProperty();
        }

        property.setName(name);
        property.setValue(value);

        safeput(name, property);
    }

    public int getIntValue(String name)
    {
        return getIntValue(name, 0);
    }

    public int getIntValue(String name, int default_value)
    {
        try {
            NumberProperty prop = (NumberProperty) safeget(name);
            if (prop == null) {
                return default_value;
            } else {
                return ((Number) prop.getValue()).intValue();
            }
        } catch (Exception e) {
            return default_value;
        }
    }

    public void setIntValue(String name, int value)
    {
        NumberProperty property = new IntegerProperty();
        property.setName(name);
        property.setValue(value);
        safeput(name, property);
    }

    public long getLongValue(String name)
    {
        try {
            NumberProperty prop = (NumberProperty) safeget(name);
            if (prop == null) {
                return 0;
            } else {
                return ((Number) prop.getValue()).longValue();
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public void setLongValue(String name, long value)
    {
        NumberProperty property = new LongProperty();
        property.setName(name);
        property.setValue(value);
        safeput(name, property);
    }

    public float getFloatValue(String name)
    {
        try {
            NumberProperty prop = (NumberProperty) safeget(name);
            if (prop == null) {
                return 0;
            } else {
                return ((Number) prop.getValue()).floatValue();
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public void setFloatValue(String name, float value)
    {
        NumberProperty property = new FloatProperty();
        property.setName(name);
        property.setValue(Float.valueOf(value));
        safeput(name, property);
    }

    public double getDoubleValue(String name)
    {
        try {
            NumberProperty prop = (NumberProperty) safeget(name);
            if (prop == null) {
                return 0;
            } else {
                return ((Number) prop.getValue()).doubleValue();
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public void setDoubleValue(String name, double value)
    {
        NumberProperty property = new DoubleProperty();
        property.setName(name);
        property.setValue(Double.valueOf(value));
        safeput(name, property);
    }

    public Date getDateValue(String name)
    {
        try {
            DateProperty prop = (DateProperty) safeget(name);
            if (prop == null) {
                return null;
            } else {
                return (Date) prop.getValue();
            }
        } catch (Exception e) {
            return null;
        }
    }

    public void setDateValue(String name, Date value)
    {
        DateProperty property = new DateProperty();
        property.setName(name);
        property.setValue(value);
        safeput(name, property);
    }

    public Set<?> getSetValue(String name)
    {
        ListProperty prop = (ListProperty) safeget(name);
        if (prop == null) {
            return new HashSet<Object>();
        } else {
            return new HashSet<Object>((Collection<?>) prop.getValue());
        }
    }

    public void setSetValue(String name, Set<?> value)
    {
        ListProperty property = new ListProperty();
        property.setValue(value);
        safeput(name, property);
    }

    public List getListValue(String name)
    {
        ListProperty prop = (ListProperty) safeget(name);
        if (prop == null) {
            return new ArrayList();
        } else {
            return (List) prop.getValue();
        }
    }

    public void setStringListValue(String name, List value)
    {
        ListProperty property = (ListProperty) safeget(name);
        if (property == null) {
            property = new StringListProperty();
        }
        property.setValue(value);
        safeput(name, property);
    }

    public void setDBStringListValue(String name, List value)
    {
        ListProperty property = (ListProperty) safeget(name);
        if (property == null) {
            property = new DBStringListProperty();
        }
        property.setValue(value);
        safeput(name, property);
    }

    // These functions should not be used
    // but instead our own implementation
    private Map<String, Object> getFields()
    {
        return this.fields;
    }

    public void setFields(Map fields)
    {
        this.fields = fields;
    }

    public PropertyInterface getField(String name)
    {
        return (PropertyInterface) this.fields.get(name);
    }

    public void addField(String name, PropertyInterface element)
    {
        this.fields.put(name, element);

        if (element instanceof BaseElement) {
            ((BaseElement) element).setOwnerDocument(getOwnerDocument());
        }
    }

    public void removeField(String name)
    {
        Object field = safeget(name);
        if (field != null) {
            this.fields.remove(name);
            this.fieldsToRemove.add(field);
        }
    }

    public Collection getFieldList()
    {
        return this.fields.values();
    }

    public Set<String> getPropertyList()
    {
        return this.fields.keySet();
    }

    public Object[] getProperties()
    {
        return getFields().values().toArray();
    }

    public String[] getPropertyNames()
    {
        return getFields().keySet().toArray(new String[0]);
    }

    /**
     * Return an iterator that will operate on a collection of values (as would be returned by getProperties or
     * getFieldList) sorted by their name (ElementInterface.getName()).
     */
    public Iterator getSortedIterator()
    {
        Iterator it = null;
        try {
            // Use getProperties to get the values in list form (rather than as generic collection)
            List propList = Arrays.asList(getProperties());

            // Use the element comparator to sort the properties by name (based on ElementInterface)
            Collections.sort(propList, new ElementComparator());

            // Iterate over the sorted property list
            it = propList.iterator();
        } catch (ClassCastException ccex) {
            // If sorting by the comparator resulted in a ClassCastException (possible),
            // iterate over the generic collection of values.
            it = getFieldList().iterator();
        }

        return it;
    }

    @Override
    public boolean equals(Object coll)
    {
        // Same Java object, they sure are equal
        if (this == coll) {
            return true;
        }

        if (!super.equals(coll)) {
            return false;
        }
        BaseCollection collection = (BaseCollection) coll;
        if (collection.getXClassReference() == null) {
            if (getXClassReference() != null) {
                return false;
            }
        } else if (!collection.getXClassReference().equals(getXClassReference())) {
            return false;
        }

        if (getFields().size() != collection.getFields().size()) {
            return false;
        }

        for (Map.Entry<String, Object> entry : getFields().entrySet()) {
            Object prop = entry.getValue();
            Object prop2 = collection.getFields().get(entry.getKey());
            if (!prop.equals(prop2)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public BaseCollection clone()
    {
        BaseCollection collection = (BaseCollection) super.clone();
        collection.setXClassReference(getRelativeXClassReference());
        collection.setNumber(getNumber());
        Map fields = getFields();
        Map cfields = new HashMap();
        for (Object objEntry : fields.entrySet()) {
            Map.Entry entry = (Map.Entry) objEntry;
            PropertyInterface prop = (PropertyInterface) ((BaseElement) entry.getValue()).clone();
            prop.setObject(collection);
            cfields.put(entry.getKey(), prop);
        }
        collection.setFields(cfields);

        return collection;
    }

    public void merge(BaseObject object)
    {
        Iterator itfields = object.getPropertyList().iterator();
        while (itfields.hasNext()) {
            String name = (String) itfields.next();
            if (safeget(name) == null) {
                safeput(name, (PropertyInterface) ((BaseElement) object.safeget(name)).clone());
            }
        }
    }

    public List<ObjectDiff> getDiff(Object oldObject, XWikiContext context)
    {
        ArrayList<ObjectDiff> difflist = new ArrayList<ObjectDiff>();
        BaseCollection oldCollection = (BaseCollection) oldObject;
        // Iterate over the new properties first, to handle changed and added objects
        for (Object key : this.getFields().keySet()) {
            String propertyName = (String) key;
            BaseProperty newProperty = (BaseProperty) this.getFields().get(propertyName);
            BaseProperty oldProperty = (BaseProperty) oldCollection.getFields().get(propertyName);
            BaseClass bclass = getXClass(context);
            PropertyClass pclass = (PropertyClass) ((bclass == null) ? null : bclass.getField(propertyName));
            String propertyType = (pclass == null) ? "" : pclass.getClassType();

            if (oldProperty == null) {
                // The property exist in the new object, but not in the old one
                if ((newProperty != null) && (!newProperty.toText().equals(""))) {
                    if (pclass != null) {
                        String newPropertyValue = (newProperty.getValue() instanceof String) ? newProperty.toText()
                            : pclass.displayView(propertyName, this, context);
                        difflist.add(new ObjectDiff(getXClassReference(), getNumber(), "",
                            ObjectDiff.ACTION_PROPERTYADDED, propertyName, propertyType, "", newPropertyValue));
                    }
                }
            } else if (!oldProperty.toText().equals(((newProperty == null) ? "" : newProperty.toText()))) {
                // The property exists in both objects and is different
                if (pclass != null) {
                    // Put the values as they would be displayed in the interface
                    String newPropertyValue = (newProperty.getValue() instanceof String) ? newProperty.toText()
                        : pclass.displayView(propertyName, this, context);
                    String oldPropertyValue = (oldProperty.getValue() instanceof String) ? oldProperty.toText()
                        : pclass.displayView(propertyName, oldCollection, context);
                    difflist
                        .add(new ObjectDiff(getXClassReference(), getNumber(), "", ObjectDiff.ACTION_PROPERTYCHANGED,
                            propertyName, propertyType, oldPropertyValue, newPropertyValue));
                } else {
                    // Cannot get property definition, so use the plain value
                    difflist
                        .add(new ObjectDiff(getXClassReference(), getNumber(), "", ObjectDiff.ACTION_PROPERTYCHANGED,
                            propertyName, propertyType, oldProperty.toText(), newProperty.toText()));
                }
            }
        }

        // Iterate over the old properties, in case there are some removed properties
        for (Object key : oldCollection.getFields().keySet()) {
            String propertyName = (String) key;
            BaseProperty newProperty = (BaseProperty) this.getFields().get(propertyName);
            BaseProperty oldProperty = (BaseProperty) oldCollection.getFields().get(propertyName);
            BaseClass bclass = getXClass(context);
            PropertyClass pclass = (PropertyClass) ((bclass == null) ? null : bclass.getField(propertyName));
            String propertyType = (pclass == null) ? "" : pclass.getClassType();

            if (newProperty == null) {
                // The property exists in the old object, but not in the new one
                if ((oldProperty != null) && (!oldProperty.toText().equals(""))) {
                    if (pclass != null) {
                        // Put the values as they would be displayed in the interface
                        String oldPropertyValue = (oldProperty.getValue() instanceof String) ? oldProperty.toText()
                            : pclass.displayView(propertyName, oldCollection, context);
                        difflist.add(new ObjectDiff(oldCollection.getXClassReference(), oldCollection.getNumber(), "",
                            ObjectDiff.ACTION_PROPERTYREMOVED, propertyName, propertyType, oldPropertyValue, ""));
                    } else {
                        // Cannot get property definition, so use the plain value
                        difflist.add(new ObjectDiff(oldCollection.getXClassReference(), oldCollection.getNumber(), "",
                            ObjectDiff.ACTION_PROPERTYREMOVED, propertyName, propertyType, oldProperty.toText(), ""));
                    }
                }
            }
        }

        return difflist;
    }

    public List getFieldsToRemove()
    {
        return this.fieldsToRemove;
    }

    public void setFieldsToRemove(List fieldsToRemove)
    {
        this.fieldsToRemove = fieldsToRemove;
    }

    @Override
    public Element toXML()
    {
        return super.toXML();
    }

    /**
     * @deprecated since 9.0RC1, use {@link #toXML()} instead
     */
    @Override
    @Deprecated
    public Element toXML(BaseClass bclass)
    {
        // Set passed class in the context so that the input event generator finds it
        XWikiContext xcontext = getXWikiContext();

        BaseClass currentBaseClass;
        DocumentReference classReference;
        if (bclass != null && xcontext != null) {
            classReference = bclass.getDocumentReference();
            currentBaseClass = xcontext.getBaseClass(bclass.getDocumentReference());
            xcontext.addBaseClass(bclass);
        } else {
            classReference = null;
            currentBaseClass = null;
        }

        try {
            return super.toXML();
        } finally {
            if (classReference != null) {
                if (currentBaseClass != null) {
                    xcontext.addBaseClass(currentBaseClass);
                } else {
                    xcontext.removeBaseClass(classReference);
                }
            }
        }
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
        return toXMLString(true);
    }

    /**
     * @since 2.4M2
     */
    public Map<String, Object> getCustomMappingMap() throws XWikiException
    {
        Map<String, Object> map = new HashMap<String, Object>();
        for (String name : this.fields.keySet()) {
            BaseProperty property = (BaseProperty) get(name);
            map.put(name, property.getCustomMappingValue());
        }
        map.put("id", getId());

        return map;
    }

    @Override
    public void setDocumentReference(DocumentReference reference)
    {
        super.setDocumentReference(reference);

        // We force to refresh the XClass reference so that next time it's retrieved again it'll be resolved against
        // the new document reference.
        this.xClassReferenceCache = null;
    }

    @Override
    public MergeManagerResult<ElementInterface, Object> merge(ElementInterface previousElement,
        ElementInterface newElement, MergeConfiguration configuration, XWikiContext context)
    {
        MergeManagerResult<ElementInterface, Object> mergeResult =
            super.merge(previousElement, newElement, configuration, context);
        BaseCollection<R> previousCollection = (BaseCollection<R>) previousElement;
        BaseCollection<R> newCollection = (BaseCollection<R>) newElement;

        BaseCollection<R> modifiableResult = (BaseCollection<R>) mergeResult.getMergeResult();
        List<ObjectDiff> classDiff = newCollection.getDiff(previousCollection, context);
        for (ObjectDiff diff : classDiff) {
            PropertyInterface propertyResult = getField(diff.getPropName());
            PropertyInterface previousProperty = previousCollection.getField(diff.getPropName());
            PropertyInterface newProperty = newCollection.getField(diff.getPropName());

            if (ObjectDiff.ACTION_PROPERTYADDED.equals(diff.getAction())) {
                if (propertyResult == null) {
                    // Add if none has been added by user already
                    modifiableResult.safeput(diff.getPropName(),
                        configuration.isProvidedVersionsModifiables() ? newProperty : newProperty.clone());
                    mergeResult.setModified(true);
                } else if (!propertyResult.equals(newProperty)) {
                    // collision between DB and new: property to add but already exists in the DB
                    // If we need to fallback on next version, set next version.
                    if (configuration.getConflictFallbackVersion() == MergeConfiguration.ConflictFallbackVersion.NEXT) {
                        modifiableResult.safeput(diff.getPropName(),
                            configuration.isProvidedVersionsModifiables() ? newProperty : newProperty.clone());
                        mergeResult.setModified(true);
                    }
                    mergeResult.getLog().error("Collision found on property [{}]", newProperty.getReference());
                }
            } else if (ObjectDiff.ACTION_PROPERTYREMOVED.equals(diff.getAction())) {
                if (propertyResult != null) {
                    if (propertyResult.equals(previousProperty)) {
                        // Delete if it's the same as previous one
                        modifiableResult.removeField(diff.getPropName());
                        mergeResult.setModified(true);
                    } else {
                        // collision between DB and new: property to remove but not the same as previous
                        // version
                        // We don't remove the field in case of fallback.
                        mergeResult.getLog().error("Collision found on property [{}]", previousProperty.getReference());
                    }
                } else {
                    // Already removed from DB, lets assume the user is prescient
                    mergeResult.getLog().warn("Property [{}] already removed", previousProperty.getReference());
                }
            } else if (ObjectDiff.ACTION_PROPERTYCHANGED.equals(diff.getAction())) {
                if (propertyResult != null) {
                    if (propertyResult.equals(previousProperty)) {
                        // Let some automatic migration take care of that modification between DB and new
                        modifiableResult.safeput(diff.getPropName(),
                            configuration.isProvidedVersionsModifiables() ? newProperty : newProperty.clone());
                        mergeResult.setModified(true);
                    } else if (!propertyResult.equals(newProperty)) {
                        // Try to apply 3 ways merge on the property
                        // FIXME: we should deprecate mergeField and rewrite it properly, but it's a lot of work
                        // as it involves to also rewrite PropertyClass#mergeProperty
                        // right now we still use it, but we ensure that the configuration is used to modify the
                        // actual values, and not clones as it was the behaviour
                        MergeResult propertyMergeResult = new MergeResult();
                        MergeConfiguration propertyMergeConfiguration = new MergeConfiguration();
                        propertyMergeConfiguration.setConcernedDocument(configuration.getConcernedDocument());
                        propertyMergeConfiguration.setUserReference(configuration.getUserReference());
                        propertyMergeConfiguration.setProvidedVersionsModifiables(true);
                        propertyMergeConfiguration.setConflictFallbackVersion(
                            configuration.getConflictFallbackVersion());
                        mergeField(propertyResult, previousProperty, newProperty, propertyMergeConfiguration, context,
                            propertyMergeResult);
                        mergeResult.getLog().addAll(propertyMergeResult.getLog());
                        if (propertyMergeResult.isModified()) {
                            mergeResult.setModified(true);
                        }
                    }
                } else {
                    // collision between DB and new: property to modify but does not exist in DB
                    // Lets assume it's a mistake to fix
                    mergeResult.getLog().warn("Collision found on property [{}]", newProperty.getReference());

                    modifiableResult.safeput(diff.getPropName(),
                        configuration.isProvidedVersionsModifiables() ? newProperty : newProperty.clone());
                    mergeResult.setModified(true);
                }
            }
        }
        return mergeResult;
    }

    protected void mergeField(PropertyInterface currentElement, ElementInterface previousElement,
        ElementInterface newElement, MergeConfiguration configuration, XWikiContext context, MergeResult mergeResult)
    {
        currentElement.merge(previousElement, newElement, configuration, context, mergeResult);
    }

    @Override
    public boolean apply(ElementInterface newElement, boolean clean)
    {
        boolean modified = false;

        BaseCollection<R> newCollection = (BaseCollection<R>) newElement;

        if (clean) {
            // Delete fields that don't exist anymore
            List<String> fieldsToDelete = new ArrayList<String>(this.fields.size());
            for (String key : this.fields.keySet()) {
                if (newCollection.safeget(key) == null) {
                    fieldsToDelete.add(key);
                }
            }

            for (String key : fieldsToDelete) {
                removeField(key);
                modified = true;
            }
        }

        // Add new fields and update existing fields
        for (Map.Entry<String, Object> entry : newCollection.fields.entrySet()) {
            PropertyInterface field = (PropertyInterface) this.fields.get(entry.getKey());
            PropertyInterface newField = (PropertyInterface) entry.getValue();

            if (field == null) {
                // If the field does not exist add it
                safeput(entry.getKey(), newField);
                modified = true;
            } else if (field.getClass() != newField.getClass()) {
                // If the field is of different type, remove it first
                removeField(entry.getKey());
                safeput(entry.getKey(), newField);
                modified = true;
            } else {
                // Otherwise try to merge the fields
                modified |= field.apply(newField, clean);
            }
        }

        return modified;
    }

    /**
     * Set the owner document of this base object.
     *
     * @param ownerDocument The owner document.
     * @since 5.3M1
     */
    @Override
    public void setOwnerDocument(XWikiDocument ownerDocument)
    {
        if (this.ownerDocument != ownerDocument) {
            super.setOwnerDocument(ownerDocument);

            for (String propertyName : getPropertyList()) {
                PropertyInterface property = getField(propertyName);
                if (property instanceof BaseElement) {
                    ((BaseElement) property).setOwnerDocument(ownerDocument);
                }
            }
        }
    }
}
