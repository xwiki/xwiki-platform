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

import org.apache.commons.lang.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentName;
import org.xwiki.bridge.DocumentNameSerializer;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.wiki.WikiModel;

@Component
public class XWikiWikiModel implements WikiModel
{
    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    @Requirement
    private DocumentNameSerializer documentNameSerializer;
    
    public String getAttachmentURL(String documentName, String attachmentName)
    {
        return this.documentAccessBridge.getAttachmentURL(documentName, attachmentName);
    }

    public boolean isDocumentAvailable(String documentName)
    {
        return this.documentAccessBridge.exists(documentName);
    }

    public String getDocumentViewURL(String documentName, String anchor, String queryString)
    {
        return this.documentAccessBridge.getURL(documentName, "view", queryString, anchor);
    }

    public String getDocumentEditURL(String documentName, String anchor, String queryString)
    {
        // Add the parent=<current document name> parameter to the query string of the edit URL so that
        // the new document is created with the current page as its parent.
        String modifiedQueryString = queryString;
        if (StringUtils.isBlank(queryString)) {
            DocumentName name = this.documentAccessBridge.getCurrentDocumentName();
            if (name != null) {
                modifiedQueryString = "parent=" + this.documentNameSerializer.serialize(name);
            }
        }
        
        return this.documentAccessBridge.getURL(documentName, "view", modifiedQueryString, anchor);
    }
}
