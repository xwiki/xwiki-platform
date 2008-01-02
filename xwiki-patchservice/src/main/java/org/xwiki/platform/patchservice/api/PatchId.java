package org.xwiki.platform.patchservice.api;

import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.xpn.xwiki.XWikiException;

public interface PatchId
{
    String getHostId();

    LogicalTime getLogicalTime();

    Date getTime();

    String getDocumentId();

    Element toXml(Document doc) throws XWikiException;

    void fromXml(Element e) throws XWikiException;
}
