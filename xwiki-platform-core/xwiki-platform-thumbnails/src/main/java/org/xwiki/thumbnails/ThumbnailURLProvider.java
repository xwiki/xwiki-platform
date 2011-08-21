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
package org.xwiki.thumbnails;

import java.util.Map;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.AttachmentReference;

/**
 * A Thumbnail URL Provider provides full image URL of a thumbnail of XWiki image attachments.
 * 
 * @version $Id$
 * @since 3.2-M3
 */
@ComponentRole
public interface ThumbnailURLProvider
{
    /**
     * Gets the URL for a thumbnail image which reference is stored as an object property value.
     * 
     * @param reference the reference of the object property to look for the attachment reference at
     * @return the URL of the thumbnail for the image specified at the object property
     * @throws NoSuchImageException when the image does not exist
     */
    String getURL(ObjectPropertyReference reference) throws NoSuchImageException;

    /**
     * Gets the URL for a thumbnail image which reference is stored as an object property value.
     * 
     * @param reference the reference of the object property to look for the attachment reference at
     * @param extraParameters extra query parameters to append to the URL
     * @return the URL of the thumbnail for the image specified at the object property
     * @throws NoSuchImageException when the image does not exist
     */
    String getURL(ObjectPropertyReference reference, Map<String, Object> extraParameters) throws NoSuchImageException;

    /**
     * Gets the URL for a thumbnail image for the passed attachment reference.
     * 
     * @param reference the image reference
     * @return the URL of the thumbnail for the image specified at the object property
     * @throws NoSuchImageException when the image does not exist
     */
    String getURL(AttachmentReference reference) throws NoSuchImageException;

    /**
     * Gets the URL for a thumbnail image for the passed attachment reference.
     * 
     * @param reference the image reference
     * @param extraParameters extra query parameters to append to the URL
     * @return the URL of the thumbnail for the image specified at the object property
     * @throws NoSuchImageException when the image does not exist
     */
    String getURL(AttachmentReference reference, Map<String, Object> extraParameters) throws NoSuchImageException;

}
