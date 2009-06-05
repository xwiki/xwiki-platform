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
package org.xwiki.rendering.internal.wiki;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.lang.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentName;
import org.xwiki.bridge.DocumentNameSerializer;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.wiki.WikiModel;

/**
 * Implementation using the Document Access Bridge ({@link DocumentAccessBridge}).
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
public class XWikiWikiModel implements WikiModel
{
    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    @Requirement
    private DocumentNameSerializer documentNameSerializer;
    
    /**
     * {@inheritDoc}
     * @see WikiModel#getAttachmentURL(String, String)
     */
    public String getAttachmentURL(String documentName, String attachmentName)
    {
        return this.documentAccessBridge.getAttachmentURL(documentName, attachmentName);
    }

    /**
     * {@inheritDoc}
     * @see WikiModel#isDocumentAvailable(String)
     */
    public boolean isDocumentAvailable(String documentName)
    {
        return this.documentAccessBridge.exists(documentName);
    }

    /**
     * {@inheritDoc}
     * @see WikiModel#getDocumentViewURL(String, String, String)
     */
    public String getDocumentViewURL(String documentName, String anchor, String queryString)
    {
        return this.documentAccessBridge.getURL(documentName, "view", queryString, anchor);
    }

    /**
     * {@inheritDoc}
     * @see WikiModel#getDocumentEditURL(String, String, String)
     */
    public String getDocumentEditURL(String documentName, String anchor, String queryString)
    {
        // Add the parent=<current document name> parameter to the query string of the edit URL so that
        // the new document is created with the current page as its parent.
        String modifiedQueryString = queryString;
        if (StringUtils.isBlank(queryString)) {
            DocumentName name = this.documentAccessBridge.getCurrentDocumentName();
            if (name != null) {
                try {
                    // Note: we encode using UTF8 since it's the W3C recommendation.
                    // See http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars
                    // TODO: Once the xwiki-url module is usable, refactor this code to use it and remove the need to
                    // perform explicit encoding here.
                    modifiedQueryString = 
                        "parent=" + URLEncoder.encode(this.documentNameSerializer.serialize(name), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // Not supporting UTF-8 as a valid encoding for some reasons. We consider XWiki cannot work
                    // without that encoding.
                    throw new RuntimeException("Failed to URL encode [" + this.documentNameSerializer.serialize(name) 
                        + "] using UTF-8.", e);
                }
            }
        }
        
        return this.documentAccessBridge.getURL(documentName, "view", modifiedQueryString, anchor);
    }
}
