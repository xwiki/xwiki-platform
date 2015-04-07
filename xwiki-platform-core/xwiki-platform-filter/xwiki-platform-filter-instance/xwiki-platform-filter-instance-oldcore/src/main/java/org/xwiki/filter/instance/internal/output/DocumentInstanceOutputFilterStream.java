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
package org.xwiki.filter.instance.internal.output;

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
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

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
    private static final TranslationMarker LOG_DOCUMENT_CREATED = new TranslationMarker(
        "filter.instance.log.document.created", WikiDocumentFilter.LOG_DOCUMENT_CREATED);

    private static final TranslationMarker LOG_DOCUMENT_UPDATED = new TranslationMarker(
        "filter.instance.log.document.updated", WikiDocumentFilter.LOG_DOCUMENT_UPDATED);

    private static final TranslationMarker LOG_DOCUMENT_FAILSAVE = new TranslationMarker(
        "filter.instance.log.document.failsave", WikiDocumentFilter.LOG_DOCUMENT_ERROR);

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
    private XWikiDocumentOutputFilterStream documentListener;

    @Inject
    private Logger logger;

    private boolean documentDeleted;

    private FilterEventParameters currentLocaleParameters;

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
    }

    @Override
    public void endWikiDocument(String name, FilterEventParameters parameters) throws FilterException
    {
        // Nothing to do
    }

    @Override
    public void beginWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws FilterException
    {
        this.currentLocaleParameters = parameters;
    }

    @Override
    public void endWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws FilterException
    {
        this.currentLocaleParameters = null;
    }

    @Override
    public void beginWikiDocumentRevision(String version, FilterEventParameters parameters) throws FilterException
    {
        // Nothing to do
    }

    @Override
    public void endWikiDocumentRevision(String version, FilterEventParameters parameters) throws FilterException
    {
        XWikiDocument inputDocument = this.documentListener.getDocument();

        XWikiContext xcontext = this.xcontextProvider.get();

        try {
            XWikiDocument document =
                xcontext.getWiki().getDocument(inputDocument.getDocumentReferenceWithLocale(), xcontext);

            boolean isNew = document.isNew();

            if (document.isNew()) {
                document = inputDocument;
            } else {
                if (this.properties.isPreviousDeleted() && !this.documentDeleted) {
                    // Put previous version in recycle bin
                    if (xcontext.getWiki().hasRecycleBin(xcontext)) {
                        xcontext.getWiki().getRecycleBinStore()
                            .saveToRecycleBin(document, xcontext.getUser(), new Date(), xcontext, true);
                    }

                    // Make sure to not generate DocumentDeletedEvent since from listener point of view it's not
                    xcontext.getWiki().getStore().deleteXWikiDoc(document, xcontext);

                    this.documentDeleted = true;
                    document = inputDocument;
                } else {
                    // Safer to clone for thread safety and in case the save fail
                    document = document.clone();

                    document.loadAttachmentsContent(xcontext);
                    document.apply(inputDocument);
                }
            }

            document.setMinorEdit(inputDocument.isMinorEdit());

            // Author

            if (this.properties.isAuthorPreserved()
                && parameters.containsKey(WikiDocumentFilter.PARAMETER_REVISION_AUTHOR)) {
                document.setAuthorReference(inputDocument.getAuthorReference());
            } else {
                document.setAuthorReference(xcontext.getUserReference());
            }

            // Content author

            if (this.properties.isAuthorPreserved()
                && parameters.containsKey(WikiDocumentFilter.PARAMETER_CONTENT_AUTHOR)) {
                document.setContentAuthorReference(inputDocument.getContentAuthorReference());
            } else {
                document.setContentAuthorReference(document.getAuthorReference());
            }

            // Creator

            if (document.isNew() && !this.properties.isAuthorPreserved()
                || !this.currentLocaleParameters.containsKey(WikiDocumentFilter.PARAMETER_CREATION_AUTHOR)) {
                document.setCreatorReference(document.getAuthorReference());
            }

            // Save history
            if (document.isNew() && document.getDocumentArchive() != null) {
                // we need to force the saving the document archive
                if (document.getDocumentArchive() != null) {
                    xcontext.getWiki().getVersioningStore()
                        .saveXWikiDocArchive(document.getDocumentArchive(xcontext), true, xcontext);
                }
            }

            // Don't preserve version or history if we don't delete the previous document
            if (document.isNew() && (this.properties.isAuthorPreserved() || this.properties.isVersionPreserved())) {
                // Make sure version is set
                document.setVersion(document.getVersion());

                document.setMetaDataDirty(false);
                document.setContentDirty(false);

                xcontext.getWiki().saveDocument(document, document.getComment(), document.isMinorEdit(), xcontext);
            } else {
                xcontext.getWiki().saveDocument(document, this.properties.getSaveComment(), xcontext);
            }

            if (this.properties.isVerbose()) {
                if (isNew) {
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
}
