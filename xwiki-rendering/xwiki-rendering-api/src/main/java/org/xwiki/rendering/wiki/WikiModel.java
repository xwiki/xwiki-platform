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
package org.xwiki.rendering.wiki;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Bridge between the Rendering module and a Wiki Model. Contains wiki APIs required by Rendering classes such as 
 * Renderers. For example the XHTML Link Renderer needs to know if a wiki document exists in order to know how to 
 * generate the HTML (in order to display a question mark for non existing documents) and it also needs to get the
 * URL pointing the wiki document. 
 * 
 * @version $Id: $
 * @since 2.0M1
 */
@ComponentRole
public interface WikiModel
{
    /**
     * @param documentName the document name of the document containing the attachment. The syntax used depends on 
     *        the underlying wiki system used. For example for XWiki a valid documentName would be 
     *        {@code wiki:Space.Page}
     * @param attachmentName the name of the attachment in the passed document wikip page
     * @return the URL to the attachment
     */
    String getAttachmentURL(String documentName, String attachmentName);

    /**
     * @param documentName the document name as a String. The syntax used depends on 
     *        the underlying wiki system used. For example for XWiki a valid documentName would be 
     *        {@code wiki:Space.Page}
     * @return true if the document exists and can be viewed or false otherwise
     */
    boolean isDocumentAvailable(String documentName);

    /**
     * @param documentName the document name as a String. The syntax used depends on 
     *        the underlying wiki system used. For example for XWiki a valid documentName would be 
     *        {@code wiki:Space.Page}
     * @param anchor an anchor pointing to some place inside the document or null
     * @param queryString a query string specifying some parameters or null
     * @return the URL to view the specified wiki document
     */
    String getDocumentViewURL(String documentName, String anchor, String queryString);
    
    /**
     * @param documentName the document name as a String. The syntax used depends on 
     *        the underlying wiki system used. For example for XWiki a valid documentName would be 
     *        {@code wiki:Space.Page}
     * @param anchor an anchor pointing to some place inside the document or null
     * @param queryString a query string specifying some parameters or null
     * @return the URL to edit the specified wiki document
     */
    String getDocumentEditURL(String documentName, String anchor, String queryString);
}
