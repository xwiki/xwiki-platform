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
package org.xwiki.gwt.wysiwyg.client.plugin.alfresco;

import org.xwiki.gwt.wysiwyg.client.wiki.Entity;

/**
 * An Alfresco entity (e.g. a space or a document).
 * 
 * @version $Id$
 */
public class AlfrescoEntity extends Entity
{
    /**
     * The name of this entity.
     */
    private String name;

    /**
     * The media type of this entity.
     */
    private String mediaType;

    /**
     * The relative path to this entity.
     */
    private String path;

    /**
     * The URL that can be used to preview this entity.
     */
    private String previewURL;

    /**
     * @return the name of this entity
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the name of this entity.
     * 
     * @param name the new entity name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the media type of this entity
     */
    public String getMediaType()
    {
        return mediaType;
    }

    /**
     * Sets the media type of this entity.
     * 
     * @param mediaType the new media type
     */
    public void setMediaType(String mediaType)
    {
        this.mediaType = mediaType;
    }

    /**
     * @return the relative path to this entity
     */
    public String getPath()
    {
        return path;
    }

    /**
     * Sets the relative path to this entity.
     * 
     * @param path the new relative path
     */
    public void setPath(String path)
    {
        this.path = path;
    }

    /**
     * @return the preview URL
     */
    public String getPreviewURL()
    {
        return previewURL;
    }

    /**
     * Sets the preview URL.
     * 
     * @param previewURL the new preview URL
     */
    public void setPreviewURL(String previewURL)
    {
        this.previewURL = previewURL;
    }
}
