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
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.filter.FilterDescriptorManager;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.instance.output.DocumentInstanceOutputProperties;
import org.xwiki.filter.output.AbstractBeanOutputFilterStream;
import org.xwiki.logging.marker.TranslationMarker;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

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
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private EntityOutputFilterStream<XWikiDocument> documentListener;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @Inject
    private Logger logger;

    private boolean firstVersion;

    private FilterEventParameters currentLocaleParameters;

    private FilterEventParameters currentRevisionParameters;

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
        this.currentLocaleParameters = parameters;
        this.currentRevisionParameters = parameters;

        // Init the first version marker
        this.firstVersion = true;
    }

    @Override
    public void endWikiDocument(String name, FilterEventParameters parameters) throws FilterException
    {
        maybeSaveDocument();

        // Reset
        this.currentRevisionParameters = null;
        this.currentLocaleParameters = null;
        this.firstVersion = false;
    }

    @Override
    public void beginWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws FilterException
    {
        this.currentLocaleParameters = parameters;
        this.currentRevisionParameters = parameters;

        // Init the first version marker
        this.firstVersion = true;
    }

    @Override
    public void endWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws FilterException
    {
        maybeSaveDocument();

        // Reset
        this.currentRevisionParameters = null;
        this.currentLocaleParameters = null;
        this.firstVersion = false;
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
        this.firstVersion = false;
    }

    private void maybeSaveDocument() throws FilterException
    {
        XWikiDocument inputDocument = this.documentListener.getEntity();
        this.documentListener.setEntity(null);

        if (this.currentRevisionParameters == null) {
            return;
        }

        boolean hasJRCSHistory = inputDocument.getDocumentArchive() != null;

        XWikiContext xcontext = this.xcontextProvider.get();

        try {
            XWikiDocument databaseDocument =
                xcontext.getWiki().getDocument(inputDocument.getDocumentReferenceWithLocale(), xcontext);

            // Remember if it's a creation or an update
            boolean isnew = databaseDocument.isNew();

            // Make sure document's attachments content are loaded from the store
            databaseDocument.loadAttachmentsContentSafe(xcontext);

            XWikiDocument document;
            if (this.firstVersion && this.properties.isPreviousDeleted()) {
                // We want to replace the existing document
                document = inputDocument;

                // But it's still an update from outside world point of view
                document.setOriginalDocument(databaseDocument);
            } else {
                // Safer to clone for thread safety and in case the save fail
                document = databaseDocument.clone();

                // We want to update the existing document
                document.apply(inputDocument);

                // Get the version from the input document
                document.setMinorEdit(inputDocument.isMinorEdit());

                // Copy input document authors if they should be preserved
                if (this.properties.isAuthorPreserved()) {
                    setAuthors(document, inputDocument);
                }
            }

            // Authors

            if (!this.properties.isAuthorPreserved()) {
                if (this.properties.isAuthorSet()) {
                    setAuthorReference(document, this.properties.getAuthor());
                } else {
                    setAuthorReference(document, xcontext.getUserReference());
                }
                DocumentAuthors authors = document.getAuthors();
                authors.setContentAuthor(authors.getEffectiveMetadataAuthor());
                if (document.isNew()) {
                    authors.setCreator(authors.getEffectiveMetadataAuthor());
                }
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

                // Make sure the document is stored exactly as is (don't increment version, etc.)
                document.setMetaDataDirty(false);
                document.setContentDirty(false);
                document.getAttachmentList().forEach(a -> a.setMetaDataDirty(false));

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
        UserReference authorUserReference = this.documentReferenceUserReferenceResolver.resolve(authorReference);
        document.setAuthor(authorUserReference);

        // Attachments author
        for (XWikiAttachment attachment : document.getAttachmentList()) {
            attachment.setAuthorReference(authorReference);
        }
    }

    private void setAuthors(XWikiDocument document, XWikiDocument inputDocument)
    {
        // Document author
        DocumentAuthors documentAuthors = document.getAuthors();
        DocumentAuthors inputDocumentAuthors = inputDocument.getAuthors();
        if (document.isNew()) {
            documentAuthors.copyAuthors(inputDocumentAuthors);
        } else {
            documentAuthors.setEffectiveMetadataAuthor(inputDocumentAuthors.getEffectiveMetadataAuthor());
            documentAuthors.setOriginalMetadataAuthor(inputDocumentAuthors.getOriginalMetadataAuthor());
            documentAuthors.setContentAuthor(inputDocumentAuthors.getContentAuthor());
        }

        // Attachments author
        for (XWikiAttachment currentAttachment : document.getAttachmentList()) {
            currentAttachment
                .setAuthorReference(inputDocument.getAttachment(currentAttachment.getFilename()).getAuthorReference());
        }
    }
}
