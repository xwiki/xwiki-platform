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
package org.xwiki.extension.xar.internal.handler.packager;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.xar.XWikiDocumentMerger;
import org.xwiki.extension.xar.XWikiDocumentMergerConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.xar.XarEntry;
import org.xwiki.xar.XarEntryType;
import org.xwiki.xar.XarEntryType.UpgradeType;
import org.xwiki.xar.XarEntryTypeResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Take care of properly merging and saving a document.
 * 
 * @version $Id$
 */
@Component(roles = DocumentMergeImporter.class)
@Singleton
public class DocumentMergeImporter
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private XWikiDocumentMerger defaultMerger;

    @Inject
    private XarEntryTypeResolver typeResolver;

    @Inject
    private EntityReferenceSerializer<String> referenceSerializer;

    /**
     * @param comment the comment to use if saving the document
     * @param currentDocument the document currently in database
     * @param previousDocument the standard version of the document
     * @param nextDocument the new standard version of the document
     * @param configuration the configuration of the import
     * @throws Exception when failing to import document
     */
    public void importDocument(String comment, XWikiDocument previousDocument, XWikiDocument currentDocument,
        XWikiDocument nextDocument, PackageConfiguration configuration) throws Exception
    {
        XarEntryType type = this.typeResolver.getDefault();
        XWikiDocumentMerger merger = this.defaultMerger;

        ComponentManager componentManager = this.componentManagerProvider.get();

        XarEntry xarEntry = configuration.getXarEntry();
        if (xarEntry != null) {
            String reference = "document:" + this.referenceSerializer.serialize(xarEntry);

            // Resolve the type
            XarEntryType configuredType = this.typeResolver.resolve(xarEntry, false);

            // Try a merger specific to the type name
            if (configuredType != null
                && componentManager.hasComponent(XWikiDocumentMerger.class, configuredType.getName())) {
                // Try a merger specific to the XAR entry type
                merger = componentManager.getInstance(XWikiDocumentMerger.class, configuredType.getName());
            } else if (componentManager.hasComponent(XWikiDocumentMerger.class, reference)) {
                // Try a merger specific to the document reference
                merger = componentManager.getInstance(XWikiDocumentMerger.class, reference);
            }

            if (configuredType != null) {
                type = configuredType;
            }
        }

        XWikiDocumentMergerConfiguration mergeConfiguration = new XWikiDocumentMergerConfiguration();
        mergeConfiguration.setAuthorReference(configuration.getUserReference());
        mergeConfiguration.setConflictActions(configuration.getConflictActions());
        mergeConfiguration.setType(getUpgradeType(type));

        XWikiDocument documentToSave =
            merger.merge(currentDocument != null && !currentDocument.isNew() ? currentDocument : null, previousDocument,
                nextDocument, mergeConfiguration);

        if (documentToSave != null && documentToSave != currentDocument) {
            saveDocument(documentToSave, comment, configuration);
        }
    }

    private UpgradeType getUpgradeType(XarEntryType type)
    {
        if (type != null) {
            return type.getUpgradeType();
        }

        return null;
    }

    private void saveDocument(XWikiDocument document, String comment, PackageConfiguration configuration)
        throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument currentDocument =
            xcontext.getWiki().getDocument(document.getDocumentReferenceWithLocale(), xcontext);

        if (!currentDocument.isNew()) {
            if (document != currentDocument) {
                if (document.isNew()) {
                    currentDocument.loadAttachmentsContentSafe(xcontext);
                    currentDocument.apply(document);
                } else {
                    currentDocument = document;
                }
            }
        } else {
            currentDocument = document;
        }

        // Set document authors
        setDocumentAuthor(currentDocument, document, configuration);

        saveDocumentSetContextUser(currentDocument, comment);
    }

    private void setDocumentAuthor(XWikiDocument currentDocument, XWikiDocument document,
        PackageConfiguration configuration)
    {
        DocumentReference configuredUser = configuration.getUserReference();
        if (configuredUser != null) {
            if (currentDocument.isNew()) {
                currentDocument.setCreatorReference(configuredUser);
            }
            currentDocument.setAuthorReference(configuredUser);
            currentDocument.setContentAuthorReference(configuredUser);

            // Set attachments authors
            for (XWikiAttachment attachment : currentDocument.getAttachmentList()) {
                if (attachment.isContentDirty()) {
                    attachment.setAuthorReference(currentDocument.getAuthorReference());
                }
            }
        } else {
            if (document != currentDocument) {
                if (currentDocument.isNew()) {
                    currentDocument.setCreatorReference(document.getCreatorReference());
                }
                currentDocument.setAuthorReference(document.getAuthorReference());
                currentDocument.setContentAuthorReference(document.getContentAuthorReference());

                // Set attachments authors
                for (XWikiAttachment attachment : document.getAttachmentList()) {
                    if (attachment.isContentDirty()) {
                        currentDocument.getAttachment(attachment.getFilename())
                            .setAuthorReference(attachment.getAuthorReference());
                    }
                }
            }

            // Make sure to keep the content author we want
            currentDocument.setContentDirty(false);
            currentDocument.setContentUpdateDate(new Date());
        }
    }

    private void saveDocumentSetContextUser(XWikiDocument document, String comment) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        DocumentReference userReference = xcontext.getUserReference();

        try {
            // Make sure to have context user corresponding to document author for badly designed listeners expecting
            // the document to actually be saved by context user
            xcontext.setUserReference(document.getAuthorReference());

            xcontext.getWiki().saveDocument(document, comment, false, xcontext);
        } finally {
            xcontext.setUserReference(userReference);
        }
    }
}
