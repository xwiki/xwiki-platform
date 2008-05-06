package com.xpn.xwiki.doc;

import java.util.List;

import com.xpn.xwiki.objects.BaseObject;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.doc.XWikiDocument} class.
 *
 * @version $Id: $
 */
public aspect XWikiDocumentCompatibilityAspect
{
    /**
     * @deprecated use {@link #getSpace()} instead
     */
    public String XWikiDocument.getWeb()
    {
        return getSpace();
    }

    /**
     * @deprecated use {@link #setSpace(String)} instead
     */
    public void XWikiDocument.setWeb(String web)
    {
        setSpace(web);
    }
    
    /**
     * @deprecated use setStringListValue or setDBStringListProperty
     */
    public void XWikiDocument.setListValue(String className, String fieldName, List value)
    {
        BaseObject bobject = getObject(className);
        if (bobject == null) {
            bobject = new BaseObject();
            addObject(className, bobject);
        }
        bobject.setName(getFullName());
        bobject.setClassName(className);
        bobject.setListValue(fieldName, value);
        setContentDirty(true);
    }
}
