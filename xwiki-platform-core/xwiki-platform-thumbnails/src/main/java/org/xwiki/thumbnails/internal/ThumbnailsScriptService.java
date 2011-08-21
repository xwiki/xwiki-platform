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
package org.xwiki.thumbnails.internal;

import java.util.Map;

import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.thumbnails.NoSuchImageException;
import org.xwiki.thumbnails.ThumbnailURLProvider;

/**
 * Script service to work with thumbnails of XWiki image attachments from scripts.
 * 
 * @version $Id$
 * @since 3.2-M3
 */
@Component("thumbnails")
public class ThumbnailsScriptService implements ScriptService
{
    /**
     * The URL provider used to provide thumbnails URLs.
     */
    @Inject
    private ThumbnailURLProvider urlProvider;

    /**
     * Gets the URL for a thumbnail image which reference is stored as an object property value.
     * 
     * @param reference the reference of the object property to look for the attachment reference at
     * @return the URL of the thumbnail for the image specified at the object property
     * @see ThumbnailURLProvider#getURL(ObjectPropertyReference, Map)
     */
    public String getThumbnailURL(ObjectPropertyReference reference)
    {
        try {
            return this.urlProvider.getURL(reference);
        } catch (NoSuchImageException e) {
            return null;
        }
    }

    /**
     * Gets the URL for a thumbnail image which reference is stored as an object property value.
     * 
     * @param reference the reference of the object property to look for the attachment reference at
     * @param extraParameters extra query parameters to append to the URL
     * @return the URL of the thumbnail for the image specified at the object property
     * @see ThumbnailURLProvider#getURL(ObjectPropertyReference, Map)
     */
    public String getThumbnailURL(ObjectPropertyReference reference, Map<String, Object> extraParameters)
    {
        try {
            return this.urlProvider.getURL(reference, extraParameters);
        } catch (NoSuchImageException e) {
            return null;
        }
    }

    /**
     * Gets the thumbnail URL for an image passed as attachment reference.
     * 
     * @param reference the reference of the image
     * @return the URL of the thumbnail for the image referenced
     * @see ThumbnailURLProvider#getURL(AttachmentReference, Map)
     */
    public String getThumbnailURL(AttachmentReference reference)
    {
        try {
            return this.urlProvider.getURL(reference);
        } catch (NoSuchImageException e) {
            return null;
        }
    }

    /**
     * Gets the thumbnail URL for an image passed as attachment reference.
     * 
     * @param reference the reference of the image
     * @param extraParameters extra query parameters to append to the URL
     * @return the URL of the thumbnail for the image referenced
     * @see ThumbnailURLProvider#getURL(AttachmentReference, Map)
     */
    public String getThumbnailURL(AttachmentReference reference, Map<String, Object> extraParameters)
    {
        try {
            return this.urlProvider.getURL(reference, extraParameters);
        } catch (NoSuchImageException e) {
            return null;
        }
    }
}
