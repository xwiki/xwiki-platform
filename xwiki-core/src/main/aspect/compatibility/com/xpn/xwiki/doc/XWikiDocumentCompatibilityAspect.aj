package com.xpn.xwiki.doc;

import java.util.List;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.DocumentSection;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.doc.XWikiDocument} class.
 *
 * @version $Id$
 */
public aspect XWikiDocumentCompatibilityAspect
{
    /**
     * @deprecated use {@link #getSpace()} instead
     */
    @Deprecated
    public String XWikiDocument.getWeb()
    {
        return getSpace();
    }

    /**
     * @deprecated use {@link #setSpace(String)} instead
     */
    @Deprecated
    public void XWikiDocument.setWeb(String web)
    {
        setSpace(web);
    }
    
    /**
     * @deprecated use setStringListValue or setDBStringListProperty
     */
    @Deprecated
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
    
    /**
     * This method to split section according to title.
     * 
     * @return the sections in the current document
     * @throws XWikiException
     * @deprecated use {@link #getSections()} instead, since 1.6M1
     */
    @Deprecated
    public List<DocumentSection> XWikiDocument.getSplitSectionsAccordingToTitle() throws XWikiException
    {
        return getSections();
    }
}
