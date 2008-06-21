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

import java.util.Date;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.LogicalTime;
import org.xwiki.platform.patchservice.api.Patch;
import org.xwiki.platform.patchservice.api.PatchId;
import org.xwiki.platform.patchservice.api.RWPatchId;
import org.xwiki.platform.patchservice.api.XmlSerializable;

import com.xpn.xwiki.XWikiException;

/**
 * Default implementation for {@link RWPatchId}.
 * 
 * @see org.xwiki.platform.patchservice.api.RWPatchId
 * @see org.xwiki.platform.patchservice.api.PatchId
 * @version $Id$
 * @since XWikiPlatform 1.3
 */
public class PatchIdImpl implements PatchId, RWPatchId, XmlSerializable
{
    /** The name of the XML element corresponding to patch IDs. */
    public static final String NODE_NAME = "id";

    /**
     * The name of the XML attribute holding the document ID.
     * 
     * @see PatchId#getDocumentId()
     */
    public static final String DOCID_ATTRIBUTE_NAME = "doc";

    /**
     * The name of the XML attribute holding the host ID.
     * 
     * @see PatchId#getHostId()
     */
    public static final String HOSTID_ATTRIBUTE_NAME = "host";

    /**
     * The name of the XML attribute holding the patch creation time.
     * 
     * @see PatchId#getTime()
     */
    public static final String TIME_ATTRIBUTE_NAME = "time";

    /**
     * The document ID.
     * 
     * @see PatchId#getDocumentId()
     */
    private String documentId;

    /**
     * The host ID.
     * 
     * @see PatchId#getHostId()
     */
    private String hostId;

    /**
     * The patch creation time on the originating server.
     * 
     * @see PatchId#getTime()
     */
    private Date time;

    /**
     * The patch creation logical time.
     * 
     * @see PatchId#getLogicalTime()
     */
    private LogicalTime logicalTime;

    /**
     * {@inheritDoc}
     */
    public void setDocumentId(String documentId)
    {
        this.documentId = documentId;
    }

    /**
     * {@inheritDoc}
     */
    public void setHostId(String hostId)
    {
        this.hostId = hostId;
    }

    /**
     * {@inheritDoc}
     */
    public void setLogicalTime(LogicalTime logicalTime)
    {
        this.logicalTime = logicalTime;
    }

    /**
     * {@inheritDoc}
     */
    public void setTime(Date time)
    {
        this.time = time;
    }

    /**
     * {@inheritDoc}
     */
    public String getDocumentId()
    {
        return this.documentId;
    }

    /**
     * {@inheritDoc}
     */
    public String getHostId()
    {
        return this.hostId;
    }

    /**
     * {@inheritDoc}
     */
    public LogicalTime getLogicalTime()
    {
        return this.logicalTime;
    }

    /**
     * {@inheritDoc}
     */
    public Date getTime()
    {
        return this.time;
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = doc.createElement(NODE_NAME);
        xmlNode.setAttribute(DOCID_ATTRIBUTE_NAME, this.documentId);
        xmlNode.setAttribute(HOSTID_ATTRIBUTE_NAME, this.hostId);
        try {
            xmlNode.setAttribute(TIME_ATTRIBUTE_NAME, Patch.DATE_FORMAT.format(this.time));
        } catch (Exception e) {
        }
        try {
            xmlNode.appendChild(this.logicalTime.toXml(doc));
        } catch (DOMException e) {
        }
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        try {
            this.documentId = e.getAttribute(DOCID_ATTRIBUTE_NAME);
            this.hostId = e.getAttribute(HOSTID_ATTRIBUTE_NAME);
            String timeText = e.getAttribute(TIME_ATTRIBUTE_NAME);
            this.time = Patch.DATE_FORMAT.parse(timeText);
            this.logicalTime = new LogicalTimeImpl((Element) e.getElementsByTagName(LogicalTimeImpl.NODE_NAME).item(0));
        } catch (Exception ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_DOC_XML_PARSING,
                "Invalid Patch XML", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other)
    {
        try {
            PatchIdImpl that = (PatchIdImpl) other;
            return this.hostId.equals(that.hostId) && this.documentId.equals(that.documentId)
                && this.time.equals(that.time) && this.logicalTime.equals(that.logicalTime);
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
        return new HashCodeBuilder(37, 5).append(this.hostId).append(this.documentId).append(this.time).append(
            this.logicalTime).toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "[" + this.documentId + "@" + this.hostId + " at " + this.time + " (" + this.logicalTime + ")]";
    }
}
