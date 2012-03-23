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
package org.xwiki.extension.xar.internal.handler.packager.xml;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.xar.internal.handler.ConflictQuestion;
import org.xwiki.extension.xar.internal.handler.packager.DefaultPackager;
import org.xwiki.extension.xar.internal.handler.packager.NotADocumentException;
import org.xwiki.extension.xar.internal.handler.packager.PackageConfiguration;
import org.xwiki.extension.xar.internal.handler.packager.XarEntry;
import org.xwiki.extension.xar.internal.handler.packager.XarEntryMergeResult;
import org.xwiki.extension.xar.internal.handler.packager.XarFile;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeResult;

/**
 * @version $Id$
 * @since 4.0M2
 */
public class DocumentImporterHandler extends DocumentHandler
{
    private XarFile previousXarFile;

    private DefaultPackager packager;

    private XarEntryMergeResult mergeResult;

    private PackageConfiguration configuration;

    public DocumentImporterHandler(DefaultPackager packager, ComponentManager componentManager, String wiki)
    {
        super(componentManager, wiki);

        this.packager = packager;
    }

    public void setPreviousXarFile(XarFile previousXarFile)
    {
        this.previousXarFile = previousXarFile;
    }

    public void setConfiguration(PackageConfiguration configuration)
    {
        this.configuration = configuration;
    }

    public XarEntryMergeResult getMergeResult()
    {
        return this.mergeResult;
    }

    private DocumentReference getUserReference(XWikiContext context)
    {
        DocumentReference userReference = this.configuration.getUserReference();

        if (userReference == null) {
            userReference = context.getUserReference();
        }

        return userReference;
    }

    private void saveDocument(XWikiDocument document, String comment, XWikiContext context) throws Exception
    {
        XWikiDocument currentDocument = getDatabaseDocument();
        DocumentReference userReference = getUserReference(context);

        if (!currentDocument.isNew()) {
            if (document != currentDocument) {
                if (document.isNew()) {
                    currentDocument.apply(document);
                } else {
                    currentDocument = document;
                }
            }
        } else {
            currentDocument = document;
            currentDocument.setCreatorReference(userReference);
        }

        currentDocument.setAuthorReference(userReference);
        currentDocument.setContentAuthorReference(userReference);

        context.getWiki().saveDocument(currentDocument, comment, context);
    }

    private XWikiDocument askDocumentToSave(XWikiDocument currentDocument, XWikiDocument previousDocument,
        XWikiDocument nextDocument, XWikiDocument mergedDocument)
    {
        // Ask what to do
        ConflictQuestion question =
            new ConflictQuestion(currentDocument, previousDocument, nextDocument, mergedDocument);

        if (this.configuration != null && this.configuration.getJobStatus() != null) {
            try {
                this.configuration.getJobStatus().ask(question);
            } catch (InterruptedException e) {
                // TODO: log something ?
            }
        }

        XWikiDocument documentToSave;

        switch (question.getGlobalAction()) {
            case CURRENT:
                documentToSave = currentDocument;
                break;
            case NEXT:
                documentToSave = nextDocument;
                break;
            case PREVIOUS:
                documentToSave = previousDocument;
                break;
            case CUSTOM:
                documentToSave = question.getCustomDocument() != null ? question.getCustomDocument() : mergedDocument;
                break;
            default:
                documentToSave = mergedDocument;
                break;
        }

        return documentToSave;
    }

    private void saveDocument(String comment) throws SAXException
    {
        try {
            XWikiContext context = getXWikiContext();

            XWikiDocument currentDocument = getDatabaseDocument();
            XWikiDocument nextDocument = getDocument();

            // Merge and save
            if (currentDocument != null && !currentDocument.isNew()) {
                XWikiDocument previousDocument = getPreviousDocument();

                if (previousDocument != null) {
                    XWikiDocument mergedDocument = currentDocument.clone();

                    MergeResult documentMergeResult =
                        mergedDocument.merge(previousDocument, nextDocument,
                            this.configuration.getMergeConfiguration(), context);

                    if (documentMergeResult.isModified()) {
                        if (this.configuration.isInteractive() && !documentMergeResult.getErrors().isEmpty()) {
                            XWikiDocument documentToSave =
                                askDocumentToSave(currentDocument, previousDocument, nextDocument, mergedDocument);

                            if (documentToSave != currentDocument) {
                                saveDocument(documentToSave, comment, context);
                            }
                        } else {
                            saveDocument(mergedDocument, comment, context);
                        }
                    }

                    this.mergeResult =
                        new XarEntryMergeResult(new XarEntry(mergedDocument.getDocumentReference(),
                            mergedDocument.getLanguage()), documentMergeResult);
                } else {
                    saveDocument(nextDocument, comment, context);
                }
            } else {
                saveDocument(nextDocument, comment, context);
            }
        } catch (Exception e) {
            throw new SAXException("Failed to save document", e);
        }
    }

    private XWikiDocument getDatabaseDocument() throws ComponentLookupException, XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWikiDocument document = getDocument();

        XWikiDocument existingDocument = context.getWiki().getDocument(document.getDocumentReference(), context);

        if (StringUtils.isNotEmpty(document.getLanguage())) {
            String defaultLanguage = existingDocument.getDefaultLanguage();
            XWikiDocument translatedDocument = existingDocument.getTranslatedDocument(document.getLanguage(), context);

            if (translatedDocument == existingDocument) {
                translatedDocument = new XWikiDocument(document.getDocumentReference());
                translatedDocument.setDefaultLanguage(defaultLanguage);
                translatedDocument.setTranslation(1);
                translatedDocument.setLanguage(document.getLanguage());
            }

            existingDocument = translatedDocument;
        }

        return existingDocument;
    }

    private XWikiDocument getPreviousDocument() throws NotADocumentException, ParserConfigurationException,
        SAXException, IOException
    {
        XWikiDocument previousDocument = null;

        if (this.previousXarFile != null) {
            XWikiDocument document = getDocument();

            DocumentHandler documentHandler = new DocumentHandler(getComponentManager(), document.getWikiName());

            XarEntry realEntry =
                this.previousXarFile.getEntry(new EntityReference(document.getName(), EntityType.DOCUMENT,
                    new EntityReference(document.getSpace(), EntityType.SPACE)), document.getRealLanguage());
            if (realEntry != null) {
                this.packager.parseDocument(this.previousXarFile.getInputStream(realEntry), documentHandler);

                previousDocument = documentHandler.getDocument();
            }
        }

        return previousDocument;
    }

    private void saveAttachment(XWikiAttachment attachment, String comment) throws SAXException
    {
        try {
            XWikiContext context = getXWikiContext();

            // Set proper author
            // TODO: add a setAuthorReference in XWikiAttachment
            XWikiDocument document = getDocument();
            document.setAuthorReference(context.getUserReference());
            attachment.setAuthor(document.getAuthor());

            XWikiDocument dbDocument = getDatabaseDocument();

            XWikiAttachment dbAttachment = dbDocument.getAttachment(attachment.getFilename());

            if (dbAttachment == null) {
                dbDocument.getAttachmentList().add(attachment);
            } else {
                dbAttachment.setContent(attachment.getContentInputStream(context));
                dbAttachment.setFilename(attachment.getFilename());
                dbAttachment.setAuthor(attachment.getAuthor());
            }

            context.getWiki().saveDocument(dbDocument, comment, context);

            // reset content to since it could consume lots of memory and it's not used in diff for now
            attachment.setAttachment_content(null);
            getDocument().getAttachmentList().add(attachment);
        } catch (Exception e) {
            throw new SAXException("Failed to save attachment [" + attachment + "]", e);
        }
    }

    @Override
    protected void endAttachment(String uri, String localName, String qName) throws SAXException
    {
        AttachmentHandler handler = (AttachmentHandler) getCurrentHandler();

        saveAttachment(handler.getAttachment(), "Import: add attachment");
    }

    @Override
    protected void endHandlerElement(String uri, String localName, String qName) throws SAXException
    {
        saveDocument(getDocument().getAttachmentList().isEmpty() ? "Import" : "Import: final save");
    }
}
