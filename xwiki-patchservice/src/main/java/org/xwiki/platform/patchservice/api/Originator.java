package org.xwiki.platform.patchservice.api;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.xpn.xwiki.XWikiException;

public interface Originator
{
    String getAuthor();

    String getHostId();

    String getWikiId();

    Element toXml(Document doc) throws XWikiException;

    void fromXml(Element e) throws XWikiException;
}
