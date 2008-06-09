package com.xpn.xwiki.validation;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.doc.XWikiDocument;

public interface XWikiValidationInterface {
    public boolean validateDocument(XWikiDocument doc, XWikiContext context);
    public boolean validateObject(BaseObject object, XWikiContext context);
}
