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
package org.xwiki.wikistream.mediawiki.xml.input;

import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyName;
import org.xwiki.wikistream.xml.internal.input.XMLInputParameters;

public class MediaWikiXMLInputParameters extends XMLInputParameters
{
    private String attachmentSrcPath;

    private String attachmentExcludeDirs;

    private String defaultSpace = "Main";

    private String allowedImageExtensions;

    /**
     * @param attachmentSrcPath absolute path to MediaWiki attachments directory.
     */
    @PropertyName("Attachment Path")
    @PropertyDescription("Absolute Path to MediaWiki Attachments Directory")
    public void setAttachmentSrcPath(String attachmentSrcPath)
    {
        this.attachmentSrcPath = attachmentSrcPath;
    }

    /**
     * @param attachmentExcludeDirs the list of directories to be excluded to search for attachments in the MediaWiki
     *            Image directory.
     */
    @PropertyName("Exclude Directories")
    @PropertyDescription("Comma seperated list of all directories to be excluded to search for attachments")
    public void setAttachmentExcludeDirs(String attachmentExcludeDirs)
    {
        this.attachmentExcludeDirs = attachmentExcludeDirs;
    }

    /**
     * @return the attachmentSrcPath
     */
    public String getAttachmentSrcPath()
    {
        return this.attachmentSrcPath;
    }

    /**
     * @return the attachmentExcludeDirs
     */
    public String getAttachmentExcludeDirs()
    {
        return this.attachmentExcludeDirs;
    }

    /**
     * @return the defaultSpace
     */
    public String getDefaultSpace()
    {
        return this.defaultSpace;
    }

    /**
     * @param defaultSpace the defaultSpace to set
     */
    @PropertyName("Default Space")
    @PropertyDescription("Default Space for importing the data.XWiki uses Main as default space")
    public void setDefaultSpace(String defaultSpace)
    {
        this.defaultSpace = defaultSpace;
    }

    /**
     * @return the allowedImageExtensions
     */
    public String getAllowedImageExtensions()
    {
        return this.allowedImageExtensions;
    }

    /**
     * @param allowedImageExtensions the list of all the image formats to be considered during import
     */
    @PropertyName("Image Extensions")
    @PropertyDescription("Comma seperated list of all the image formats to be considered during import")
    public void setAllowedImageExtensions(String allowedImageExtensions)
    {
        this.allowedImageExtensions = allowedImageExtensions;
    }
}
