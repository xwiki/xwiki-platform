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

import java.util.Map;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.rendering.listener.reference.ResourceReference;

/**
 * Bridge between the Rendering module and a Wiki Model. Contains wiki APIs required by Rendering classes such as
 * Renderers. For example the XHTML Link Renderer needs to know if a wiki document exists in order to know how to
 * generate the HTML (in order to display a question mark for non existing documents) and it also needs to get the URL
 * pointing the wiki document.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@ComponentRole
public interface WikiModel
{
    /**
     * @param attachmentReference the reference to the attachment
     * @return the URL to the attachment
     * @since 2.5RC1
     */
    String getAttachmentURL(ResourceReference attachmentReference);

    /**
     * Generate image specific URL. The difference with {@link #getAttachmentURL(org.xwiki.rendering.listener.reference.ResourceReference)} is that in some
     * implementation we want to make a distinction between displayed image and a simple link targeting an attachment
     * file.
     *
     * @param attachmentReference the reference to the attachment
     * @param parameters the custom parameters
     * @return the URL to the image
     * @since 2.5RC1
     */
    String getImageURL(ResourceReference attachmentReference, Map<String, String> parameters);

    /**
     * @param documentReference the reference to the document
     * @return true if the document exists and can be viewed or false otherwise
     */
    boolean isDocumentAvailable(ResourceReference documentReference);

    /**
     * @param documentReference the reference to the document
     * @return the URL to view the specified wiki document
     */
    String getDocumentViewURL(ResourceReference documentReference);

    /**
     * @param documentReference the reference to the document
     * @return the URL to edit the specified wiki document
     */
    String getDocumentEditURL(ResourceReference documentReference);
}
