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

import org.dom4j.io.SAXContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public class DocumentImporterHandler extends AbstractHandler
{
    private boolean needSave = true;

    /**
     * Avoid create a new SAXContentHandler for each object/class when the same can be used for all.
     */
    public SAXContentHandler domBuilder = new SAXContentHandler();

    public DocumentImporterHandler(ComponentManager componentManager, String wiki)
    {
        super(componentManager);

        setCurrentBean(new XWikiDocument(new DocumentReference(wiki, "XWiki", "Page")));

        // Default syntax in a XAR is xwiki/1.0
        getDocument().setSyntax(Syntax.XWIKI_1_0);

        // skip useless known elements
        this.skippedElements.add("version");
        this.skippedElements.add("minorEdit");
        this.skippedElements.add("comment");
    }

    public XWikiDocument getDocument()
    {
        return (XWikiDocument) getCurrentBean();
    }

    public void setWiki(String wiki)
    {
        getDocument().setDatabase(wiki);
    }

    private void saveDocument(String comment) throws SAXException
    {
        try {
            XWikiContext context = getXWikiContext();

            XWikiDocument document = getDocument();
            XWikiDocument dbDocument = getDatabaseDocument();
            // TODO: get previous document

            // TODO: diff previous and new document
            // TODO: if there is differences
            // TODO: ..apply diff to db document
        } catch (Exception e) {
            throw new SAXException("Failed to save document", e);
        }

        this.needSave = false;
    }

    private XWikiDocument getDatabaseDocument() throws ComponentLookupException, XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWikiDocument document = getDocument();

        XWikiDocument existingDocument = context.getWiki().getDocument(document.getDocumentReference(), context);
        existingDocument = existingDocument.getTranslatedDocument(document.getLanguage(), context);

        return existingDocument;
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
    protected void currentBeanModified()
    {
        this.needSave = true;
    }

    @Override
    public void startElementInternal(String uri, String localName, String qName, Attributes attributes)
        throws SAXException
    {
        if (qName.equals("attachment")) {
            setCurrentHandler(new AttachmentHandler(getComponentManager()));
        } else if (qName.equals("class") || qName.equals("object")) {
            this.domBuilder.startDocument();
            setCurrentHandler(this.domBuilder);
        } else {
            super.startElementInternal(uri, localName, qName, attributes);
        }
    }

    @Override
    public void endElementInternal(String uri, String localName, String qName) throws SAXException
    {
        if (qName.equals("attachment")) {
            AttachmentHandler handler = (AttachmentHandler) getCurrentHandler();

            saveAttachment(handler.getAttachment(), "Import: add attachment");
        } else if (qName.equals("object")) {
            try {
                BaseObject baseObject = new BaseObject();
                baseObject.fromXML(this.domBuilder.getDocument().getRootElement());
                getDocument().setXObject(baseObject.getNumber(), baseObject);
            } catch (XWikiException e) {
                throw new SAXException("Failed to parse object", e);
            }

            this.needSave = true;
        } else if (qName.equals("class")) {
            try {
                getDocument().getXClass().fromXML(this.domBuilder.getDocument().getRootElement());
            } catch (XWikiException e) {
                throw new SAXException("Failed to parse object", e);
            }

            this.needSave = true;
        } else {
            super.endElementInternal(uri, localName, qName);
        }
    }

    @Override
    protected void endHandlerElement(String uri, String localName, String qName) throws SAXException
    {
        if (this.needSave) {
            saveDocument(getDocument().getAttachmentList().isEmpty() ? "Import" : "Import: final save");
        }
    }
}
