/*
 * Copyright 2007, XpertNet SARL, and individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package com.xpn.xwiki.doc.rcs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.suigeneris.jrcs.util.ToString;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Contains differences between revisions.
 * One field (diff for xml or full xml) for now.
 * Created for easy migrate to future XWikiPatch system
 * @version $Id: $
 * @since 1.2M1
 */
public class XWikiPatch
{
    /** string serialization for patch. */
    private String content;
    /** is content a difference, or full version. */
    private boolean isDiff;
    /** Default constructor, need for hibernate. */
    public XWikiPatch() { }
    /**
     * @param content - patch content
     * @param isDiff  - is patch a difference or full version
     */
    public XWikiPatch(String content, boolean isDiff)
    {
        setContent(content);
        setDiff(isDiff);
    }
    /**
     * @return string serialization for patch 
     */
    public String getContent()
    {
        return content;
    }
    /**
     * @param content - string serialization for patch
     */
    public void setContent(String content)
    {
        this.content = content;
    }
    /**
     * @return is content a difference, or full version
     */
    public boolean isDiff()
    {
        return isDiff;
    }
    /**
     * @param isDiff - is content a difference, or full version
     */
    public void setDiff(boolean isDiff)
    {
        this.isDiff = isDiff;
    }
    /**
     * Create full patch for document.
     * @param doc - document to patch
     * @param context - used for serialization document to xml
     * @return self
     * @throws XWikiException if any error
     */
    public XWikiPatch setFullVersion(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        setDiff(false);
        setContent(doc.toXML(context));
        return this;
    }
    /**
     * Create difference patch for document curdoc, using origdoc as previous version.
     * @param curDoc  - current document
     * @param origDoc - original document
     * @param context - used for serialization documents to xml
     * @return self
     * @throws XWikiException if any error
     */
    public XWikiPatch setDiffVersion(XWikiDocument curDoc, XWikiDocument origDoc,
        XWikiContext context) throws XWikiException
    {
        return setDiffVersion(curDoc, origDoc.toXML(context), context);
    }
    /**
     * Create difference patch for document curdoc, using origdoc as previous version.
     * @param curDoc     - current document
     * @param origDocXml - xml of original document
     * @param context    - used for serialization document to xml
     * @return self
     * @throws XWikiException if any error
     */
    public XWikiPatch setDiffVersion(XWikiDocument curDoc, String origDocXml,
        XWikiContext context) throws XWikiException
    {
        setDiff(true);
        try {
            setContent(XWikiPatchUtils.getDiff(curDoc.toXML(context), origDocXml));
        } catch (Exception e) {
            Object[] args = {curDoc.getFullName()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_DIFF, 
                XWikiException.ERROR_XWIKI_DIFF_XML_ERROR, 
                "Failed to create diff for doc {}", e, args);
        }
        return this;
    }
    /**
     * patch text.
     * @param origText - text to patch
     * @throws XWikiException if exception while patching
     */
    public void patch(List origText) throws XWikiException
    {
        if (!isDiff()) {
            origText.clear();
            origText.addAll(new ArrayList(Arrays.asList(ToString.stringToArray(getContent()))));
        } else {
            try {
                XWikiPatchUtils.patch(origText, getContent());
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_DIFF, 
                    XWikiException.ERROR_XWIKI_DIFF_XML_ERROR, "Exception while patching", e);
            }
        }
    }
}
