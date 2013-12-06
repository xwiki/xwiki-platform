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

import org.apache.commons.lang3.reflect.TypeUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.filter.FilterDescriptorManager;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.properties.ConverterManager;
import org.xwiki.rendering.listener.WrappingListener;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.filter.xwiki.XWikiWikiAttachmentFilter;
import org.xwiki.wikistream.filter.xwiki.XWikiWikiDocumentFilter;
import org.xwiki.wikistream.instance.output.DocumentInstanceOutputProperties;
import org.xwiki.wikistream.internal.output.AbstractBeanOutputWikiStream;
import org.xwiki.wikistream.model.filter.WikiAttachmentFilter;
import org.xwiki.wikistream.model.filter.WikiDocumentFilter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 * @since 5.2
 */
@Component
@Named(DocumentInstanceOutputWikiStreamFactory.ROLEHINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DocumentInstanceOutputWikiStream extends AbstractBeanOutputWikiStream<DocumentInstanceOutputProperties>
    implements WikiDocumentFilter
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
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ConverterManager converter;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private XWikiDocumentOutputWikiStream documentReader;

    private WrappingListener contentListener = new WrappingListener();

    private DefaultWikiPrinter currentWikiPrinter;

    private EntityReference currentEntityReference;

    @Override
    protected Object createFilter() throws WikiStreamException
    {
        return this.filterManager.createCompositeFilter(this.contentListener, this.documentReader, this);
    }

    @Override
    public void close() throws IOException
    {
        // Nothing to close
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

    // Events

    @Override
    public void beginWikiDocument(String arg0, FilterEventParameters arg1) throws WikiStreamException
    {
        // Nothing to do
    }

    @Override
    public void endWikiDocument(String arg0, FilterEventParameters arg1) throws WikiStreamException
    {
        // Nothing to do
    }

    @Override
    public void beginWikiDocumentLocale(Locale arg0, FilterEventParameters arg1) throws WikiStreamException
    {
        // Nothing to do
    }

    @Override
    public void endWikiDocumentLocale(Locale arg0, FilterEventParameters arg1) throws WikiStreamException
    {
        // Nothing to do
    }

    @Override
    public void beginWikiDocumentRevision(String arg0, FilterEventParameters arg1) throws WikiStreamException
    {
        // Nothing to do
    }

    @Override
    public void endWikiDocumentRevision(String version, FilterEventParameters parameters) throws WikiStreamException
    {
        this.documentReader.endWikiDocumentRevision(version, parameters);

        // TODO

        // Set content
        if (this.currentWikiPrinter != null) {
            this.currentDocument.setContent(this.currentWikiPrinter.getBuffer().toString());

            this.contentListener.setWrappedListener(null);
            this.currentWikiPrinter = null;
        }

        XWikiContext xcontext = this.xcontextProvider.get();

        try {
            XWikiDocument document =
                xcontext.getWiki().getDocument(this.currentDocument.getDocumentReferenceWithLocale(), xcontext);

            if (document.isNew()) {
                document = this.currentDocument;
            } else {
                if (this.properties.isPreviousDeleted()) {
                    xcontext.getWiki().deleteDocument(document, xcontext);
                    document = this.currentDocument;
                } else {
                    document.loadAttachmentsContent(xcontext);
                    document.apply(this.currentDocument);
                }
            }

            document.setMinorEdit(getBoolean(WikiDocumentFilter.PARAMETER_REVISION_MINOR, parameters, false));

            // Author

            if (this.properties.isAuthorPreserved()) {
                if (document.isNew()) {
                    document.setCreator(this.currentCreationAuthor);
                }
                document.setAuthor(getString(WikiDocumentFilter.PARAMETER_REVISION_AUTHOR, parameters, null));
                document.setContentAuthor(getString(WikiDocumentFilter.PARAMETER_CONTENT_AUTHOR, parameters, null));
            } else {
                if (document.isNew()) {
                    document.setCreatorReference(xcontext.getUserReference());
                }
                document.setAuthorReference(xcontext.getUserReference());
                document.setContentAuthorReference(xcontext.getUserReference());
            }

            // Versions and save document

            // Don't preserve version or history if we don't delete the previous document
            if (document.isNew() && this.properties.isVersionPreserved()) {
                String revisions = getString(XWikiWikiDocumentFilter.PARAMETER_JRCSREVISIONS, parameters, null);
                if (revisions != null) {
                    try {
                        document.setDocumentArchive(revisions);
                    } catch (XWikiException e) {
                        throw new WikiStreamException("Failed to set document archive", e);
                    }
                }

                document.setCreationDate(this.currentCreationDate);
                document.setDate(getDate(WikiDocumentFilter.PARAMETER_REVISION_DATE, parameters, new Date()));
                document.setComment(getString(WikiDocumentFilter.PARAMETER_REVISION_COMMENT, parameters, ""));

                document
                    .setContentUpdateDate(getDate(WikiDocumentFilter.PARAMETER_CONTENT_DATE, parameters, new Date()));

                document.setVersion(version);

                document.setMetaDataDirty(false);
                document.setContentDirty(false);

                xcontext.getWiki().saveDocument(document, document.getComment(), document.isMinorEdit(), xcontext);
            } else {
                xcontext.getWiki().saveDocument(document, this.properties.getSaveComment(), xcontext);
            }
        } catch (Exception e) {
            throw new WikiStreamException("Failed to save document", e);
        }

        // Cleanup

        this.currentDocument = null;
    }
}
