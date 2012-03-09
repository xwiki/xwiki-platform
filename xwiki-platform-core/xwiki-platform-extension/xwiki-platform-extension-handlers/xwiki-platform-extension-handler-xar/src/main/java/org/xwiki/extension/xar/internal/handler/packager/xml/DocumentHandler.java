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
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * 
 * @version $Id$
 * @since 4.0M1
 */
public class DocumentHandler extends AbstractHandler
{
    /**
     * Avoid create a new SAXContentHandler for each object/class when the same can be used for all.
     */
    public SAXContentHandler domBuilder = new SAXContentHandler();

    public DocumentHandler(ComponentManager componentManager, String wiki)
    {
        super(componentManager);

        setCurrentBean(new XWikiDocument(new DocumentReference(wiki, "XWiki", "Page")));

        // Default syntax in a XAR is xwiki/1.0
        getDocument().setSyntax(Syntax.XWIKI_1_0);

        // skip useless known elements
        this.skippedElements.add("version");
        this.skippedElements.add("minorEdit");
        this.skippedElements.add("comment");
        this.skippedElements.add("creator");
        this.skippedElements.add("author");
        this.skippedElements.add("contentAuthor");
        this.skippedElements.add("creationDate");
        this.skippedElements.add("date");
        this.skippedElements.add("contentUpdateDate");
    }

    public XWikiDocument getDocument()
    {
        return (XWikiDocument) getCurrentBean();
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

    protected void endAttachment(String uri, String localName, String qName) throws SAXException
    {
        AttachmentHandler handler = (AttachmentHandler) getCurrentHandler();

        getDocument().getAttachmentList().add(handler.getAttachment());
    }

    protected void endObject(String uri, String localName, String qName) throws SAXException
    {
        try {
            BaseObject baseObject = new BaseObject();
            baseObject.fromXML(this.domBuilder.getDocument().getRootElement());
            getDocument().setXObject(baseObject.getNumber(), baseObject);
        } catch (XWikiException e) {
            throw new SAXException("Failed to parse object", e);
        }
    }

    protected void endClass(String uri, String localName, String qName) throws SAXException
    {
        try {
            getDocument().getXClass().fromXML(this.domBuilder.getDocument().getRootElement());
        } catch (XWikiException e) {
            throw new SAXException("Failed to parse object", e);
        }
    }

    @Override
    public void endElementInternal(String uri, String localName, String qName) throws SAXException
    {
        if (qName.equals("attachment")) {
            endAttachment(uri, localName, qName);
        } else if (qName.equals("object")) {
            endObject(uri, localName, qName);
        } else if (qName.equals("class")) {
            endClass(uri, localName, qName);
        } else {
            super.endElementInternal(uri, localName, qName);
        }
    }
}
