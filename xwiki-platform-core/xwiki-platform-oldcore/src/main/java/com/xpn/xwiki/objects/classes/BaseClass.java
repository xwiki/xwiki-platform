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
package com.xpn.xwiki.objects.classes;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.SAXReader;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.internal.merge.MergeUtils;
import com.xpn.xwiki.internal.objects.classes.PropertyClassProvider;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ElementInterface;
import com.xpn.xwiki.objects.ObjectDiff;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.validation.XWikiValidationInterface;
import com.xpn.xwiki.validation.XWikiValidationStatus;
import com.xpn.xwiki.web.Utils;

/**
 * Represents an XClass, and contains XClass properties. Each field from {@link BaseCollection} is of type
 * {@link PropertyClass} and defines a single XClass property.
 * 
 * @version $Id$
 */
public class BaseClass extends BaseCollection<DocumentReference> implements ClassInterface
{
    private String customMapping;

    private String customClass;

    private String defaultWeb;

    private String defaultViewSheet;

    private String defaultEditSheet;

    private String validationScript;

    private String nameField;

    /**
     * Set to true if the class is modified from the database version of it.
     */
    private boolean isDirty = true;

    /**
     * Used to resolve a string into a proper Document Reference using the current document's reference to fill the
     * blanks, except for the page name for which the default page name is used instead and for the wiki name for which
     * the current wiki is used instead of the current document reference's wiki.
     */
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.TYPE_STRING, "currentmixed");

    @Override
    public DocumentReference getReference()
    {
        return getDocumentReference();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note: This method is overridden to add the deprecation warning so that code using is can see it's deprecated.
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
     * Note: BaseElement#setName() does not support setting reference anymore since 2.4M2. This was broken and has been
     * replaced by this overridden method. See XWIKI-5285
     * 
     * @deprecated since 2.2M2 use {@link #setDocumentReference(org.xwiki.model.reference.DocumentReference)}
     */
    @Deprecated
    @Override
    public void setName(String name)
    {
        if (this instanceof MetaClass || this instanceof PropertyMetaClass) {
            super.setName(name);
        } else {
            DocumentReference reference = getDocumentReference();

            if (reference != null) {
                EntityReference relativeReference =
                    this.relativeEntityReferenceResolver.resolve(name, EntityType.DOCUMENT);
                reference =
                    new DocumentReference(relativeReference.extractReference(EntityType.DOCUMENT).getName(),
                        new SpaceReference(relativeReference.extractReference(EntityType.SPACE).getName(), reference
                            .getParent().getParent()));
            } else {
                reference = this.currentMixedDocumentReferenceResolver.resolve(name);
            }
            setDocumentReference(reference);
        }
        setDirty(true);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This insures natural ordering between properties.
     * 
     * @see com.xpn.xwiki.objects.BaseCollection#addField(java.lang.String, com.xpn.xwiki.objects.PropertyInterface)
     */
    @Override
    public void addField(String name, PropertyInterface element)
    {
        Set<String> properties = getPropertyList();
        if (!properties.contains(name)) {
            if (((BaseCollection) element).getNumber() == 0) {
                ((BaseCollection) element).setNumber(properties.size() + 1);
            }
        }

        super.addField(name, element);

        setDirty(true);
    }

    /**
     * Mark a property as disabled. A disabled property should not be editable, but existing object values are still
     * kept in the database.
     * 
     * @param name the name of the property to disable
     * @since 2.4M2
     */
    public void disableField(String name)
    {
        PropertyClass pclass = (PropertyClass) safeget(name);

        if (pclass != null) {
            pclass.setDisabled(true);
        }

        setDirty(true);
    }

    /**
     * Re-enable a property. This field will appear again in object instances.
     * 
     * @param name the name of the property to enable
     * @since 2.4M2
     */
    public void enableField(String name)
    {
        PropertyClass pclass = (PropertyClass) safeget(name);

        if (pclass != null) {
            pclass.setDisabled(false);
        }

        setDirty(true);
    }

    @Override
    public PropertyInterface get(String name)
    {
        return safeget(name);
    }

    @Override
    public void put(String name, PropertyInterface property)
    {
        safeput(name, property);
        setDirty(true);
    }

    /**
     * Get the list of enabled (the default, normal state) property definitions that exist in this class. The resulting
     * list is unmodifiable, but the contained elements are live.
     * 
     * @return an unmodifiable list containing the enabled properties of the class
     * @see PropertyClass#isDisabled()
     * @since 2.4M2
     */
    public List<PropertyClass> getEnabledProperties()
    {
        @SuppressWarnings("unchecked")
        Collection<PropertyClass> allProperties = getFieldList();
        if (allProperties == null) {
            return Collections.emptyList();
        }

        List<PropertyClass> enabledProperties = new ArrayList<PropertyClass>(allProperties.size());

        for (PropertyClass property : allProperties) {
            if (property != null && !property.isDisabled()) {
                enabledProperties.add(property);
            }
        }

        Collections.sort(enabledProperties);
        return Collections.unmodifiableList(enabledProperties);
    }

    /**
     * Get the list of disabled property definitions that exist in this class. The resulting list is unmodifiable, but
     * the contained elements are live.
     * 
     * @return an unmodifiable list containing the disabled properties of the class
     * @see PropertyClass#isDisabled()
     * @since 2.4M2
     */
    public List<PropertyClass> getDisabledProperties()
    {
        @SuppressWarnings("unchecked")
        Collection<PropertyClass> allProperties = getFieldList();
        if (allProperties == null) {
            return Collections.emptyList();
        }

        List<PropertyClass> disabledProperties = new ArrayList<PropertyClass>();

        for (PropertyClass property : allProperties) {
            if (property != null && property.isDisabled()) {
                disabledProperties.add(property);
            }
        }

        Collections.sort(disabledProperties);
        return Collections.unmodifiableList(disabledProperties);
    }

    /**
     * Get the list of disabled properties that exist in a given object. This list is a subset of all the disabled
     * properties in a class, since the object could have been created and stored before some of the class properties
     * were added. The resulting list is unmodifiable, but the contained elements are live.
     * 
     * @param object the instance of this class where the disabled properties must exist
     * @return an unmodifiable list containing the disabled properties of the given object
     * @see PropertyClass#isDisabled()
     * @since 2.4M2
     */
    public List<PropertyClass> getDisabledObjectProperties(BaseObject object)
    {
        List<PropertyClass> disabledProperties = getDisabledProperties();
        if (disabledProperties == null) {
            return Collections.emptyList();
        }

        List<PropertyClass> disabledObjectProperties = new ArrayList<PropertyClass>(disabledProperties.size());

        for (PropertyClass property : disabledProperties) {
            try {
                if (object.get(property.getName()) != null) {
                    disabledObjectProperties.add(property);
                }
            } catch (XWikiException ex) {
                // Not really gonna happen
            }
        }

        return Collections.unmodifiableList(disabledObjectProperties);
    }

    /**
     * Retrieves deprecated properties of the given object compared to the class. A deprecated property is a property
     * which exists in the Object but doesn't exist anymore in the Class, or which has the wrong data type. This is used
     * for synchronization of existing or imported Objects with respect to the modifications of their associated Class.
     * 
     * @param object the instance of this class where to look for undefined properties
     * @return an unmodifiable list containing the properties of the object which don't exist in the class
     * @since 2.4M2
     */
    public List<BaseProperty> getDeprecatedObjectProperties(BaseObject object)
    {
        @SuppressWarnings("unchecked")
        Collection<BaseProperty> objectProperties = object.getFieldList();
        if (objectProperties == null) {
            return Collections.emptyList();
        }

        List<BaseProperty> deprecatedObjectProperties = new ArrayList<BaseProperty>();

        for (BaseProperty property : objectProperties) {
            if (safeget(property.getName()) == null) {
                deprecatedObjectProperties.add(property);
            } else {
                String propertyClass = ((PropertyClass) safeget(property.getName())).newProperty().getClassType();
                String objectPropertyClass = property.getClassType();

                if (!propertyClass.equals(objectPropertyClass)) {
                    deprecatedObjectProperties.add(property);
                }
            }
        }

        return Collections.unmodifiableList(deprecatedObjectProperties);
    }

    public BaseProperty fromString(String value)
    {
        return null; // To change body of implemented methods use Options | File Templates.
    }

    /**
     * @deprecated since 2.2.3 use {@link com.xpn.xwiki.doc.XWikiDocument#newXObject}
     */
    @Deprecated
    public BaseCollection newObject(XWikiContext context) throws XWikiException
    {
        BaseObject bobj = newCustomClassInstance(context);
        DocumentReference classReference = getDocumentReference();
        bobj.setXClassReference(classReference.removeParent(classReference.getWikiReference()));

        return bobj;
    }

    /**
     * @deprecated since 2.2.3 use {@link #fromMap(java.util.Map, com.xpn.xwiki.objects.BaseCollection)}
     */
    @Deprecated
    public BaseCollection fromMap(Map<String, ? > map, XWikiContext context) throws XWikiException
    {
        BaseCollection object = newObject(context);

        return fromMap(map, object);
    }

    public BaseCollection fromMap(Map<String, ? > map, BaseCollection object)
    {
        for (PropertyClass property : (Collection<PropertyClass>) getFieldList()) {
            String name = property.getName();
            Object formvalues = map.get(name);
            if (formvalues != null) {
                BaseProperty objprop;
                if (formvalues instanceof String[]) {
                    objprop = property.fromStringArray(((String[]) formvalues));
                } else {
                    objprop = property.fromString(formvalues.toString());
                }

                if (objprop != null) {
                    objprop.setObject(object);
                    object.safeput(name, objprop);
                }
            }
        }

        return object;
    }

    public BaseCollection fromValueMap(Map<String, ? > map, BaseCollection object)
    {
        for (PropertyClass property : (Collection<PropertyClass>) getFieldList()) {
            String name = property.getName();
            Object formvalue = map.get(name);
            if (formvalue != null) {
                BaseProperty objprop;
                objprop = property.fromValue(formvalue);
                if (objprop != null) {
                    objprop.setObject(object);
                    object.safeput(name, objprop);
                }
            }
        }

        return object;
    }

    @Override
    public BaseClass clone()
    {
        BaseClass bclass = (BaseClass) super.clone();
        bclass.setCustomClass(getCustomClass());
        bclass.setCustomMapping(getCustomMapping());
        bclass.setDefaultWeb(getDefaultWeb());
        bclass.setDefaultViewSheet(getDefaultViewSheet());
        bclass.setDefaultEditSheet(getDefaultEditSheet());
        bclass.setNameField(getNameField());
        bclass.setDirty(this.isDirty);
        bclass.setOwnerDocument(this.ownerDocument);

        return bclass;
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

        BaseClass bclass = (BaseClass) obj;

        if (!getCustomClass().equals(bclass.getCustomClass())) {
            return false;
        }

        if (!getCustomMapping().equals(bclass.getCustomMapping())) {
            return false;
        }

        if (!getDefaultViewSheet().equals(bclass.getDefaultViewSheet())) {
            return false;
        }

        if (!getDefaultEditSheet().equals(bclass.getDefaultEditSheet())) {
            return false;
        }

        if (!getDefaultWeb().equals(bclass.getDefaultWeb())) {
            return false;
        }

        if (!getValidationScript().equals(bclass.getValidationScript())) {
            return false;
        }

        if (!getNameField().equals(bclass.getNameField())) {
            return false;
        }

        return true;
    }

    public void merge(BaseClass bclass)
    {
    }

    @Override
    public Element toXML(BaseClass bclass)
    {
        return toXML();
    }

    public Element toXML()
    {
        Element cel = new DOMElement("class");

        Element el = new DOMElement("name");
        el.addText((getName() == null) ? "" : getName());
        cel.add(el);

        el = new DOMElement("customClass");
        el.addText((getCustomClass() == null) ? "" : getCustomClass());
        cel.add(el);

        el = new DOMElement("customMapping");
        el.addText((getCustomMapping() == null) ? "" : getCustomMapping());
        cel.add(el);

        el = new DOMElement("defaultViewSheet");
        el.addText((getDefaultViewSheet() == null) ? "" : getDefaultViewSheet());
        cel.add(el);

        el = new DOMElement("defaultEditSheet");
        el.addText((getDefaultEditSheet() == null) ? "" : getDefaultEditSheet());
        cel.add(el);

        el = new DOMElement("defaultWeb");
        el.addText((getDefaultWeb() == null) ? "" : getDefaultWeb());
        cel.add(el);

        el = new DOMElement("nameField");
        el.addText((getNameField() == null) ? "" : getNameField());
        cel.add(el);

        el = new DOMElement("validationScript");
        el.addText((getValidationScript() == null) ? "" : getValidationScript());
        cel.add(el);

        // Iterate over values sorted by field name so that the values are
        // exported to XML in a consistent order.
        Iterator it = getSortedIterator();
        while (it.hasNext()) {
            PropertyClass bprop = (PropertyClass) it.next();
            cel.add(bprop.toXML());
        }
        return cel;
    }

    public void fromXML(Element cel) throws XWikiException
    {
        try {
            int j = 1;
            setName(cel.element("name").getText());
            Element cclel = cel.element("customClass");
            if (cclel != null) {
                setCustomClass(cclel.getText());
                j++;
            }
            Element cmapel = cel.element("customMapping");
            if (cmapel != null) {
                setCustomMapping(cmapel.getText());
                j++;
            }
            Element cdvsel = cel.element("defaultViewSheet");
            if (cdvsel != null) {
                setDefaultViewSheet(cdvsel.getText());
                j++;
            }
            Element cdesel = cel.element("defaultEditSheet");
            if (cdesel != null) {
                setDefaultViewSheet(cdesel.getText());
                j++;
            }
            Element cdwel = cel.element("defaultWeb");
            if (cdwel != null) {
                setDefaultWeb(cdwel.getText());
                j++;
            }
            Element cnfel = cel.element("nameField");
            if (cnfel != null) {
                setNameField(cnfel.getText());
                j++;
            }

            Element valel = cel.element("validationScript");
            if (valel != null) {
                setValidationScript(valel.getText());
                j++;
            }

            @SuppressWarnings("unchecked")
            List<Element> list = cel.elements();
            for (int i = j; i < list.size(); i++) {
                Element pcel = list.get(i);
                String name = pcel.getName();
                String classType = pcel.element("classType").getText();
                PropertyClassProvider provider = null;
                try {
                    // First try to use the specified class type as hint.
                    provider = Utils.getComponent(PropertyClassProvider.class, classType);
                } catch (Exception e) {
                    // In previous versions the class type was the full Java class name of the property class
                    // implementation. Extract the hint by removing the Java package prefix and the Class suffix.
                    classType = StringUtils.removeEnd(StringUtils.substringAfterLast(classType, "."), "Class");
                    provider = Utils.getComponent(PropertyClassProvider.class, classType);
                }
                // We should use PropertyClassInterface (instead of PropertyClass, its default implementation) but it
                // doesn't have the fromXML method and adding it breaks the backwards compatibility. We make the
                // assumption that all property classes extend PropertyClass.
                PropertyClass property = (PropertyClass) provider.getInstance();
                property.setName(name);
                property.setObject(this);
                property.fromXML(pcel);
                safeput(name, property);
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_CLASSES,
                XWikiException.ERROR_XWIKI_CLASSES_PROPERTY_CLASS_INSTANCIATION, "Error instanciating property class",
                e, null);
        }
    }

    public void fromXML(String xml) throws XWikiException
    {
        SAXReader reader = new SAXReader();
        Document domdoc;

        if ((xml == null) || (xml.trim().equals(""))) {
            return;
        }

        xml = xml.replaceAll("<>", "<unknown>");
        xml = xml.replaceAll("</>", "</unknown>");

        try {
            StringReader in = new StringReader(xml);
            domdoc = reader.read(in);
        } catch (DocumentException e) {
            Object[] args = {xml};
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_DOC_XML_PARSING,
                "Error parsing xml {0}", e, args);
        }

        Element docel = domdoc.getRootElement();
        if (docel != null) {
            fromXML(docel);
        }
    }

    public boolean addTextField(String fieldName, String fieldPrettyName, int size)
    {
        if (get(fieldName) == null) {
            StringClass text_class = new StringClass();
            text_class.setName(fieldName);
            text_class.setPrettyName(fieldPrettyName);
            text_class.setSize(size);
            text_class.setObject(this);
            put(fieldName, text_class);

            return true;
        }

        return false;
    }

    public boolean addPasswordField(String fieldName, String fieldPrettyName, int size)
    {
        if (get(fieldName) == null) {
            PasswordClass text_class = new PasswordClass();
            text_class.setName(fieldName);
            text_class.setPrettyName(fieldPrettyName);
            text_class.setSize(size);
            text_class.setObject(this);
            put(fieldName, text_class);

            return true;
        }

        return false;
    }

    public boolean addEmailField(String fieldName, String fieldPrettyName, int size)
    {
        if (get(fieldName) == null) {
            EmailClass emailClass = new EmailClass();
            emailClass.setName(fieldName);
            emailClass.setPrettyName(fieldPrettyName);
            emailClass.setSize(size);
            emailClass.setObject(this);
            put(fieldName, emailClass);

            return true;
        }

        return false;
    }

    public boolean addBooleanField(String fieldName, String fieldPrettyName, String displayType)
    {
        if (get(fieldName) == null) {
            BooleanClass boolean_class = new BooleanClass();
            boolean_class.setName(fieldName);
            boolean_class.setPrettyName(fieldPrettyName);
            boolean_class.setDisplayType(displayType);
            boolean_class.setObject(this);
            put(fieldName, boolean_class);

            return true;
        }

        return false;
    }

    public boolean addUsersField(String fieldName, String fieldPrettyName)
    {
        return addUsersField(fieldName, fieldPrettyName, true);
    }

    /**
     * @since XWiki Core 1.1.2, XWiki Core 1.2M2
     */
    public boolean addUsersField(String fieldName, String fieldPrettyName, boolean multiSelect)
    {
        return addUsersField(fieldName, fieldPrettyName, 5, multiSelect);
    }

    public boolean addUsersField(String fieldName, String fieldPrettyName, int size)
    {
        return addUsersField(fieldName, fieldPrettyName, size, true);
    }

    /**
     * @since XWiki Core 1.1.2, XWiki Core 1.2M2
     */
    public boolean addUsersField(String fieldName, String fieldPrettyName, int size, boolean multiSelect)
    {
        if (get(fieldName) == null) {
            UsersClass users_class = new UsersClass();
            users_class.setName(fieldName);
            users_class.setPrettyName(fieldPrettyName);
            users_class.setSize(size);
            users_class.setMultiSelect(multiSelect);
            users_class.setObject(this);
            put(fieldName, users_class);

            return true;
        }

        return false;
    }

    public boolean addLevelsField(String fieldName, String fieldPrettyName)
    {
        return addLevelsField(fieldName, fieldPrettyName, 3);
    }

    public boolean addLevelsField(String fieldName, String fieldPrettyName, int size)
    {
        if (get(fieldName) == null) {
            LevelsClass levels_class = new LevelsClass();
            levels_class.setName(fieldName);
            levels_class.setPrettyName(fieldPrettyName);
            levels_class.setSize(size);
            levels_class.setMultiSelect(true);
            levels_class.setObject(this);
            put(fieldName, levels_class);

            return true;
        }

        return false;
    }

    public boolean addGroupsField(String fieldName, String fieldPrettyName)
    {
        return addGroupsField(fieldName, fieldPrettyName, 5);
    }

    public boolean addGroupsField(String fieldName, String fieldPrettyName, int size)
    {
        if (get(fieldName) == null) {
            GroupsClass groups_class = new GroupsClass();
            groups_class.setName(fieldName);
            groups_class.setPrettyName(fieldPrettyName);
            groups_class.setSize(size);
            groups_class.setMultiSelect(true);
            groups_class.setObject(this);
            put(fieldName, groups_class);

            return true;
        }

        return false;
    }

    public boolean addTemplateField(String fieldName, String fieldPrettyName)
    {
        return addTextAreaField(fieldName, fieldPrettyName, 80, 15);
    }

    public boolean addTextAreaField(String fieldName, String fieldPrettyName, int cols, int rows)
    {
        if (get(fieldName) == null) {
            TextAreaClass template_class = new TextAreaClass();
            template_class.setName(fieldName);
            template_class.setPrettyName(fieldPrettyName);
            template_class.setSize(cols);
            template_class.setRows(rows);
            template_class.setObject(this);
            put(fieldName, template_class);

            return true;
        }

        return false;
    }

    public boolean addStaticListField(String fieldName, String fieldPrettyName, String values)
    {
        return addStaticListField(fieldName, fieldPrettyName, 1, false, values);
    }

    public boolean addStaticListField(String fieldName, String fieldPrettyName, int size, boolean multiSelect,
        String values)
    {
        return addStaticListField(fieldName, fieldPrettyName, size, multiSelect, values, null);
    }

    public boolean addStaticListField(String fieldName, String fieldPrettyName, int size, boolean multiSelect,
        String values, String displayType)
    {
        return addStaticListField(fieldName, fieldPrettyName, size, multiSelect, values, displayType, null);
    }

    /**
     * @since XWiki Core 1.1.2, XWiki Core 1.2M2
     */
    public boolean addStaticListField(String fieldName, String fieldPrettyName, int size, boolean multiSelect,
        String values, String displayType, String separators)
    {
        return addStaticListField(fieldName, fieldPrettyName, size, multiSelect, false, values, displayType, separators);
    }

    /**
     * @since 1.8M1
     */
    public boolean addStaticListField(String fieldName, String fieldPrettyName, int size, boolean multiSelect,
        boolean relationalStorage, String values, String displayType, String separators)
    {
        if (get(fieldName) == null) {
            StaticListClass list_class = new StaticListClass();
            list_class.setName(fieldName);
            list_class.setPrettyName(fieldPrettyName);
            list_class.setSize(size);
            list_class.setMultiSelect(multiSelect);
            list_class.setRelationalStorage(relationalStorage);
            list_class.setValues(values);
            if (displayType != null) {
                list_class.setDisplayType(displayType);
            }
            if (separators != null) {
                list_class.setSeparators(separators);
                list_class.setSeparator(separators.substring(0, 1));
            }
            list_class.setObject(this);
            put(fieldName, list_class);

            return true;
        }

        return false;
    }

    public boolean addNumberField(String fieldName, String fieldPrettyName, int size, String type)
    {
        if (get(fieldName) == null) {
            NumberClass number_class = new NumberClass();
            number_class.setName(fieldName);
            number_class.setPrettyName(fieldPrettyName);
            number_class.setSize(size);
            number_class.setNumberType(type);
            number_class.setObject(this);
            put(fieldName, number_class);

            return true;
        }

        return false;
    }

    public boolean addDateField(String fieldName, String fieldPrettyName)
    {
        return addDateField(fieldName, fieldPrettyName, null, 1);
    }

    public boolean addDateField(String fieldName, String fieldPrettyName, String dformat)
    {
        return addDateField(fieldName, fieldPrettyName, dformat, 1);
    }

    public boolean addDateField(String fieldName, String fieldPrettyName, String dformat, int emptyIsToday)
    {
        if (get(fieldName) == null) {
            DateClass date_class = new DateClass();
            date_class.setName(fieldName);
            date_class.setPrettyName(fieldPrettyName);
            if (dformat != null) {
                date_class.setDateFormat(dformat);
            }
            date_class.setObject(this);
            date_class.setEmptyIsToday(emptyIsToday);
            put(fieldName, date_class);

            return true;
        }

        return false;
    }

    public boolean addDBListField(String fieldName, String fieldPrettyName, String sql)
    {
        return addDBListField(fieldName, fieldPrettyName, 1, false, sql);
    }

    public boolean addDBListField(String fieldName, String fieldPrettyName, int size, boolean multiSelect, String sql)
    {
        return addDBListField(fieldName, fieldPrettyName, size, multiSelect, multiSelect, sql);
    }

    /**
     * @since 1.8M1
     */
    public boolean addDBListField(String fieldName, String fieldPrettyName, int size, boolean multiSelect,
        boolean relationalStorage, String sql)
    {
        if (get(fieldName) == null) {
            DBListClass list_class = new DBListClass();
            list_class.setName(fieldName);
            list_class.setPrettyName(fieldPrettyName);
            list_class.setSize(size);
            list_class.setMultiSelect(multiSelect);
            list_class.setRelationalStorage(relationalStorage);
            list_class.setSql(sql);
            list_class.setObject(this);
            put(fieldName, list_class);

            return true;
        }

        return false;
    }

    public boolean addDBTreeListField(String fieldName, String fieldPrettyName, String sql)
    {
        return addDBTreeListField(fieldName, fieldPrettyName, 1, false, sql);
    }

    public boolean addDBTreeListField(String fieldName, String fieldPrettyName, int size, boolean multiSelect,
        String sql)
    {
        return addDBTreeListField(fieldName, fieldPrettyName, size, multiSelect, multiSelect, sql);
    }

    /**
     * @since 1.8M1
     */
    public boolean addDBTreeListField(String fieldName, String fieldPrettyName, int size, boolean multiSelect,
        boolean relationalStorage, String sql)
    {
        if (get(fieldName) == null) {
            DBTreeListClass list_class = new DBTreeListClass();
            list_class.setName(fieldName);
            list_class.setPrettyName(fieldPrettyName);
            list_class.setSize(size);
            list_class.setMultiSelect(multiSelect);
            list_class.setRelationalStorage(relationalStorage);
            list_class.setSql(sql);
            list_class.setObject(this);
            put(fieldName, list_class);

            return true;
        }

        return false;
    }

    public void setCustomMapping(String customMapping)
    {
        this.customMapping = customMapping;
    }

    public String getCustomMapping()
    {
        if ("XWiki.XWikiPreferences".equals(getName())) {
            return "internal";
        }

        if (this.customMapping == null) {
            return "";
        }

        return this.customMapping;
    }

    public boolean hasCustomMapping()
    {
        String cMapping = getCustomMapping();

        return (cMapping != null) && (!"".equals(cMapping));
    }

    public boolean hasExternalCustomMapping()
    {
        String cMapping = getCustomMapping();

        return (cMapping != null) && (!"".equals(cMapping)) && (!"internal".equals(cMapping));
    }

    public boolean hasInternalCustomMapping()
    {
        return "internal".equals(this.customMapping);
    }

    public boolean isCustomMappingValid(XWikiContext context) throws XWikiException
    {
        return isCustomMappingValid(getCustomMapping(), context);
    }

    public boolean isCustomMappingValid(String custommapping, XWikiContext context) throws XWikiException
    {
        if ((custommapping != null) && (custommapping.trim().length() > 0)) {
            return context.getWiki().getStore().isCustomMappingValid(this, custommapping, context);
        } else {
            return true;
        }
    }

    public List<String> getCustomMappingPropertyList(XWikiContext context)
    {
        String custommapping1 = getCustomMapping();
        if ((custommapping1 != null) && (custommapping1.trim().length() > 0)) {
            return context.getWiki().getStore().getCustomMappingPropertyList(this);
        } else {
            return new ArrayList<String>();
        }
    }

    public void setCustomClass(String customClass)
    {
        this.customClass = customClass;
    }

    public String getCustomClass()
    {
        if (this.customClass == null) {
            return "";
        }

        return this.customClass;
    }

    public BaseObject newCustomClassInstance(XWikiContext context) throws XWikiException
    {
        String customClass = getCustomClass();
        try {
            if ((customClass == null) || (customClass.equals(""))) {
                return new BaseObject();
            } else {
                return (BaseObject) Class.forName(getCustomClass()).newInstance();
            }
        } catch (Exception e) {
            Object[] args = {customClass};
            throw new XWikiException(XWikiException.MODULE_XWIKI_CLASSES,
                XWikiException.ERROR_XWIKI_CLASSES_CUSTOMCLASSINVOCATIONERROR, "Cannot instanciate custom class {0}",
                e, args);
        }
    }

    /**
     * @since 2.2.3
     */
    public static BaseObject newCustomClassInstance(DocumentReference classReference, XWikiContext context)
        throws XWikiException
    {
        BaseClass bclass = context.getWiki().getXClass(classReference, context);
        BaseObject object = (bclass == null) ? new BaseObject() : bclass.newCustomClassInstance(context);

        return object;
    }

    /**
     * @deprecated since 2.2.3 use {@link #newCustomClassInstance(DocumentReference, XWikiContext)}
     */
    @Deprecated
    public static BaseObject newCustomClassInstance(String className, XWikiContext context) throws XWikiException
    {
        BaseClass bclass = context.getWiki().getClass(className, context);
        BaseObject object = (bclass == null) ? new BaseObject() : bclass.newCustomClassInstance(context);

        return object;
    }

    public String getDefaultWeb()
    {
        if (this.defaultWeb == null) {
            return "";
        }

        return this.defaultWeb;
    }

    public void setDefaultWeb(String defaultWeb)
    {
        this.defaultWeb = defaultWeb;
    }

    public String getDefaultViewSheet()
    {
        if (this.defaultViewSheet == null) {
            return "";
        }

        return this.defaultViewSheet;
    }

    public void setDefaultViewSheet(String defaultViewSheet)
    {
        this.defaultViewSheet = defaultViewSheet;
    }

    public String getDefaultEditSheet()
    {
        if (this.defaultEditSheet == null) {
            return "";
        }

        return this.defaultEditSheet;
    }

    public void setDefaultEditSheet(String defaultEditSheet)
    {
        this.defaultEditSheet = defaultEditSheet;
    }

    public String getNameField()
    {
        if (this.nameField == null) {
            return "";
        }

        return this.nameField;
    }

    public void setNameField(String nameField)
    {
        this.nameField = nameField;
    }

    public void setValidationScript(String validationScript)
    {
        this.validationScript = validationScript;
    }

    public String getValidationScript()
    {
        if (this.validationScript == null) {
            return "";
        } else {
            return this.validationScript;
        }
    }

    public boolean validateObject(BaseObject obj, XWikiContext context) throws XWikiException
    {
        boolean isValid = true;
        Object[] props = getPropertyNames();
        for (int i = 0; i < props.length; i++) {
            String propname = (String) props[i];
            BaseProperty property = (BaseProperty) obj.get(propname);
            PropertyClass propclass = (PropertyClass) get(propname);
            isValid &= propclass.validateProperty(property, context);
        }

        String validSript = getValidationScript();
        if ((validSript != null) && (!validSript.trim().equals(""))) {
            isValid &= executeValidationScript(obj, validSript, context);
        }

        return isValid;
    }

    private boolean executeValidationScript(BaseObject obj, String validationScript, XWikiContext context)
    {
        try {
            XWikiValidationInterface validObject =
                (XWikiValidationInterface) context.getWiki().parseGroovyFromPage(validationScript, context);

            return validObject.validateObject(obj, context);
        } catch (Throwable e) {
            XWikiValidationStatus.addExceptionToContext(getName(), "", e, context);
            return false;
        }
    }

    public void flushCache()
    {
        Object[] props = getPropertyNames();
        for (int i = 0; i < props.length; i++) {
            String propname = (String) props[i];
            PropertyClass propclass = (PropertyClass) get(propname);
            if (propclass != null) {
                propclass.flushCache();
            }
        }
    }

    @Override
    public List<ObjectDiff> getDiff(Object oldObject, XWikiContext context)
    {
        ArrayList<ObjectDiff> difflist = new ArrayList<ObjectDiff>();
        BaseClass oldClass = (BaseClass) oldObject;
        for (PropertyClass newProperty : (Collection<PropertyClass>) getFieldList()) {
            String propertyName = newProperty.getName();
            PropertyClass oldProperty = (PropertyClass) oldClass.get(propertyName);
            String propertyType = newProperty.getClassType();

            if (oldProperty == null) {
                difflist.add(new ObjectDiff(getXClassReference(), getNumber(), "", ObjectDiff.ACTION_PROPERTYADDED,
                    propertyName, propertyType, "", ""));
            } else if (!oldProperty.equals(newProperty)) {
                difflist.add(new ObjectDiff(getXClassReference(), getNumber(), "", ObjectDiff.ACTION_PROPERTYCHANGED,
                    propertyName, propertyType, "", ""));
            }
        }

        for (PropertyClass oldProperty : (Collection<PropertyClass>) oldClass.getFieldList()) {
            String propertyName = oldProperty.getName();
            PropertyClass newProperty = (PropertyClass) get(propertyName);
            String propertyType = oldProperty.getClassType();

            if (newProperty == null) {
                difflist.add(new ObjectDiff(getXClassReference(), getNumber(), "", ObjectDiff.ACTION_PROPERTYREMOVED,
                    propertyName, propertyType, "", ""));
            }
        }

        return difflist;
    }

    @Override
    public void merge(ElementInterface previousElement, ElementInterface newElement, MergeConfiguration configuration,
        XWikiContext context, MergeResult mergeResult)
    {
        BaseClass previousClass = (BaseClass) previousElement;
        BaseClass newClass = (BaseClass) newElement;

        setCustomClass(MergeUtils.mergeOject(previousClass.getCustomClass(), newClass.getCustomClass(),
            getCustomClass(), mergeResult));

        setCustomMapping(MergeUtils.mergeOject(previousClass.getCustomMapping(), newClass.getCustomMapping(),
            getCustomMapping(), mergeResult));

        setDefaultWeb(MergeUtils.mergeOject(previousClass.getDefaultWeb(), newClass.getDefaultWeb(), getDefaultWeb(),
            mergeResult));

        setDefaultViewSheet(MergeUtils.mergeOject(previousClass.getDefaultViewSheet(), newClass.getDefaultViewSheet(),
            getDefaultViewSheet(), mergeResult));

        setDefaultEditSheet(MergeUtils.mergeOject(previousClass.getDefaultEditSheet(), newClass.getDefaultEditSheet(),
            getDefaultEditSheet(), mergeResult));

        setNameField(MergeUtils.mergeOject(previousClass.getNameField(), newClass.getNameField(), getNameField(),
            mergeResult));

        // Properties

        super.merge(previousElement, newElement, configuration, context, mergeResult);
    }

    @Override
    public boolean apply(ElementInterface newElement, boolean clean)
    {
        boolean modified = super.apply(newElement, clean);

        BaseClass newBaseClass = (BaseClass) newElement;

        if (!StringUtils.equals(getCustomClass(), newBaseClass.getCustomClass())) {
            setCustomClass(newBaseClass.getCustomClass());
            modified = true;
        }

        if (!StringUtils.equals(getCustomMapping(), newBaseClass.getCustomMapping())) {
            setCustomMapping(newBaseClass.getCustomMapping());
            modified = true;
        }

        if (!StringUtils.equals(getDefaultWeb(), newBaseClass.getDefaultWeb())) {
            setDefaultWeb(newBaseClass.getDefaultWeb());
            modified = true;
        }

        if (!StringUtils.equals(getDefaultViewSheet(), newBaseClass.getDefaultViewSheet())) {
            setDefaultViewSheet(newBaseClass.getDefaultViewSheet());
            modified = true;
        }

        if (!StringUtils.equals(getDefaultEditSheet(), newBaseClass.getDefaultEditSheet())) {
            setDefaultEditSheet(newBaseClass.getDefaultEditSheet());
            modified = true;
        }

        if (!StringUtils.equals(getNameField(), newBaseClass.getNameField())) {
            setNameField(newBaseClass.getNameField());
            modified = true;
        }

        return modified;
    }

    /**
     * Set the owner document of this base property.
     *
     * @param ownerDocument The owner document.
     * @since 4.3M2
     */
    public void setOwnerDocument(XWikiDocument ownerDocument)
    {
        super.setOwnerDocument(ownerDocument);

        if (this.ownerDocument != null) {
            setDocumentReference(this.ownerDocument.getDocumentReference());
        }

        if (ownerDocument != null && isDirty) {
            ownerDocument.setContentDirty(true);
        }
    }

    /**
     * @param isDirty Indicate if the dirty flag should be set or cleared.
     * @since 4.3M2
     */
    public void setDirty(boolean isDirty)
    {
        this.isDirty = isDirty;
        if (isDirty && ownerDocument != null) {
            ownerDocument.setContentDirty(true);
        }
    }
}
