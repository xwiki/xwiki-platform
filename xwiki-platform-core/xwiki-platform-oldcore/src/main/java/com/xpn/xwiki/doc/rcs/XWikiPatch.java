/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
 */
package com.xpn.xwiki.doc.rcs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.suigeneris.jrcs.util.ToString;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Contains differences between revisions. One field (diff for xml or full xml) for now.
 * Created for easy migration to future XWikiPatch system.
 * 
 * @version $Id$
 * @since 1.2M1
 */
public class XWikiPatch
{
    /** Logger. */
    private static final Log LOG = LogFactory.getLog(XWikiPatch.class);

    /** string serialization for patch. */
    private String content;

    /** is content a difference, or full version. */
    private boolean isDiff;

    /** Default constructor, need for hibernate. */
    public XWikiPatch()
    {
    }

    /**
     * @param content - patch content
     * @param isDiff - is patch a difference or full version
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
     * @return is content a difference. using content field to determine.
     */
    private boolean isContentDiff()
    {
        return !content.startsWith("<");
    }

    /**
     * @return is content a difference, or full version
     */
    public boolean isDiff()
    {
        if (content != null) {
            if (isDiff != isContentDiff()) {
                LOG.warn("isDiff: Patch is inconsistent. Content and diff field are contradicting");
                return isContentDiff();
            }
        }
        return isDiff;
    }

    /**
     * @param isDiff - is content a difference, or full version
     */
    public void setDiff(boolean isDiff)
    {
        if (content != null) {
            if (isDiff != isContentDiff()) {
                LOG.warn("setDiff: Patch is inconsistent. Content and diff field are contradicting");
                this.isDiff = isContentDiff();
                return;
            }
        }
        this.isDiff = isDiff;
    }

    /**
     * Store the XML export of the document as the history patch; this will be a history milestone.
     * 
     * @param version Document version to store in the history patch.
     * @param context Needed for serializing documents to xml.
     * @return Self, with the patch content set to the XML export of the document version.
     * @throws XWikiException if any error
     */
    public XWikiPatch setFullVersion(XWikiDocument version, XWikiContext context)
        throws XWikiException
    {
        return setFullVersion(version.toXML(context));
    }

    /**
     * Store the XML export of the document as the history patch; this will be a history milestone.
     * 
     * @param versionXml Document version to store in the history patch, in the XML export format.
     * @return Self, with the patch content set to the XML export of the document version.
     * @throws XWikiException if any error occurs
     */
    public XWikiPatch setFullVersion(String versionXml) throws XWikiException
    {
        setContent(versionXml);
        setDiff(false);
        return this;
    }

    /**
     * Create history patch between originalVersion and newVersion as difference on the XML export
     * of the two versions. The patch is created between newVersion and originalVersion.
     * 
     * @param originalVersion Original version of the document document.
     * @param newVersion Current version of the document.
     * @param context Needed for serializing documents to xml.
     * @return Self, with the patch content set to the generated diff between the two version.
     * @throws XWikiException if any error occurs
     */
    public XWikiPatch setDiffVersion(XWikiDocument originalVersion, XWikiDocument newVersion,
        XWikiContext context) throws XWikiException
    {
        return setDiffVersion(originalVersion.toXML(context), newVersion.toXML(context),
            newVersion.getFullName());
    }

    /**
     * Create history patch between originalVersion and newVersion as difference on the XML export
     * of the two versions. The patch is created between newVersion and originalVersion.
     * 
     * @param originalVersionXml Original version of the document document, in the XML export
     *            format.
     * @param newVersion Current version of the document.
     * @param context Needed for serializing documents to xml.
     * @return Self, with the patch content set to the generated diff between the two version.
     * @throws XWikiException if any error occurs
     */
    public XWikiPatch setDiffVersion(String originalVersionXml, XWikiDocument newVersion,
        XWikiContext context) throws XWikiException
    {
        return setDiffVersion(originalVersionXml, newVersion.toXML(context), newVersion
            .getFullName());
    }

    /**
     * Create history patch between originalVersion and newVersion as difference on the XML export
     * of the two versions. The patch is created between newVersion and originalVersion.
     * 
     * @param originalVersionXml Original version of the document document, in the XML export
     *            format.
     * @param newVersionXml Current version of the document, in the XML export format.
     * @param docName Needed for the exception report.
     * @return Self, with the patch content set to the generated diff between the two version.
     * @throws XWikiException if any error occurs
     */
    public XWikiPatch setDiffVersion(String originalVersionXml, String newVersionXml,
        String docName) throws XWikiException
    {        
        try {
            // The history keeps reversed patches, from the most recent to the previous version.
            setContent(XWikiPatchUtils.getDiff(newVersionXml, originalVersionXml));
            setDiff(true);
        } catch (Exception e) {
            Object[] args = {docName};
            throw new XWikiException(XWikiException.MODULE_XWIKI_DIFF,
                XWikiException.ERROR_XWIKI_DIFF_XML_ERROR,
                "Failed to create diff for doc {0}",
                e,
                args);
        }
        return this;
    }

    /**
     * Patch text.
     * 
     * @param origText - text to patch
     * @throws XWikiException if exception while patching
     */
    public void patch(List<String> origText) throws XWikiException
    {
        if (!isDiff()) {
            origText.clear();
            origText.addAll(
                new ArrayList<String>(Arrays.asList(ToString.stringToArray(getContent()))));
        } else {
            try {
                XWikiPatchUtils.patch(origText, getContent());
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_DIFF,
                    XWikiException.ERROR_XWIKI_DIFF_XML_ERROR,
                    "Exception while patching",
                    e);
            }
        }
    }
}
