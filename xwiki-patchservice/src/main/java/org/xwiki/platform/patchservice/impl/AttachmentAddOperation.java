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
 *
 */
package org.xwiki.platform.patchservice.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Formatter;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

public class AttachmentAddOperation extends AbstractOperationImpl implements RWOperation
{
    private byte[] data;

    private String filename;

    private String author;

    static {
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_ATTACHMENT_ADD, AttachmentAddOperation.class);
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_ATTACHMENT_SET, AttachmentAddOperation.class);
    }

    public AttachmentAddOperation()
    {
        this.setType(Operation.TYPE_ATTACHMENT_ADD);
    }

    /**
     * {@inheritDoc}
     */
    public void apply(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        try {
            XWikiAttachment attachment = doc.getAttachment(this.filename);
            if (this.getType().equals(TYPE_ATTACHMENT_ADD)) {
                if (attachment != null) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                        new Formatter().format("Cannot apply patch: Attachment already exists: [%s]",
                            new Object[] {this.filename}).toString());
                }
                attachment = new XWikiAttachment();
            }
            doc.getAttachmentList().add(attachment);
            attachment.setContent(this.data);
            attachment.setFilename(this.filename);
            attachment.setAuthor(this.author);
            attachment.setDoc(doc);
        } catch (Exception ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Invalid attachment: " + this.filename);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAttachment(InputStream is, String filename, String author)
    {
        this.setType(TYPE_ATTACHMENT_ADD);
        try {
            this.data = org.apache.commons.io.IOUtils.toByteArray(is);
            this.filename = filename;
            this.author = author;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean setAttachment(InputStream is, String filename, String author)
    {
        if (!this.addAttachment(is, filename, author)) {
            return false;
        }
        this.setType(TYPE_ATTACHMENT_SET);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        this.setType(getOperationType(e));
        this.data = getAttachmentContent(e);
        this.filename = getAttachmentFilename(e);
        this.author = getAttachmentAuthor(e);
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = createOperationNode(doc);
        xmlNode.appendChild(createAttachmentNode(this.data, this.filename, this.author, doc));
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other)
    {
        try {
            AttachmentAddOperation that = (AttachmentAddOperation) other;
            return this.getType().equals(that.getType()) && this.filename.equals(that.filename)
                && this.author.equals(that.author) && ArrayUtils.isEquals(this.data, that.data);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(37, 41).append(this.getType()).append(this.filename).append(this.author).append(
            this.data).toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return this.getType() + ": [" + this.filename + "] by [" + this.author + "]";
    }
}
