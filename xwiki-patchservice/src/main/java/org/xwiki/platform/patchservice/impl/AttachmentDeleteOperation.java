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

import java.util.Formatter;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

public class AttachmentDeleteOperation extends AbstractOperationImpl implements RWOperation
{
    private String filename;

    static {
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_ATTACHMENT_DELETE, AttachmentDeleteOperation.class);
    }

    public AttachmentDeleteOperation()
    {
        this.setType(Operation.TYPE_ATTACHMENT_DELETE);
    }

    /**
     * {@inheritDoc}
     */
    public void apply(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        try {
            XWikiAttachment attachment = doc.getAttachment(this.filename);
            if (attachment == null) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                    new Formatter().format("Cannot apply patch: Attachment does not exists: [%s]",
                        new Object[] {this.filename}).toString());
            }
            doc.deleteAttachment(attachment, context);
        } catch (Exception ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Invalid attachment: " + this.filename);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteAttachment(String filename)
    {
        this.filename = filename;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        this.filename = getAttachmentFilename(e);
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = createOperationNode(doc);
        xmlNode.appendChild(createAttachmentNode(this.filename, doc));
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other)
    {
        try {
            AttachmentDeleteOperation that = (AttachmentDeleteOperation) other;
            return this.getType().equals(that.getType()) && this.filename.equals(that.filename);
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
        return new HashCodeBuilder(37, 41).append(this.getType()).append(this.filename).toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return this.getType() + ": [" + this.filename + "]";
    }
}
