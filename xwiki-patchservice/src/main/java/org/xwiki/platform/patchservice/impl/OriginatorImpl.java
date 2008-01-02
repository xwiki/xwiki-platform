package org.xwiki.platform.patchservice.impl;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.RWOriginator;

import com.xpn.xwiki.XWikiException;

public class OriginatorImpl implements RWOriginator
{
    public static final String NODE_NAME = "originator";
    public static final String AUTHOR_ATTRIBUTE_NAME = "author";
    public static final String HOST_ATTRIBUTE_NAME = "host";
    public static final String WIKI_ATTRIBUTE_NAME = "wiki";
    private String author;
    private String hostId;
    private String wikiId;
    public void setAuthor(String author)
    {
        this.author = author;
    }

    public void setHostId(String hostId)
    {
        this.hostId = hostId;
    }

    public void setWikiId(String wikiId)
    {
        this.wikiId = wikiId;
    }

    public void fromXml(Element e) throws XWikiException
    {
        this.author = e.getAttribute(AUTHOR_ATTRIBUTE_NAME);
        this.hostId = e.getAttribute(HOST_ATTRIBUTE_NAME);
        this.wikiId = e.getAttribute(WIKI_ATTRIBUTE_NAME);
    }

    public String getAuthor()
    {
        return this.author;
    }

    public String getHostId()
    {
        return this.hostId;
    }

    public String getWikiId()
    {
        return this.wikiId;
    }

    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = doc.createElement(NODE_NAME);
        xmlNode.setAttribute(AUTHOR_ATTRIBUTE_NAME, this.author);
        xmlNode.setAttribute(HOST_ATTRIBUTE_NAME, this.hostId);
        xmlNode.setAttribute(WIKI_ATTRIBUTE_NAME, this.wikiId);
        return xmlNode;
    }
}
