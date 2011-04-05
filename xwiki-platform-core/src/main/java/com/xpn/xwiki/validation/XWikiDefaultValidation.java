package com.xpn.xwiki.validation;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.doc.XWikiDocument;

public class XWikiDefaultValidation implements XWikiValidationInterface {
    public boolean validateDocument(XWikiDocument doc, XWikiContext context) {
        return true;
    }
    public boolean validateObject(BaseObject object, XWikiContext context) {
        return true;
    }
}
