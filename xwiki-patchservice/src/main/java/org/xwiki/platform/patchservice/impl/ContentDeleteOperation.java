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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.Position;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class ContentDeleteOperation extends AbstractOperationImpl implements RWOperation
{
    private Position position;

    private String removedContent;

    static {
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_CONTENT_DELETE, ContentDeleteOperation.class);
    }

    public ContentDeleteOperation()
    {
        this.setType(Operation.TYPE_CONTENT_DELETE);
    }

    /**
     * {@inheritDoc}
     */
    public void apply(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        try {
            String content = doc.getContent();
            if (!this.position.checkPosition(content)
                || !this.position.getTextAfterPosition(content).startsWith(this.removedContent))
            {
                throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                    "Patch does not fit. Expected [" + StringUtils.abbreviate(this.removedContent, 20)
                        + "], but found [" + StringUtils.abbreviate(this.position.getTextAfterPosition(content), 20)
                        + "]");
            }
            content =
                this.position.getTextBeforePosition(content)
                    + this.position.getTextAfterPosition(content).substring(this.removedContent.length());
            doc.setContent(content);
        } catch (StringIndexOutOfBoundsException ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Patch cannot be applied", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete(String text, Position position)
    {
        this.removedContent = text;
        this.position = position;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        this.removedContent = getTextValue(e);
        this.position = loadPositionNode(e);
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = createOperationNode(doc);
        xmlNode.appendChild(createTextNode(this.removedContent, doc));
        xmlNode.appendChild(this.position.toXml(doc));
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other)
    {
        try {
            ContentDeleteOperation otherOperation = (ContentDeleteOperation) other;
            return otherOperation.position.equals(this.position)
                && otherOperation.removedContent.equals(this.removedContent);
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
        return new HashCodeBuilder(5, 7).append(this.position).append(this.removedContent).toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return this.getType() + ": [" + this.removedContent + "] at " + this.position;
    }
}
