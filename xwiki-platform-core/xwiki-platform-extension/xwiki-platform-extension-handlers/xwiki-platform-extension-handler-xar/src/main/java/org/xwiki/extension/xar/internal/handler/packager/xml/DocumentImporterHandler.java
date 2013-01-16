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
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.xar.internal.handler.packager.DefaultPackager;
import org.xwiki.extension.xar.internal.handler.packager.DocumentMergeImporter;
import org.xwiki.extension.xar.internal.handler.packager.NotADocumentException;
import org.xwiki.extension.xar.internal.handler.packager.PackageConfiguration;
import org.xwiki.extension.xar.internal.handler.packager.XarEntry;
import org.xwiki.extension.xar.internal.handler.packager.XarEntryMergeResult;
import org.xwiki.extension.xar.internal.handler.packager.XarFile;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 * @since 4.0M2
 */
public class DocumentImporterHandler extends DocumentHandler
{
    private DefaultPackager packager;

    private XarEntryMergeResult mergeResult;

    private PackageConfiguration configuration;

    private EntityReferenceSerializer<String> compactWikiSerializer;

    private DocumentMergeImporter importer;

    /**
     * Attachment are imported before trying to merge a document for memory handling reasons so we need to know if there
     * was really an existing document before starting to import attachments.
     */
    private Boolean hasCurrentDocument;

    public DocumentImporterHandler(DefaultPackager packager, ComponentManager componentManager, String wiki,
        DocumentMergeImporter importer) throws ComponentLookupException
    {
        super(componentManager, wiki);

        this.compactWikiSerializer =
            getComponentManager().getInstance(EntityReferenceSerializer.TYPE_STRING, "compactwiki");

        this.packager = packager;
        this.importer = importer;
    }

    public void setConfiguration(PackageConfiguration configuration)
    {
        this.configuration = configuration;
    }

    public XarEntryMergeResult getMergeResult()
    {
        return this.mergeResult;
    }

    private String getUserString()
    {
        return this.compactWikiSerializer.serialize(this.configuration.getUserReference(), getDocument()
            .getDocumentReference());
    }

    private void saveDocumentSetContextUser(XWikiDocument document, String comment, boolean isMinorEdit,
        XWikiContext context) throws Exception
    {
        DocumentReference userReference = context.getUserReference();

        try {
            // Make sure to have context user corresponding to document author for badly designed listeners expecting
            // the document to actually be saved by context user
            context.setUserReference(document.getAuthorReference());

            context.getWiki().saveDocument(document, comment, isMinorEdit, context);
        } catch (Exception e) {
            context.setUserReference(userReference);
        }
    }

    private void saveDocument(String comment) throws SAXException
    {
        try {
            XWikiDocument databaseDocument = getDatabaseDocument();

            this.mergeResult =
                this.importer.saveDocument(comment, getPreviousDocument(), this.hasCurrentDocument ? databaseDocument
                    : null, getDocument(), this.configuration);
        } catch (Exception e) {
            throw new SAXException("Failed to save document", e);
        }
    }

    private XWikiDocument getDatabaseDocument() throws ComponentLookupException, XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWikiDocument document = getDocument();

        XWikiDocument existingDocument = context.getWiki().getDocument(document.getDocumentReference(), context);

        if (!document.getLocale().equals(Locale.ROOT)) {
            Locale defaultLocale = existingDocument.getDefaultLocale();
            XWikiDocument translatedDocument = existingDocument.getTranslatedDocument(document.getLocale(), context);

            if (translatedDocument == existingDocument) {
                translatedDocument = new XWikiDocument(document.getDocumentReference());
                translatedDocument.setDefaultLocale(defaultLocale);
                translatedDocument.setTranslation(1);
                translatedDocument.setLocale(document.getLocale());
            }

            existingDocument = translatedDocument;
        }

        if (this.hasCurrentDocument == null) {
            this.hasCurrentDocument = !existingDocument.isNew();
        }

        return existingDocument;
    }

    private XWikiDocument getPreviousDocument() throws NotADocumentException, ParserConfigurationException,
        SAXException, IOException
    {
        XWikiDocument previousDocument = null;

        XWikiDocument document = getDocument();
        XarEntry xarEntry = new XarEntry(document.getSpace(), document.getName(), document.getLocale());
        XarFile previousXarFile = this.configuration.getPreviousPages().get(xarEntry);
        if (previousXarFile != null) {
            DocumentHandler documentHandler = new DocumentHandler(getComponentManager(), document.getWikiName());

            XarEntry realEntry = previousXarFile.getEntry(xarEntry.getDocumentReference(), xarEntry.getLocale());
            if (realEntry != null) {
                this.packager.parseDocument(previousXarFile.getInputStream(realEntry), documentHandler);

                previousDocument = documentHandler.getDocument();
            }
        }

        return previousDocument;
    }

    private void saveAttachment(XWikiAttachment attachment, String comment) throws SAXException
    {
        try {
            XWikiContext context = getXWikiContext();

            XWikiDocument document = getDocument();

            // Set proper author
            DocumentReference userReference = this.configuration.getUserReference();
            if (userReference != null) {
                document.setAuthorReference(userReference);
                attachment.setAuthor(getUserString());
            }

            XWikiDocument dbDocument = getDatabaseDocument();

            XWikiAttachment dbAttachment = dbDocument.getAttachment(attachment.getFilename());

            if (dbAttachment == null) {
                attachment.setDoc(dbDocument);
                dbDocument.getAttachmentList().add(attachment);
            } else {
                dbAttachment.setContent(attachment.getContentInputStream(context));
                dbAttachment.setFilename(attachment.getFilename());
                dbAttachment.setAuthor(attachment.getAuthor());
            }

            saveDocumentSetContextUser(dbDocument, comment, true, context);

            // reset content since it could consume lots of memory and it's not used in diff for now
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
