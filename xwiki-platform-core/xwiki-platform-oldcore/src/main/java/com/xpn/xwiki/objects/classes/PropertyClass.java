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

import java.util.Iterator;

import javax.script.ScriptContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.ecs.xhtml.input;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.hibernate.mapping.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.internal.xml.XMLAttributeValueFilter;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.validation.XWikiValidationStatus;
import com.xpn.xwiki.web.Utils;

/**
 * Represents an XClass property and contains property definitions (eg "relational storage", "display type",
 * "separator", "multi select", etc). Each property definition is of type {@link BaseProperty}.
 *
 * @version $Id$
 */
public class PropertyClass extends BaseCollection<ClassPropertyReference>
    implements PropertyClassInterface, Comparable<PropertyClass>
{
    private static final long serialVersionUID = 1L;

    /**
     * Logging helper object.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyClass.class);

    /**
     * Identifier used to specify that the property has a custom displayer in the XClass itself.
     */
    private static final String CLASS_DISPLAYER_IDENTIFIER = "class";

    /**
     * Identifier prefix used to specify that the property has a custom displayer in a wiki document.
     */
    private static final String DOCUMENT_DISPLAYER_IDENTIFIER_PREFIX = "doc:";

    /**
     * Identifier prefix used to specify that the property has a custom displayer in a velocity template.
     */
    private static final String TEMPLATE_DISPLAYER_IDENTIFIER_PREFIX = "template:";

    private BaseClass xclass;

    private long id;

    private PropertyMetaClass pMetaClass;

    protected String cachedCustomDisplayer;

    public PropertyClass()
    {
    }

    public PropertyClass(String name, String prettyname, PropertyMetaClass xWikiClass)
    {
        setName(name);
        setPrettyName(prettyname);
        setxWikiClass(xWikiClass);
        setUnmodifiable(false);
        setDisabled(false);
    }

    @Override
    protected ClassPropertyReference createReference()
    {
        return new ClassPropertyReference(getName(), this.xclass.getReference());
    }

    @Override
    public BaseClass getXClass(XWikiContext context)
    {
        return getxWikiClass();
    }

    public BaseClass getxWikiClass()
    {
        if (this.pMetaClass == null) {
            MetaClass metaClass = MetaClass.getMetaClass();
            this.pMetaClass = (PropertyMetaClass) metaClass.get(getClassType());
        }
        return this.pMetaClass;
    }

    public void setxWikiClass(BaseClass xWikiClass)
    {
        this.pMetaClass = (PropertyMetaClass) xWikiClass;
    }

    @Override
    public BaseCollection getObject()
    {
        return this.xclass;
    }

    @Override
    public void setObject(BaseCollection object)
    {
        this.xclass = (BaseClass) object;
    }

    /**
     * Computes the field full name using this format: {@code parentName_fieldName}. There are 2 main cases:
     * <ul>
     * <li>this is a class property: the prefix is the class reference (e.g. XWiki.TagClass_tags)</li>
     * <li>this is a meta property: the prefix is the property type (e.g. TextArea_editor)</li>
     * </ul>
     * 
     * @return the field full name
     */
    public String getFieldFullName()
    {
        String prefix = getObject() != null ? getObject().getName() : getClassName();
        return prefix + "_" + getName();
    }

    @Override
    public long getId()
    {
        if (getObject() == null) {
            return this.id;
        }
        return getObject().getId();
    }

    @Override
    public void setId(long id)
    {
        this.id = id;
    }

    @Override
    public String toString(BaseProperty property)
    {
        return property.toText();
    }

    @Override
    public BaseProperty fromString(String value)
    {
        return null;
    }

    public BaseProperty newPropertyfromXML(Element ppcel)
    {
        String value = ppcel.getText();
        return fromString(value);
    }

    @Override
    public void displayHidden(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        input input = new input();
        input.setAttributeFilter(new XMLAttributeValueFilter());
        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop != null) {
            input.setValue(prop.toText());
        }

        input.setType("hidden");
        input.setName(prefix + name);
        input.setID(prefix + name);
        buffer.append(input.toString());
    }

    @Override
    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop != null) {
            buffer.append(prop.toText());
        }
    }

    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        input input = new input();
        input.setAttributeFilter(new XMLAttributeValueFilter());

        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop != null) {
            input.setValue(prop.toText());
        }

        input.setType("text");
        input.setName(prefix + name);
        input.setID(prefix + name);
        input.setDisabled(isDisabled());
        buffer.append(input.toString());
    }

    public String displayHidden(String name, String prefix, BaseCollection object, XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        displayHidden(buffer, name, prefix, object, context);
        return buffer.toString();
    }

    public String displayHidden(String name, BaseCollection object, XWikiContext context)
    {
        return displayHidden(name, "", object, context);
    }

    public String displayView(String name, String prefix, BaseCollection object, XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        displayView(buffer, name, prefix, object, context);
        return buffer.toString();
    }

    public String displayView(String name, BaseCollection object, XWikiContext context)
    {
        return displayView(name, "", object, context);
    }

    public String displayEdit(String name, String prefix, BaseCollection object, XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        displayEdit(buffer, name, prefix, object, context);
        return buffer.toString();
    }

    public String displayEdit(String name, BaseCollection object, XWikiContext context)
    {
        return displayEdit(name, "", object, context);
    }

    public boolean isCustomDisplayed(XWikiContext context)
    {
        return (StringUtils.isNotEmpty(getCachedDefaultCustomDisplayer(context)));
    }

    public void displayCustom(StringBuffer buffer, String fieldName, String prefix, String type, BaseObject object,
        final XWikiContext context) throws XWikiException
    {
        String content = "";
        try {
            ScriptContext scontext = Utils.getComponent(ScriptContextManager.class).getCurrentScriptContext();
            scontext.setAttribute("name", fieldName, ScriptContext.ENGINE_SCOPE);
            scontext.setAttribute("prefix", prefix, ScriptContext.ENGINE_SCOPE);
            // The PropertyClass instance can be used to access meta properties in the custom displayer (e.g.
            // dateFormat, multiSelect). It can be obtained from the XClass of the given object but only if the property
            // has been added to the XClass. We need to have it in the Velocity context for the use case when an XClass
            // property needs to be previewed before being added to the XClass.
            scontext.setAttribute("field", new com.xpn.xwiki.api.PropertyClass(this, context),
                ScriptContext.ENGINE_SCOPE);
            scontext.setAttribute("object", new com.xpn.xwiki.api.Object(object, context), ScriptContext.ENGINE_SCOPE);
            scontext.setAttribute("type", type, ScriptContext.ENGINE_SCOPE);
            /* This is a text alternative fallback to explain what the input is about. 
             If the input has already been labelled in another way, this fallback will be ignored by Assistive Techs.
             */
            scontext.setAttribute("aria-label",
                localizePlainOrKey("core.model.xclass.editClassProperty.textAlternative",
                    this.getTranslatedPrettyName(context)), ScriptContext.ENGINE_SCOPE);

            BaseProperty prop = (BaseProperty) object.safeget(fieldName);
            if (prop != null) {
                scontext.setAttribute("value", prop.getValue(), ScriptContext.ENGINE_SCOPE);
            } else {
                // The $value property can exist in the velocity context, we overwrite it to make sure we don't get a
                // wrong value in the displayer when the property does not exist yet.
                scontext.setAttribute("value", null, ScriptContext.ENGINE_SCOPE);
            }

            String customDisplayer = getCachedDefaultCustomDisplayer(context);
            if (StringUtils.isNotEmpty(customDisplayer)) {
                if (customDisplayer.equals(CLASS_DISPLAYER_IDENTIFIER)) {
                    final String rawContent = getCustomDisplay();
                    XWikiDocument classDocument =
                        context.getWiki().getDocument(getObject().getDocumentReference(), context);
                    final String classSyntax = classDocument.getSyntax().toIdString();
                    // Using author reference since the document content is not relevant in this case.
                    DocumentReference authorReference = classDocument.getAuthorReference();
                    if (authorReference == null && classDocument.isNew()) {
                        // If the class document has not been saved yet (e.g. we could be previewing a class property in
                        // the class editor) then use the context user as author (e.g. the user that is in the process
                        // of creating the class).
                        authorReference = context.getUserReference();
                    }

                    // Make sure we render the custom displayer with the rights of the user who wrote it (i.e. class
                    // document author).
                    content = renderContentInContext(rawContent, classSyntax, authorReference,
                        classDocument.getDocumentReference(), context);
                } else if (customDisplayer.startsWith(DOCUMENT_DISPLAYER_IDENTIFIER_PREFIX)) {
                    XWikiDocument displayerDoc = context.getWiki().getDocument(
                        StringUtils.substringAfter(customDisplayer, DOCUMENT_DISPLAYER_IDENTIFIER_PREFIX), context);
                    final String rawContent = displayerDoc.getContent();
                    final String displayerDocSyntax = displayerDoc.getSyntax().toIdString();
                    DocumentReference authorReference = displayerDoc.getContentAuthorReference();

                    // Make sure we render the custom displayer with the rights of the user who wrote it (i.e. displayer
                    // document content author).
                    content = renderContentInContext(rawContent, displayerDocSyntax, authorReference,
                        displayerDoc.getDocumentReference(), context);
                } else if (customDisplayer.startsWith(TEMPLATE_DISPLAYER_IDENTIFIER_PREFIX)) {
                    content = context.getWiki().evaluateTemplate(
                        StringUtils.substringAfter(customDisplayer, TEMPLATE_DISPLAYER_IDENTIFIER_PREFIX), context);
                }
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_CLASSES,
                XWikiException.ERROR_XWIKI_CLASSES_CANNOT_PREPARE_CUSTOM_DISPLAY,
                "Exception while preparing the custom display of " + fieldName, e, null);

        }
        buffer.append(content);
    }

    /**
     * Render content in the current document's context with the rights of the given user.
     * 
     * @since 8.3M2
     * @deprecated since 10.11RC1, use
     *             {@link #renderContentInContext(String, String, DocumentReference, DocumentReference, XWikiContext)}
     *             instead
     */
    @Deprecated
    protected String renderContentInContext(final String content, final String syntax,
        DocumentReference authorReference, final XWikiContext context) throws Exception
    {
        return renderContentInContext(content, syntax, authorReference, null, context);
    }

    /**
     * Render content in the current document's context with the rights of the given user.
     * 
     * @since 10.11RC1
     */
    protected String renderContentInContext(final String content, final String syntax,
        DocumentReference authorReference, DocumentReference secureDocument, final XWikiContext context)
        throws Exception
    {
        return Utils.getComponent(AuthorExecutor.class)
            .call(() -> context.getDoc().getRenderedContent(content, syntax, context), authorReference, secureDocument);
    }

    @Override
    public String getClassName()
    {
        BaseClass bclass = getxWikiClass();
        return (bclass == null) ? "" : bclass.getName();
    }

    // In property classes we need to store this info in the HashMap for fields
    // This way it is readable by the displayEdit/displayView functions..
    @Override
    public String getName()
    {
        return getStringValue("name");
    }

    @Override
    public void setName(String name)
    {
        setStringValue("name", name);
    }

    public String getCustomDisplay()
    {
        return getStringValue("customDisplay");
    }

    public void setCustomDisplay(String value)
    {
        setLargeStringValue("customDisplay", value);
    }

    @Override
    public String getPrettyName()
    {
        return getStringValue("prettyName");
    }

    public String getPrettyName(XWikiContext context)
    {
        return getTranslatedPrettyName(context);
    }

    public String getTranslatedPrettyName(XWikiContext context)
    {
        String msgName = getFieldFullName();
        if ((context == null) || (context.getWiki() == null)) {
            return getPrettyName();
        }

        String prettyName = localizePlain(msgName);
        if (prettyName == null) {
            return getPrettyName();
        }
        return prettyName;
    }

    @Override
    public void setPrettyName(String prettyName)
    {
        setStringValue("prettyName", prettyName);
    }

    /**
     * @param property name of the property
     * @return the localized value of the property, with a fallback to the inner value
     */
    private String getLocalizedPropertyValue(String property)
    {
        String propertyName = String.format("%s_%s", getFieldFullName(), property);
        String propertyValue = localizePlain(propertyName);
        if (propertyValue == null) {
            propertyName = getLargeStringValue(property);
            if (StringUtils.isNotBlank(propertyName)) {
                propertyValue = localizePlainOrKey(propertyName, propertyName);
            }
        }
        return propertyValue;
    }

    /**
     * Get the localized hint. A hint is a text displayed in the object editor to help the user filling some content.
     *
     * @return the localized hint.
     * @since 9.11RC1
     */
    public String getHint()
    {
        return getLocalizedPropertyValue("hint");
    }

    /**
     * Set the text displayed in the object editor to help the user filling some content.
     * 
     * @since 9.11RC1
     */
    public void setHint(String hint)
    {
        setLargeStringValue("hint", hint);
    }

    public String getTooltip()
    {
        return getLargeStringValue("tooltip");
    }

    /**
     * Gets international tooltip
     *
     * @param context
     * @return
     */
    public String getTooltip(XWikiContext context)
    {
        return getLocalizedPropertyValue("tooltip");
    }

    public void setTooltip(String tooltip)
    {
        setLargeStringValue("tooltip", tooltip);
    }

    @Override
    public int getNumber()
    {
        return getIntValue("number");
    }

    @Override
    public void setNumber(int number)
    {
        setIntValue("number", number);
    }

    /**
     * Each type of XClass property is identified by a string that specifies the data type of the property value (e.g.
     * 'String', 'Number', 'Date') without disclosing implementation details. The internal implementation of an XClass
     * property type can change over time but its {@code classType} should not.
     * <p>
     * The {@code classType} can be used as a hint to lookup various components related to this specific XClass property
     * type. See {@link com.xpn.xwiki.internal.objects.classes.PropertyClassProvider} for instance.
     *
     * @return an identifier for the data type of the property value (e.g. 'String', 'Number', 'Date')
     */
    public String getClassType()
    {
        // By default the hint is computed by removing the Class suffix, if present, from the Java simple class name
        // (without the package). Subclasses can overwrite this method to use a different hint format.
        return StringUtils.removeEnd(getClass().getSimpleName(), "Class");
    }

    /**
     * Sets the property class type.
     *
     * @param type the class type
     * @deprecated since 4.3M1, the property class type cannot be modified
     */
    @Deprecated
    public void setClassType(String type)
    {
        LOGGER.warn("The property class type cannot be modified!");
    }

    @Override
    public PropertyClass clone()
    {
        PropertyClass pclass = (PropertyClass) super.clone();
        pclass.setObject(getObject());
        return pclass;
    }

    @Override
    public Element toXML(BaseClass bclass)
    {
        return toXML();
    }

    @Override
    public Element toXML()
    {
        Element pel = new DOMElement(getName());

        // Iterate over values sorted by field name so that the values are
        // exported to XML in a consistent order.
        Iterator it = getSortedIterator();
        while (it.hasNext()) {
            BaseProperty bprop = (BaseProperty) it.next();
            pel.add(bprop.toXML());
        }
        Element el = new DOMElement("classType");
        String classType = getClassType();
        if (this.getClass().getSimpleName().equals(classType + "Class")) {
            // Keep exporting the full Java class name for old/default property types to avoid breaking the XAR format
            // (to allow XClasses created with the current version of XWiki to be imported in an older version).
            classType = this.getClass().getName();
        }
        el.addText(classType);
        pel.add(el);
        return pel;
    }

    @Override
    public void fromXML(Element element) throws XWikiException
    {
        super.fromXML(element);
    }

    @Override
    public String toFormString()
    {
        return toString();
    }

    public void initLazyCollections()
    {
    }

    public boolean isUnmodifiable()
    {
        return (getIntValue("unmodifiable") == 1);
    }

    public void setUnmodifiable(boolean unmodifiable)
    {
        if (unmodifiable) {
            setIntValue("unmodifiable", 1);
        } else {
            setIntValue("unmodifiable", 0);
        }
    }

    /**
     * See if this property is disabled or not. A disabled property should not be editable, but existing object values
     * are still kept in the database.
     *
     * @return {@code true} if this property is disabled and should not be used, {@code false} otherwise
     * @see #setDisabled(boolean)
     * @since 2.4M2
     */
    public boolean isDisabled()
    {
        return (getIntValue("disabled", 0) == 1);
    }

    /**
     * Disable or re-enable this property. A disabled property should not be editable, but existing object values are
     * still kept in the database.
     *
     * @param disabled whether the property is disabled or not
     * @see #isDisabled()
     * @since 2.4M2
     */
    public void setDisabled(boolean disabled)
    {
        if (disabled) {
            setIntValue("disabled", 1);
        } else {
            setIntValue("disabled", 0);
        }
    }

    public BaseProperty fromStringArray(String[] strings)
    {
        return fromString(strings[0]);
    }

    public boolean isValidColumnTypes(Property hibprop)
    {
        return true;
    }

    @Override
    public BaseProperty fromValue(Object value)
    {
        BaseProperty property = newProperty();
        if (property != null) {
            property.setValue(value);
            return property;
        }

        return null;
    }

    @Override
    public BaseProperty newProperty()
    {
        return new BaseProperty();
    }

    public void setValidationRegExp(String validationRegExp)
    {
        setStringValue("validationRegExp", validationRegExp);
    }

    public String getValidationRegExp()
    {
        return getStringValue("validationRegExp");
    }

    public String getValidationMessage()
    {
        return getStringValue("validationMessage");
    }

    public void setValidationMessage(String validationMessage)
    {
        setStringValue("validationMessage", validationMessage);
    }

    public boolean validateProperty(BaseProperty property, XWikiContext context)
    {
        String regexp = getValidationRegExp();
        if ((regexp == null) || (regexp.trim().equals(""))) {
            return true;
        }

        String value = ((property == null) || (property.getValue() == null)) ? "" : property.getValue().toString();
        try {
            if (context.getUtil().match(regexp, value)) {
                return true;
            }
            XWikiValidationStatus.addErrorToContext((getObject() == null) ? "" : getObject().getName(), getName(),
                getTranslatedPrettyName(context), getValidationMessage(), context);

            return false;
        } catch (Exception e) {
            XWikiValidationStatus.addExceptionToContext((getObject() == null) ? "" : getObject().getName(), getName(),
                e, context);

            return false;
        }
    }

    @Override
    public void flushCache()
    {
        this.cachedCustomDisplayer = null;
    }

    /**
     * Compares two property definitions based on their index number.
     *
     * @param other the other property definition to be compared with
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than
     *         the specified object.
     * @see #getNumber()
     * @since 2.4M2
     */
    @Override
    public int compareTo(PropertyClass other)
    {
        int result = this.getNumber() - other.getNumber();

        // This should never happen, but just to remove the randomness in case it does happen, also compare their names.
        if (result == 0) {
            result = this.getName().compareTo(other.getName());
        }

        return result;
    }

    protected String getFullQueryPropertyName()
    {
        return "obj." + getName();
    }

    /**
     * Returns the current cached default custom displayer for the PropertyClass. The result will be cached and can be
     * flushed using {@link #flushCache()}. If it returns the empty string, then there is no default custom displayer
     * for this class.
     *
     * @param context the current request context
     * @return An identifier for the location of a custom displayer. This can be {@code class} if there's custom display
     *         code specified in the class itself, {@code page:currentwiki:XWiki.BooleanDisplayer} if such a document
     *         exists in the current wiki, {@code page:xwiki:XWiki.StringDisplayer} if such a document exists in the
     *         main wiki, or {@code template:displayer_boolean.vm} if a template on the filesystem or in the current
     *         skin exists.
     */
    protected String getCachedDefaultCustomDisplayer(XWikiContext context)
    {
        // First look at custom displayer in class. We should not cache this one.
        String customDisplay = getCustomDisplay();
        if (StringUtils.isNotEmpty(customDisplay)) {
            return CLASS_DISPLAYER_IDENTIFIER;
        }

        // Then look for pages or templates
        if (this.cachedCustomDisplayer == null) {
            this.cachedCustomDisplayer = getDefaultCustomDisplayer(getTypeName(), context);
        }
        return this.cachedCustomDisplayer;
    }

    /**
     * Method to find the default custom displayer to use for a specific Property Class.
     *
     * @param propertyClassName the type of the property; this is defined in each subclass, such as {@code boolean},
     *            {@code string} or {@code dblist}
     * @param context the current request context
     * @return An identifier for the location of a custom displayer. This can be {@code class} if there's custom display
     *         code specified in the class itself, {@code page:currentwiki:XWiki.BooleanDisplayer} if such a document
     *         exists in the current wiki, {@code page:xwiki:XWiki.StringDisplayer} if such a document exists in the
     *         main wiki, or {@code template:displayer_boolean.vm} if a template on the filesystem or in the current
     *         skin exists.
     */
    protected String getDefaultCustomDisplayer(String propertyClassName, XWikiContext context)
    {
        LOGGER.debug("Looking up default custom displayer for property class name [{}]", propertyClassName);

        try {
            // First look into the current wiki
            String pageName = StringUtils.capitalize(propertyClassName) + "Displayer";
            DocumentReference reference = new DocumentReference(context.getWikiId(), "XWiki", pageName);
            if (context.getWiki().exists(reference, context)) {
                LOGGER.debug("Found default custom displayer for property class name in local wiki: [{}]", pageName);
                return DOCUMENT_DISPLAYER_IDENTIFIER_PREFIX + "XWiki." + pageName;
            }

            // Look in the main wiki
            if (!context.isMainWiki()) {
                reference = new DocumentReference(context.getMainXWiki(), "XWiki", pageName);
                if (context.getWiki().exists(reference, context)) {
                    LOGGER.debug("Found default custom displayer for property class name in main wiki: [{}]", pageName);
                    return DOCUMENT_DISPLAYER_IDENTIFIER_PREFIX + context.getMainXWiki() + ":XWiki." + pageName;
                }
            }

            // Look in templates
            String templateName = "displayer_" + propertyClassName + ".vm";
            TemplateManager templateManager = Utils.getComponent(TemplateManager.class);
            Template existingTemplate = templateManager.getTemplate(templateName);
            if (existingTemplate != null) {
                LOGGER.debug("Found default custom displayer for property class name as template: [{}]", templateName);
                return TEMPLATE_DISPLAYER_IDENTIFIER_PREFIX + templateName;
            }
        } catch (Throwable e) {
            // If we fail we consider there is no custom displayer
            LOGGER.error("Error finding if property [{}] has a custom displayer. "
                + "Considering that there's no custom displayer.", propertyClassName, e);
        }

        return null;
    }

    /**
     * Get a short name identifying this type of property. This is derived from the java class name, lowercasing the
     * part before {@code Class}.
     *
     * @return a string, for example {@code string}, {@code dblist}, {@code number}
     */
    private String getTypeName()
    {
        return StringUtils.substringBeforeLast(this.getClass().getSimpleName(), "Class").toLowerCase();
    }

    /**
     * Apply a 3 ways merge on passed current, previous and new version of the same property. The passed current version
     * is modified as result of the merge.
     *
     * @param currentProperty the current version of the element and the one to modify
     * @param previousProperty the previous version of the element
     * @param newProperty the new version of the property
     * @param configuration the configuration of the merge Indicate how to deal with some conflicts use cases, etc.
     * @param context the XWiki context
     * @param mergeResult the merge report
     * @since 6.2M1
     */
    public <T extends EntityReference> void mergeProperty(BaseProperty<T> currentProperty,
        BaseProperty<T> previousProperty, BaseProperty<T> newProperty, MergeConfiguration configuration,
        XWikiContext context, MergeResult mergeResult)
    {
        currentProperty.merge(previousProperty, newProperty, configuration, context, mergeResult);
    }
}
