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
package com.xpn.xwiki.internal.filter.output;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.math.NumberUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.filter.FilterDescriptorManager;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.event.xwiki.XWikiWikiDocumentFilter;
import org.xwiki.filter.input.InputSource;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.listener.WrappingListener;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.filter.XWikiDocumentFilter;
import com.xpn.xwiki.internal.filter.XWikiDocumentFilterCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class XWikiDocumentOutputFilterStream extends AbstractEntityOutputFilterStream<XWikiDocument>
    implements Initializable
{
    @Inject
    private FilterDescriptorManager filterManager;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private RenderingContext renderingContext;

    @Inject
    private LocalizationContext localizationContext;

    @Inject
    private EntityOutputFilterStream<XWikiAttachment> attachmentFilter;

    @Inject
    private EntityOutputFilterStream<BaseClass> classFilter;

    @Inject
    private EntityOutputFilterStream<BaseObject> objectFilter;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    private WrappingListener contentListener = new WrappingListener();

    private DefaultWikiPrinter currentWikiPrinter;

    private Syntax previousTargetSyntax;

    private Locale currentLocale;

    private String currentVersion;

    private FilterEventParameters currentLocaleParameters;

    private Locale currentDefaultLocale;

    private UserReference previousCreationAuthor;

    private Date previousCreationDate;

    @Override
    public void initialize() throws InitializationException
    {
        initialize(this.attachmentFilter, this.classFilter, this.objectFilter);
    }

    @Override
    protected Object createFilter()
    {
        List<XWikiDocumentFilter> filters = new ArrayList<>(this.children.size() + 1);
        for (EntityOutputFilterStream<?> child : this.children) {
            filters.add((XWikiDocumentFilter) child.getFilter());
        }
        filters.add(this);

        this.filter = new XWikiDocumentFilterCollection(filters)
        {
            @Override
            public void beginWikiClass(FilterEventParameters parameters) throws FilterException
            {
                if (!objectFilter.isEnabled()) {
                    classFilter.enable();
                }

                super.beginWikiClass(parameters);
            }

            @Override
            public void endWikiClass(FilterEventParameters parameters) throws FilterException
            {
                super.endWikiClass(parameters);

                classFilter.disable();
            }

            @Override
            public void beginWikiObject(String name, FilterEventParameters parameters) throws FilterException
            {
                objectFilter.enable();

                super.beginWikiObject(name, parameters);
            }

            @Override
            public void endWikiObject(String name, FilterEventParameters parameters) throws FilterException
            {
                super.endWikiObject(name, parameters);

                objectFilter.disable();
            }

            @Override
            public void onWikiAttachment(String name, InputStream content, Long size, FilterEventParameters parameters)
                throws FilterException
            {
                attachmentFilter.enable();

                super.onWikiAttachment(name, content, size, parameters);

                attachmentFilter.disable();
            }
        };

        if (this.contentListener != null) {
            // Inject listener for the document content events
            return this.filterManager.createCompositeFilter(this.contentListener, this.filter);
        } else {
            return this.filter;
        }
    }

    private XWikiAttachmentOutputFilterStream getXWikiAttachmentOutputFilterStream()
    {
        return (XWikiAttachmentOutputFilterStream) this.attachmentFilter;
    }

    private BaseClassOutputFilterStream getBaseClassOutputFilterStream()
    {
        return (BaseClassOutputFilterStream) this.classFilter;
    }

    private BaseObjectOutputFilterStream getBaseObjectOutputFilterStream()
    {
        return (BaseObjectOutputFilterStream) this.objectFilter;
    }

    /**
     * Optimization to disable any plumbing to support rendering events. To be used in case we know this kind of event
     * won't be received or we want to ignore them. Should be called before {@link #getFilter()}.
     */
    public void disableRenderingEvents()
    {
        this.contentListener = null;
    }

    // Events

    private EntityReference getDefaultDocumentReference()
    {
        if (this.properties != null && this.properties.getDefaultReference() != null) {
            return this.properties.getDefaultReference();
        }

        if (this.entity != null) {
            return this.entity.getDocumentReference();
        }

        return null;
    }

    private void begin(FilterEventParameters parameters) throws FilterException
    {
        DocumentReference documentReference =
            this.documentEntityResolver.resolve(this.currentEntityReference, getDefaultDocumentReference());

        if (this.entity == null) {
            this.entity = new XWikiDocument(documentReference, this.currentLocale);
        } else {
            this.entity.setDocumentReference(documentReference);
            this.entity.setLocale(this.currentLocale);
        }

        // Mark the document as restricted to avoid that any scripts are executed as scripts should only be executed
        // on the current, saved version, see https://jira.xwiki.org/browse/XWIKI-20594
        this.entity.setRestricted(true);

        // Find default author
        DocumentReference defaultAuthorDocumentReference;
        // TODO: move to UserReference based APIs in DocumentInstanceOutputProperties
        if (this.properties.isAuthorSet()) {
            defaultAuthorDocumentReference = this.properties.getAuthor();
        } else {
            XWikiContext xcontext = xcontextProvider.get();
            defaultAuthorDocumentReference = xcontext != null ? xcontext.getUserReference() : null;
        }
        UserReference defaultAuthorReference = this.userDocumentResolver.resolve(defaultAuthorDocumentReference);

        // Resolve the current effective author
        this.entity.getAuthors().setEffectiveMetadataAuthor(getUserReference(
            WikiDocumentFilter.PARAMETER_REVISION_EFFECTIVEMETADATA_AUTHOR, parameters, defaultAuthorReference));
        // Use effective metadata author as default
        this.entity.getAuthors()
            .setOriginalMetadataAuthor(getUserReference(WikiDocumentFilter.PARAMETER_REVISION_ORIGINALMETADATA_AUTHOR,
                parameters, this.entity.getAuthors().getEffectiveMetadataAuthor()));
        this.entity.getAuthors().setContentAuthor(
            getUserReference(WikiDocumentFilter.PARAMETER_CONTENT_AUTHOR, parameters,
                this.entity.getAuthors().getEffectiveMetadataAuthor()));

        this.entity.setDate(getDate(WikiDocumentFilter.PARAMETER_REVISION_DATE, parameters, new Date()));
        this.entity.setContentUpdateDate(
            getDate(WikiDocumentFilter.PARAMETER_CONTENT_DATE, parameters, this.entity.getDate()));

        UserReference defaultCreationAuthor = this.previousCreationAuthor;
        Date defaultCreationDate = this.previousCreationDate;
        if (defaultCreationAuthor == null) {
            // Use the effective author as creator by default for the first version
            defaultCreationAuthor = this.entity.getAuthors().getEffectiveMetadataAuthor();
            defaultCreationDate = this.entity.getDate();
        }

        this.entity.getAuthors().setCreator(getUserReference(WikiDocumentFilter.PARAMETER_CREATION_AUTHOR,
            this.currentLocaleParameters, defaultCreationAuthor));
        this.entity.setCreationDate(
            getDate(WikiDocumentFilter.PARAMETER_CREATION_DATE, this.currentLocaleParameters, defaultCreationDate));

        this.entity.setDefaultLocale(this.currentDefaultLocale);

        this.entity.setSyntax(getSyntax(WikiDocumentFilter.PARAMETER_SYNTAX, parameters, null));

        this.entity.setParentReference(getEntityReference(WikiDocumentFilter.PARAMETER_PARENT, parameters, null));
        this.entity.setCustomClass(getString(WikiDocumentFilter.PARAMETER_CUSTOMCLASS, parameters, null));
        this.entity.setTitle(getString(WikiDocumentFilter.PARAMETER_TITLE, parameters, null));
        this.entity.setDefaultTemplate(getString(WikiDocumentFilter.PARAMETER_DEFAULTTEMPLATE, parameters, null));
        this.entity.setValidationScript(getString(WikiDocumentFilter.PARAMETER_VALIDATIONSCRIPT, parameters, null));
        this.entity.setHidden(getBoolean(WikiDocumentFilter.PARAMETER_HIDDEN, parameters, false));

        this.entity.setMinorEdit(getBoolean(WikiDocumentFilter.PARAMETER_REVISION_MINOR, parameters, false));

        String revisions =
            getString(XWikiWikiDocumentFilter.PARAMETER_JRCSREVISIONS, this.currentLocaleParameters, null);
        if (revisions != null) {
            try {
                this.entity.setDocumentArchive(revisions);
            } catch (XWikiException e) {
                throw new FilterException("Failed to set document archive", e);
            }
        }

        if (this.currentVersion != null && this.properties.isVersionPreserved()) {
            if (VALID_VERSION.matcher(this.currentVersion).matches()) {
                this.entity.setVersion(this.currentVersion);
            } else if (NumberUtils.isDigits(this.currentVersion)) {
                this.entity.setVersion(this.currentVersion + ".1");
            } else {
                // TODO: log something, probably a warning
            }
        }

        this.entity.setComment(getString(WikiDocumentFilter.PARAMETER_REVISION_COMMENT, parameters, ""));

        // Content

        if (this.contentListener != null) {
            // Remember the current rendering context target syntax
            this.previousTargetSyntax = this.renderingContext.getTargetSyntax();
        }

        if (parameters.containsKey(WikiDocumentFilter.PARAMETER_CONTENT)) {
            this.entity.setContent(getString(WikiDocumentFilter.PARAMETER_CONTENT, parameters, null));

            if (this.contentListener != null) {
                // Cancel any existing content listener
                this.currentWikiPrinter = null;
                this.contentListener.setWrappedListener(null);
            }
        } else if (this.contentListener != null) {
            if (this.properties != null && this.properties.getDefaultSyntax() != null) {
                this.entity.setSyntax(this.properties.getDefaultSyntax());
            } else {
                // Make sure to set the default syntax if none were provided
                this.entity.setSyntax(this.entity.getSyntax());
            }

            ComponentManager componentManager = this.componentManagerProvider.get();

            String syntaxString = this.entity.getSyntax().toIdString();
            if (componentManager.hasComponent(PrintRendererFactory.class, syntaxString)) {
                PrintRendererFactory rendererFactory;
                try {
                    rendererFactory = componentManager.getInstance(PrintRendererFactory.class, syntaxString);
                } catch (ComponentLookupException e) {
                    throw new FilterException(
                        String.format("Failed to find PrintRendererFactory for syntax [%s]", this.entity.getSyntax()),
                        e);
                }

                this.currentWikiPrinter = new DefaultWikiPrinter();
                ((MutableRenderingContext) this.renderingContext).setTargetSyntax(rendererFactory.getSyntax());
                this.contentListener.setWrappedListener(rendererFactory.createRenderer(this.currentWikiPrinter));
            }
        }

        // Initialize the class
        getBaseClassOutputFilterStream().setEntity(this.entity.getXClass());
    }

    private void end(FilterEventParameters parameters)
    {
        // Set content
        if (this.currentWikiPrinter != null) {
            this.entity.setContent(this.currentWikiPrinter.getBuffer().toString());

            this.contentListener.setWrappedListener(null);
            this.currentWikiPrinter = null;
        }

        if (this.contentListener != null) {
            // Reset
            ((MutableRenderingContext) this.renderingContext).setTargetSyntax(this.previousTargetSyntax);
        }
    }

    @Override
    public void beginWikiDocument(String name, FilterEventParameters parameters) throws FilterException
    {
        super.beginWikiDocument(name, parameters);

        if (parameters.containsKey(WikiDocumentFilter.PARAMETER_LOCALE)) {
            this.currentDefaultLocale = get(Locale.class, WikiDocumentFilter.PARAMETER_LOCALE, parameters, Locale.ROOT);
        } else {
            this.currentDefaultLocale = this.localizationContext.getCurrentLocale();
        }

        this.currentLocale = Locale.ROOT;
        this.currentLocaleParameters = parameters;

        begin(parameters);
    }

    @Override
    public void endWikiDocument(String name, FilterEventParameters parameters) throws FilterException
    {
        end(parameters);

        super.endWikiDocument(name, parameters);

        // Reset
        this.currentLocaleParameters = null;
        this.currentLocale = null;
        this.currentDefaultLocale = null;
        this.previousCreationAuthor = null;
        this.previousCreationDate = null;
    }

    @Override
    public void beginWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws FilterException
    {
        this.currentLocale = locale;
        this.currentLocaleParameters = parameters;

        begin(parameters);
    }

    @Override
    public void endWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws FilterException
    {
        end(parameters);

        // Reset
        this.currentLocale = null;
        this.currentLocaleParameters = null;
        this.previousCreationAuthor = null;
        this.previousCreationDate = null;
    }

    @Override
    public void beginWikiDocumentRevision(String version, FilterEventParameters parameters) throws FilterException
    {
        this.currentVersion = version;

        begin(parameters);
    }

    @Override
    public void endWikiDocumentRevision(String version, FilterEventParameters parameters) throws FilterException
    {
        end(parameters);

        // Remember some metadata for next entity
        this.previousCreationAuthor = this.entity.getAuthors().getCreator();
        this.previousCreationDate = this.entity.getCreationDate();

        // Reset
        this.currentVersion = null;
    }

    @Override
    public void endWikiClass(FilterEventParameters parameters) throws FilterException
    {
        getBaseClassOutputFilterStream().setEntity(null);
    }

    @Override
    public void onWikiAttachment(String name, InputStream content, Long size, FilterEventParameters parameters)
        throws FilterException
    {
        endAttachment();
    }

    @Override
    public void endWikiDocumentAttachment(String name, InputSource content, Long size, FilterEventParameters parameters)
        throws FilterException
    {
        endAttachment();
    }

    private void endAttachment()
    {
        this.entity.setAttachment(getXWikiAttachmentOutputFilterStream().getEntity());

        // Reset attachment
        getXWikiAttachmentOutputFilterStream().setEntity(null);
    }

    @Override
    public void endWikiObject(String name, FilterEventParameters parameters) throws FilterException
    {
        super.endWikiObject(name, parameters);

        BaseObject baseObject = getBaseObjectOutputFilterStream().getEntity();

        if (baseObject.getNumber() < 0) {
            this.entity.addXObject(baseObject);
        } else {
            this.entity.setXObject(baseObject.getNumber(), baseObject);
        }

        getBaseObjectOutputFilterStream().setEntity(null);
    }
}
