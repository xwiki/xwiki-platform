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
package org.xwiki.wikistream.instance.internal.output;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.filter.FilterDescriptorManager;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.properties.ConverterManager;
import org.xwiki.rendering.listener.WrappingListener;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.filter.xwiki.XWikiWikiAttachmentFilter;
import org.xwiki.wikistream.filter.xwiki.XWikiWikiDocumentFilter;
import org.xwiki.wikistream.instance.internal.XWikiDocumentFilter;
import org.xwiki.wikistream.instance.output.DocumentInstanceOutputProperties;
import org.xwiki.wikistream.model.filter.WikiAttachmentFilter;
import org.xwiki.wikistream.model.filter.WikiClassFilter;
import org.xwiki.wikistream.model.filter.WikiDocumentFilter;
import org.xwiki.wikistream.model.filter.WikiObjectFilter;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.objects.classes.PropertyClassProvider;
import com.xpn.xwiki.internal.objects.meta.PropertyMetaClassInterface;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.PropertyClassInterface;

/**
 * @version $Id$
 * @since 5.4M1
 */
@Component(roles = XWikiDocumentOutputWikiStream.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class XWikiDocumentOutputWikiStream implements XWikiDocumentFilter
{
    @Inject
    private FilterDescriptorManager filterManager;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> entityResolver;

    @Inject
    @Named("relative")
    private EntityReferenceResolver<String> relativeResolver;

    @Inject
    private ConverterManager converter;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    private DocumentInstanceOutputProperties properties;

    private WrappingListener contentListener = new WrappingListener();

    private DefaultWikiPrinter currentWikiPrinter;

    private EntityReference currentEntityReference;

    private Locale currentLocale;

    private Date currentCreationDate;

    private String currentCreationAuthor;

    private Locale currentDefaultLocale;

    private XWikiDocument document;

    private BaseClass currentXClass;

    private PropertyClass currentClassProperty;

    private PropertyMetaClassInterface currentClassPropertyMeta;

    private BaseObject currentXObject;

    private BaseClass currentXObjectClass;

    private Object filter;

    protected Object getFilter() throws WikiStreamException
    {
        if (this.filter == null) {
            this.filter = this.filterManager.createCompositeFilter(this.contentListener, this);
        }

        return this.filter;
    }

    public void setProperties(DocumentInstanceOutputProperties properties)
    {
        this.properties = properties;
    }

    public XWikiDocument getDocument()
    {
        return this.document;
    }

    private <T> T get(Type type, String key, FilterEventParameters parameters, T def)
    {
        if (!parameters.containsKey(key)) {
            return def;
        }

        Object value = parameters.get(key);

        if (value == null) {
            return null;
        }

        if (TypeUtils.isInstance(value, type)) {
            return (T) value;
        }

        return this.converter.convert(type, value);
    }

    private Date getDate(String key, FilterEventParameters parameters, Date def)
    {
        return get(Date.class, key, parameters, def);
    }

    private String getString(String key, FilterEventParameters parameters, String def)
    {
        return get(String.class, key, parameters, def);
    }

    private boolean getBoolean(String key, FilterEventParameters parameters, boolean def)
    {
        return get(boolean.class, key, parameters, def);
    }

    private int getInt(String key, FilterEventParameters parameters, int def)
    {
        return get(int.class, key, parameters, def);
    }

    private Syntax getSyntax(String key, FilterEventParameters parameters, Syntax def)
    {
        return get(Syntax.class, key, parameters, def);
    }

    private EntityReference getEntityReference(String key, FilterEventParameters parameters, EntityReference def)
    {
        Object reference = get(Object.class, key, parameters, def);

        if (reference instanceof EntityReference) {
            return (EntityReference) reference;
        }

        return reference != null ? this.relativeResolver.resolve(reference.toString(), EntityType.DOCUMENT, parameters)
            : def;
    }

    // Events

    @Override
    public void beginWiki(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentEntityReference = new EntityReference(name, EntityType.WIKI, this.currentEntityReference);
    }

    @Override
    public void endWiki(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentEntityReference = this.currentEntityReference.getParent();
    }

    @Override
    public void beginWikiSpace(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentEntityReference = new EntityReference(name, EntityType.SPACE, this.currentEntityReference);
    }

    @Override
    public void endWikiSpace(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentEntityReference = this.currentEntityReference.getParent();
    }

    @Override
    public void beginWikiDocument(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentEntityReference = new EntityReference(name, EntityType.DOCUMENT, this.currentEntityReference);

        this.currentDefaultLocale = (Locale) parameters.get(WikiDocumentFilter.PARAMETER_LOCALE);
    }

    @Override
    public void endWikiDocument(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentEntityReference = this.currentEntityReference.getParent();

        this.currentDefaultLocale = null;
    }

    @Override
    public void beginWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentLocale = locale;

        this.currentCreationDate = getDate(WikiDocumentFilter.PARAMETER_CREATION_DATE, parameters, null);
        this.currentCreationAuthor = getString(WikiDocumentFilter.PARAMETER_CREATION_AUTHOR, parameters, null);
    }

    @Override
    public void endWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentLocale = null;

        this.currentCreationDate = null;
        this.currentCreationAuthor = null;
    }

    @Override
    public void beginWikiDocumentRevision(String version, FilterEventParameters parameters) throws WikiStreamException
    {
        this.document =
            new XWikiDocument(this.entityResolver.resolve(this.currentEntityReference, this.properties != null
                ? this.properties.getDefaultReference() : null));

        this.document.setCreationDate(this.currentCreationDate);
        this.document.setCreator(this.currentCreationAuthor);
        this.document.setDefaultLocale(this.currentDefaultLocale);

        this.document.setSyntax(getSyntax(WikiDocumentFilter.PARAMETER_SYNTAX, parameters, null));

        this.document.setLocale(this.currentLocale);

        this.document.setParentReference(getEntityReference(WikiDocumentFilter.PARAMETER_PARENT, parameters, null));
        this.document.setCustomClass(getString(WikiDocumentFilter.PARAMETER_CUSTOMCLASS, parameters, null));
        this.document.setTitle(getString(WikiDocumentFilter.PARAMETER_TITLE, parameters, null));
        this.document.setDefaultTemplate(getString(WikiDocumentFilter.PARAMETER_DEFAULTTEMPLATE, parameters, null));
        this.document.setValidationScript(getString(WikiDocumentFilter.PARAMETER_VALIDATIONSCRIPT, parameters, null));
        this.document.setHidden(getBoolean(WikiDocumentFilter.PARAMETER_HIDDEN, parameters, false));

        this.document.setMinorEdit(getBoolean(WikiDocumentFilter.PARAMETER_REVISION_MINOR, parameters, false));
        this.document.setAuthor(getString(WikiDocumentFilter.PARAMETER_REVISION_AUTHOR, parameters, null));
        this.document.setContentAuthor(getString(WikiDocumentFilter.PARAMETER_CONTENT_AUTHOR, parameters, null));

        String revisions = getString(XWikiWikiDocumentFilter.PARAMETER_JRCSREVISIONS, parameters, null);
        if (revisions != null) {
            try {
                this.document.setDocumentArchive(revisions);
            } catch (XWikiException e) {
                throw new WikiStreamException("Failed to set document archive", e);
            }
        }

        this.document.setVersion(version);

        this.document.setDate(getDate(WikiDocumentFilter.PARAMETER_REVISION_DATE, parameters, new Date()));
        this.document.setComment(getString(WikiDocumentFilter.PARAMETER_REVISION_COMMENT, parameters, ""));

        this.document.setContentUpdateDate(getDate(WikiDocumentFilter.PARAMETER_CONTENT_DATE, parameters, new Date()));

        // Content

        if (parameters.containsKey(WikiDocumentFilter.PARAMETER_CONTENT)) {
            this.document.setContent(getString(WikiDocumentFilter.PARAMETER_CONTENT, parameters, null));
        } else {
            if (this.properties != null && this.properties.getDefaultSyntax() != null) {
                this.document.setSyntax(this.properties.getDefaultSyntax());
            } else {
                // Make sure to set the default syntax if none were provided
                this.document.setSyntax(this.document.getSyntax());
            }

            ComponentManager componentManager = this.componentManagerProvider.get();

            if (componentManager.hasComponent(PrintRendererFactory.class, this.document.getSyntax().toIdString())) {
                PrintRendererFactory rendererFactory;
                try {
                    rendererFactory =
                        componentManager
                            .getInstance(PrintRendererFactory.class, this.document.getSyntax().toIdString());
                } catch (ComponentLookupException e) {
                    throw new WikiStreamException(String.format("Failed to find PrintRendererFactory for syntax [%s]",
                        this.document.getSyntax()), e);
                }

                this.currentWikiPrinter = new DefaultWikiPrinter();
                this.contentListener.setWrappedListener(rendererFactory.createRenderer(this.currentWikiPrinter));
            }
        }
    }

    @Override
    public void endWikiDocumentRevision(String version, FilterEventParameters parameters) throws WikiStreamException
    {
        // Set content
        if (this.currentWikiPrinter != null) {
            this.document.setContent(this.currentWikiPrinter.getBuffer().toString());

            this.contentListener.setWrappedListener(null);
            this.currentWikiPrinter = null;
        }

    }

    @Override
    public void onWikiAttachment(String name, InputStream content, Long size, FilterEventParameters parameters)
        throws WikiStreamException
    {
        XWikiAttachment attachment = new XWikiAttachment(this.document, name);

        attachment.setAuthor("");

        try {
            attachment.setContent(content);
        } catch (IOException e) {
            throw new WikiStreamException("Failed to set attachment content", e);
        }

        // Author

        attachment.setAuthor(getString(WikiAttachmentFilter.PARAMETER_REVISION_AUTHOR, parameters, ""));

        // Version

        if (this.properties == null || this.properties.isVersionPreserved()) {
            if (parameters.containsKey(WikiAttachmentFilter.PARAMETER_REVISION)) {
                attachment.setVersion(getString(WikiAttachmentFilter.PARAMETER_REVISION, parameters, null));
            }
            attachment.setComment(getString(WikiAttachmentFilter.PARAMETER_REVISION_COMMENT, parameters, null));
            attachment.setDate(getDate(WikiAttachmentFilter.PARAMETER_REVISION_DATE, parameters, null));

            String revisions = getString(XWikiWikiAttachmentFilter.PARAMETER_JRCSREVISIONS, parameters, null);
            if (revisions != null) {
                try {
                    attachment.setArchive(revisions);
                } catch (XWikiException e) {
                    throw new WikiStreamException("Failed to set attachment archive", e);
                }
            }

            attachment.setMetaDataDirty(false);
        }

        this.document.addAttachment(attachment);
    }

    @Override
    public void beginWikiClass(FilterEventParameters parameters) throws WikiStreamException
    {
        if (this.currentXObject != null) {
            this.currentXClass = new BaseClass();
            this.currentXClass.setDocumentReference(this.currentXClass.getXClassReference());
            this.currentXObjectClass = this.currentXClass;
        } else {
            this.currentXClass = this.document.getXClass();
        }

        this.currentXClass.setCustomClass(getString(WikiClassFilter.PARAMETER_CUSTOMCLASS, parameters, null));
        this.currentXClass.setCustomMapping(getString(WikiClassFilter.PARAMETER_CUSTOMMAPPING, parameters, null));
        this.currentXClass
            .setDefaultViewSheet(getString(WikiClassFilter.PARAMETER_SHEET_DEFAULTVIEW, parameters, null));
        this.currentXClass
            .setDefaultEditSheet(getString(WikiClassFilter.PARAMETER_SHEET_DEFAULTEDIT, parameters, null));
        this.currentXClass.setDefaultWeb(getString(WikiClassFilter.PARAMETER_DEFAULTSPACE, parameters, null));
        this.currentXClass.setNameField(getString(WikiClassFilter.PARAMETER_NAMEFIELD, parameters, null));
        this.currentXClass.setValidationScript(getString(WikiClassFilter.PARAMETER_VALIDATIONSCRIPT, parameters, null));
    }

    @Override
    public void endWikiClass(FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentXClass = null;
    }

    @Override
    public void beginWikiClassProperty(String name, String type, FilterEventParameters parameters)
        throws WikiStreamException
    {
        ComponentManager componentManager = this.componentManagerProvider.get();

        PropertyClassProvider provider;

        // First try to use the specified class type as hint.
        try {
            if (componentManager.hasComponent(PropertyClassProvider.class, type)) {
                provider = componentManager.getInstance(PropertyClassProvider.class, type);
            } else {
                // In previous versions the class type was the full Java class name of the property class
                // implementation. Extract the hint by removing the Java package prefix and the Class suffix.
                String classType = StringUtils.removeEnd(StringUtils.substringAfterLast(type, "."), "Class");
                provider = componentManager.getInstance(PropertyClassProvider.class, classType);
            }
        } catch (ComponentLookupException e) {
            throw new WikiStreamException(String.format(
                "Failed to get instance of the property class provider for type [%s]", type), e);
        }

        this.currentClassPropertyMeta = provider.getDefinition();

        // We should use PropertyClassInterface (instead of PropertyClass, its default implementation) but it
        // doesn't have the set methods and adding them would breaks the backwards compatibility. We make the
        // assumption that all property classes extend PropertyClass.
        this.currentClassProperty = (PropertyClass) provider.getInstance();
        this.currentClassProperty.setName(name);
        this.currentClassProperty.setObject(this.currentXClass);

        this.currentXClass.safeput(name, this.currentClassProperty);
    }

    @Override
    public void endWikiClassProperty(String name, String type, FilterEventParameters parameters)
        throws WikiStreamException
    {
        this.currentClassPropertyMeta = null;
        this.currentClassProperty = null;
    }

    @Override
    public void onWikiClassPropertyField(String name, String value, FilterEventParameters parameters)
        throws WikiStreamException
    {
        PropertyClass propertyClass;
        try {
            propertyClass = (PropertyClass) this.currentClassPropertyMeta.get(name);
        } catch (XWikiException e) {
            throw new WikiStreamException(String.format(
                "Failed to get definition of field [%s] for property type [%s]", name,
                this.currentClassProperty.getClassType()), e);
        }

        BaseProperty< ? > field = propertyClass.fromString(value);

        this.currentClassProperty.safeput(name, field);
    }

    @Override
    public void beginWikiObject(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        String className = getString(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, parameters, null);

        int number = getInt(WikiObjectFilter.PARAMETER_NUMBER, parameters, 0);

        this.currentXObject = new BaseObject();
        this.currentXObject.setClassName(className);

        if (number < 0) {
            this.document.addXObject(this.currentXObject);
        } else {
            this.document.setXObject(number, this.currentXObject);
        }

        this.currentXObject.setGuid(getString(WikiObjectFilter.PARAMETER_GUID, parameters, null));
    }

    @Override
    public void endWikiObject(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentXObject = null;
        this.currentXObjectClass = null;
    }

    @Override
    public void onWikiObjectProperty(String name, String value, FilterEventParameters parameters)
        throws WikiStreamException
    {
        PropertyClassInterface propertyclass = (PropertyClassInterface) this.currentXObjectClass.safeget(name);

        PropertyInterface property = propertyclass.fromString(value);

        this.currentXObject.safeput(name, property);
    }
}
