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
package org.xwiki.store.filesystem.internal;

import java.io.File;

/**
 * A means of getting files for storing information about a given attachment.
 *
 * @version $Id$
 * @since 3.0M2
 */
public class DefaultAttachmentFileProvider implements AttachmentFileProvider
{
    /**
     * This stores the attachment metadata for each revision of the attachment in XML format.
     *
     * @see #getAttachmentVersioningMetaFile()
     */
    private static final String ATTACH_ARCHIVE_META_FILENAME = "~METADATA.xml";

    /**
     * The directory where all information about this attachment resides.
     */
    private final File attachmentDir;

    /**
     * The name of the attached file.
     */
    private final String attachmentFileName;

    /**
     * The Constructor.
     *
     * @param attachmentDir a directory where all information about this attachment is stored.
     * @param fileName the name of the attachment file.
     */
    public DefaultAttachmentFileProvider(final File attachmentDir, final String fileName)
    {
        this.attachmentDir = attachmentDir;
        this.attachmentFileName = fileName;
    }

    /**
     * @return the directory where information about this attachment is stored.
     */
    protected File getAttachmentDir()
    {
        return this.attachmentDir;
    }

    /**
     * @return the name of this attachment.
     */
    protected String getAttachmentFileName()
    {
        return this.attachmentFileName;
    }

    @Override
    public File getAttachmentContentFile()
    {
        return new File(this.attachmentDir, StoreFileUtils.getStoredFilename(this.attachmentFileName, null));
    }

    @Override
    public File getAttachmentVersioningMetaFile()
    {
        return new File(this.attachmentDir, ATTACH_ARCHIVE_META_FILENAME);
    }

    @Override
    public File getAttachmentVersionContentFile(final String versionName)
    {
        return new File(this.attachmentDir, StoreFileUtils.getStoredFilename(this.attachmentFileName, versionName));
    }
}
