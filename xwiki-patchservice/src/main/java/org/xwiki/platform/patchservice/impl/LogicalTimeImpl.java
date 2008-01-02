package org.xwiki.platform.patchservice.impl;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.LogicalTime;

import com.xpn.xwiki.XWikiException;

public class LogicalTimeImpl implements LogicalTime
{
    public static final String NODE_NAME = "logicalTime";

    public LogicalTimeImpl()
    {
    }

    public LogicalTimeImpl(Element e) throws XWikiException
    {
        fromXml(e);
    }

    public void fromXml(Element e) throws XWikiException
    {
        // TODO Auto-generated method stub
    }

    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = doc.createElement(NODE_NAME);
        return xmlNode;
    }

    public int compareTo(Object other)
    {
        if (!(other instanceof LogicalTimeImpl))
        {
            return -1;
        }
        return 0;
    }

}
