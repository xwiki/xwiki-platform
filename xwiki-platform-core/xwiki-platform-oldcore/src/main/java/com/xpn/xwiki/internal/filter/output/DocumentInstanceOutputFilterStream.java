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

import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.filter.FilterDescriptorManager;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.instance.output.DocumentInstanceOutputProperties;
import org.xwiki.filter.output.AbstractBeanOutputFilterStream;
import org.xwiki.logging.marker.TranslationMarker;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Named(DocumentInstanceOutputFilterStreamFactory.ROLEHINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DocumentInstanceOutputFilterStream extends AbstractBeanOutputFilterStream<DocumentInstanceOutputProperties>
    implements WikiDocumentFilter
{
    private static final TranslationMarker LOG_DOCUMENT_CREATED =
        new TranslationMarker("filter.instance.log.document.created", WikiDocumentFilter.LOG_DOCUMENT_CREATED);

    private static final TranslationMarker LOG_DOCUMENT_UPDATED =
        new TranslationMarker("filter.instance.log.document.updated", WikiDocumentFilter.LOG_DOCUMENT_UPDATED);

    private static final TranslationMarker LOG_DOCUMENT_FAILSAVE =
        new TranslationMarker("filter.instance.log.document.failsave", WikiDocumentFilter.LOG_DOCUMENT_ERROR);

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
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private EntityOutputFilterStream<XWikiDocument> documentListener;

    @Inject
    private Logger logger;

    private boolean documentDeleted;

    private FilterEventParameters currentLocaleParameters;

    private FilterEventParameters currentRevisionParameters;

    private XWikiDocumentOutputFilterStream getXWikiDocumentOutputFilterStream()
    {
        return (XWikiDocumentOutputFilterStream) this.documentListener;
    }

    @Override
    protected Object createFilter() throws FilterException
    {
        return this.filterManager.createCompositeFilter(this.documentListener.getFilter(), this);
    }

    @Override
    public void close() throws IOException
    {
        // Nothing to close
    }

    @Override
    public void setProperties(DocumentInstanceOutputProperties properties) throws FilterException
    {
        super.setProperties(properties);

        this.documentListener.setProperties(properties);
    }

    // Events

    @Override
    public void beginWikiDocument(String name, FilterEventParameters parameters) throws FilterException
    {
        this.documentDeleted = false;

        this.currentLocaleParameters = parameters;
        this.currentRevisionParameters = parameters;
    }

    @Override
    public void endWikiDocument(String name, FilterEventParameters parameters) throws FilterException
    {
        maybeSaveDocument();

        // Reset
        this.currentRevisionParameters = null;
        this.currentLocaleParameters = null;
    }

    @Override
    public void beginWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws FilterException
    {
        this.currentLocaleParameters = parameters;
        this.currentRevisionParameters = parameters;
    }

    @Override
    public void endWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws FilterException
    {
        maybeSaveDocument();

        // Reset
        this.currentRevisionParameters = null;
        this.currentLocaleParameters = null;
    }

    @Override
    public void beginWikiDocumentRevision(String version, FilterEventParameters parameters) throws FilterException
    {
        this.currentRevisionParameters = parameters;
    }

    @Override
    public void endWikiDocumentRevision(String version, FilterEventParameters parameters) throws FilterException
    {
        maybeSaveDocument();

        // Reset
        this.currentRevisionParameters = null;
    }

    private void maybeSaveDocument() throws FilterException
    {
        XWikiDocument inputDocument = this.documentListener.getEntity();
        this.documentListener.setEntity(null);

        if (this.currentRevisionParameters == null) {
            return;
        }

        XWikiContext xcontext = this.xcontextProvider.get();

        try {
            XWikiDocument document =
                xcontext.getWiki().getDocument(inputDocument.getDocumentReferenceWithLocale(), xcontext);

            if (!this.documentDeleted && !document.isNew() && this.properties.isPreviousDeleted()) {
                XWikiDocument originalDocument = document;

                // Save current context wiki
                WikiReference currentWiki = xcontext.getWikiReference();
                try {
                    // Make sure the store is executed in the right context
                    xcontext.setWikiReference(document.getDocumentReference().getWikiReference());

                    // Put previous version in recycle bin
                    if (xcontext.getWiki().hasRecycleBin(xcontext)) {
                        xcontext.getWiki().getRecycleBinStore().saveToRecycleBin(document, xcontext.getUser(),
                            new Date(), xcontext, true);
                    }

                    // Make sure to not generate DocumentDeletedEvent since from listener point of view it's not
                    xcontext.getWiki().getStore().deleteXWikiDoc(document, xcontext);
                    this.documentDeleted = true;
                } finally {
                    // Restore current context wiki
                    xcontext.setWikiReference(currentWiki);
                }

                document = xcontext.getWiki().getDocument(inputDocument.getDocumentReferenceWithLocale(), xcontext);

                // Remember deleted document as the actual previous version of the document (to simulate an update
                // instead of a creation)
                document.setOriginalDocument(originalDocument);
            } else {
                // Make sure to remember that the document should not be deleted anymore
                this.documentDeleted = true;
            }

            // Remember if it's a creation or an update
            boolean isnew = document.isNew();

            // Safer to clone for thread safety and in case the save fail
            document = document.clone();

            document.loadAttachmentsContentSafe(xcontext);
            document.apply(inputDocument);

            // Get the version from the input document

            document.setMinorEdit(inputDocument.isMinorEdit());

            // Authors

            if (!this.properties.isAuthorPreserved()) {
                if (this.properties.isAuthorSet()) {
                    setAuthorReference(document, this.properties.getAuthor());
                } else {
                    setAuthorReference(document, xcontext.getUserReference());
                }
                document.setContentAuthorReference(document.getAuthorReference());
                if (document.isNew()) {
                    document.setCreatorReference(document.getAuthorReference());
                }
            } else {
                setAuthors(document, inputDocument);
            }

            // Version related information and save

            if (this.properties.isVersionPreserved()) {
                // Make sure to use metadata coming from the input document
                document.setVersion(inputDocument.getVersion());
                document.setDate(inputDocument.getDate());
                document.setContentUpdateDate(inputDocument.getContentUpdateDate());
                for (XWikiAttachment attachment : document.getAttachmentList()) {
                    attachment.setVersion(inputDocument.getAttachment(attachment.getFilename()).getVersion());
                }
                if (document.isNew()) {
                    document.setCreationDate(inputDocument.getCreationDate());
                    document.setDocumentArchive(inputDocument.getDocumentArchive());
                }

                // Make sure the document won't be modified by the store
                document.setMetaDataDirty(false);
                document.setContentDirty(false);

                xcontext.getWiki().saveDocument(document, inputDocument.getComment(), inputDocument.isMinorEdit(),
                    xcontext);
            } else {
                // Forget the input history to let the store do its standard job
                document.setDocumentArchive((XWikiDocumentArchive) null);

                xcontext.getWiki().saveDocument(document, this.properties.getSaveComment(), xcontext);
            }

            if (this.properties.isVerbose()) {
                if (isnew) {
                    this.logger.info(LOG_DOCUMENT_CREATED, "Created document [{}]",
                        document.getDocumentReferenceWithLocale());
                } else {
                    this.logger.info(LOG_DOCUMENT_UPDATED, "Updated document [{}]",
                        document.getDocumentReferenceWithLocale());
                }
            }
        } catch (Exception e) {
            this.logger.error(LOG_DOCUMENT_FAILSAVE, "Failed to save document [{}]",
                inputDocument.getDocumentReferenceWithLocale(), e);

            if (this.properties.isStoppedWhenSaveFail()) {
                throw new FilterException("Failed to save document", e);
            }
        }
    }

    private void setAuthorReference(XWikiDocument document, DocumentReference authorReference)
    {
        // Document author
        document.setAuthorReference(authorReference);

        // Attachments author
        for (XWikiAttachment attachment : document.getAttachmentList()) {
            attachment.setAuthorReference(authorReference);
        }
    }

    private void setAuthors(XWikiDocument document, XWikiDocument inputDocument)
    {
        // Document author
        document.setAuthorReference(inputDocument.getAuthorReference());
        document.setContentAuthorReference(inputDocument.getContentAuthorReference());
        if (document.isNew()) {
            document.setCreatorReference(inputDocument.getCreatorReference());
        }

        // Attachments author
        for (XWikiAttachment currentAttachment : document.getAttachmentList()) {
            currentAttachment
                .setAuthorReference(inputDocument.getAttachment(currentAttachment.getFilename()).getAuthorReference());
        }
    }
}
