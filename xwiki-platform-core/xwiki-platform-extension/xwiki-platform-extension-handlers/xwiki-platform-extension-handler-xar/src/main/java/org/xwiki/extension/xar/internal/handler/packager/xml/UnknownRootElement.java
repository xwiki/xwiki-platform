package org.xwiki.extension.xar.internal.handler.packager.xml;

import org.xml.sax.SAXException;

public class UnknownRootElement extends SAXException
{
    public UnknownRootElement(String elementName)
    {
        this(elementName, null);
    }

    public UnknownRootElement(String elementName, Exception e)
    {
        super("Unknown element [" + elementName + "]", e);
    }
}
