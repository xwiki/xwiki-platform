package org.xwiki.platform.patchservice.impl;

import java.io.Serializable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.LogicalTime;

import com.xpn.xwiki.XWikiException;

/**
 * Default implementation for {@link LogicalTime}.
 * 
 * @see org.xwiki.platform.patchservice.api.LogicalTime
 * @version $Id$
 * @since XWikiPlatform 1.3
 */
public class LogicalTimeImpl implements LogicalTime, Serializable
{
    /** The name of the XML element corresponding to logical time objects. */
    public static final String NODE_NAME = "logicalTime";

    public LogicalTimeImpl()
    {
    }

    public LogicalTimeImpl(Element e) throws XWikiException
    {
        fromXml(e);
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = doc.createElement(NODE_NAME);
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(LogicalTime other)
    {
        if (!(other instanceof LogicalTimeImpl)) {
            return -1;
        }
        return 0;
    }
}
