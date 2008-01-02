package org.xwiki.platform.patchservice.api;

import org.w3c.dom.Element;

import com.xpn.xwiki.XWikiException;

public interface OperationFactory
{
    RWOperation newOperation(String type) throws XWikiException;

    Operation loadOperation(Element e) throws XWikiException;
}
