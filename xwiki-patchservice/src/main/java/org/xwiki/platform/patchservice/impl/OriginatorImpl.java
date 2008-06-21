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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Originator;
import org.xwiki.platform.patchservice.api.RWOriginator;
import org.xwiki.platform.patchservice.api.XmlSerializable;

import com.xpn.xwiki.XWikiException;

/**
 * Default implementation for {@link RWOriginator}.
 * 
 * @see org.xwiki.platform.patchservice.api.RWOriginator
 * @see org.xwiki.platform.patchservice.api.Originator
 * @version $Id$
 * @since XWikiPlatform 1.3
 */
public class OriginatorImpl implements Originator, RWOriginator, XmlSerializable
{
    /** The name of the XML element corresponding to originator objects. */
    public static final String NODE_NAME = "originator";

    /**
     * The name of the XML attribute holding the author wiki name.
     * 
     * @see Originator#getAuthor()
     */
    public static final String AUTHOR_ATTRIBUTE_NAME = "author";

    /**
     * The name of the XML attribute holding the host identifier.
     * 
     * @see Originator#getHostId()
     */
    public static final String HOST_ATTRIBUTE_NAME = "host";

    /**
     * The name of the XML attribute holding the wiki name.
     * 
     * @see Originator#getWikiId()
     */
    public static final String WIKI_ATTRIBUTE_NAME = "wiki";

    /**
     * The patch author.
     * 
     * @see Originator#getAuthor()
     */
    private String author;

    /**
     * The host identifier.
     * 
     * @see Originator#getHostId()
     */
    private String hostId;

    /**
     * The wiki name.
     * 
     * @see Originator#getWikiId()
     */
    private String wikiId;

    /**
     * {@inheritDoc}
     */
    public String getAuthor()
    {
        return this.author;
    }

    /**
     * {@inheritDoc}
     */
    public void setAuthor(String author)
    {
        this.author = author;
    }

    /**
     * {@inheritDoc}
     */
    public String getWikiId()
    {
        return this.wikiId;
    }

    /**
     * {@inheritDoc}
     */
    public void setWikiId(String wikiId)
    {
        this.wikiId = wikiId;
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
    public void setHostId(String hostId)
    {
        this.hostId = hostId;
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        this.author = e.getAttribute(AUTHOR_ATTRIBUTE_NAME);
        this.hostId = e.getAttribute(HOST_ATTRIBUTE_NAME);
        this.wikiId = e.getAttribute(WIKI_ATTRIBUTE_NAME);
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = doc.createElement(NODE_NAME);
        xmlNode.setAttribute(AUTHOR_ATTRIBUTE_NAME, this.author);
        xmlNode.setAttribute(HOST_ATTRIBUTE_NAME, this.hostId);
        xmlNode.setAttribute(WIKI_ATTRIBUTE_NAME, this.wikiId);
        return xmlNode;
    }
}
