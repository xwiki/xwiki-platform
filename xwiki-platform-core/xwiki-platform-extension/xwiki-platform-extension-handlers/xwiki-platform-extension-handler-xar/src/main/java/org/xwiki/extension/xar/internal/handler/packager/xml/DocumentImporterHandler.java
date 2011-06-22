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

import org.xml.sax.SAXException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.xar.internal.handler.packager.DefaultPackager;
import org.xwiki.extension.xar.internal.handler.packager.NotADocumentException;
import org.xwiki.extension.xar.internal.handler.packager.XarEntry;
import org.xwiki.extension.xar.internal.handler.packager.XarEntryMergeResult;
import org.xwiki.extension.xar.internal.handler.packager.XarFile;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;

public class DocumentImporterHandler extends DocumentHandler
{
    private XarFile previousXarFile;

    private DefaultPackager packager;

    private XarEntryMergeResult mergeResult;
    
    private MergeConfiguration mergeConfiguration;

    public DocumentImporterHandler(DefaultPackager packager, ComponentManager componentManager, String wiki)
    {
        super(componentManager, wiki);

        this.packager = packager;
    }

    public void setPreviousXarFile(XarFile previousXarFile)
    {
        this.previousXarFile = previousXarFile;
    }

    public void setMergeConfiguration(MergeConfiguration mergeConfiguration)
    {
        this.mergeConfiguration = mergeConfiguration;
    }

    public XarEntryMergeResult getMergeResult()
    {
        return mergeResult;
    }

    private void saveDocument(String comment) throws SAXException
    {
        try {
            XWikiContext context = getXWikiContext();

            XWikiDocument document = getDocument();
            XWikiDocument dbDocument = getDatabaseDocument().clone();
            XWikiDocument previousDocument = getPreviousDocument();

            if (previousDocument != null && !dbDocument.isNew()) {
                MergeResult documentMergeResult = dbDocument.merge(previousDocument, document, this.mergeConfiguration, context);
                if (documentMergeResult.isModified()) {
                    context.getWiki().saveDocument(dbDocument, comment, context);
                }
                this.mergeResult =
                    new XarEntryMergeResult(new XarEntry(dbDocument.getDocumentReference(), dbDocument.getLanguage()),
                        documentMergeResult);
            } else {
                if (!dbDocument.isNew()) {
                    document.setVersion(dbDocument.getVersion());
                }

                context.getWiki().saveDocument(document, comment, context);
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
        existingDocument = existingDocument.getTranslatedDocument(document.getLanguage(), context);

        return existingDocument;
    }

    private XWikiDocument getPreviousDocument() throws NotADocumentException, ParserConfigurationException,
        SAXException, IOException
    {
        XWikiDocument previousDocument = null;

        if (previousXarFile != null) {
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
