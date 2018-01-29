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

import org.xwiki.store.internal.FileSystemStoreUtils;

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

    /**
     * {@inheritDoc}
     * <p>
     * Get a File for loading or storing this attachment's content. This file is derived from the name of the document
     * which the attachment resides in and the attachment filename. The file will be placed in the storage area in a
     * directory structure called
     * {@code <storage dir>/<wiki>/<space>/<document name>/~this/attachments/<attachment file name>/} So an attachment
     * called file.txt in a document called Sandbox.Test in a the main wiki ("xwiki") would go in the following file:
     * <br/>
     * {@code <storage dir>/xwiki/Sandbox/Test/~this/attachments/file.txt/file.txt}
     * </p>
     *
     * @see AttachmentFileProvider#getAttachmentContentFile()
     */
    @Override
    public File getAttachmentContentFile()
    {
        // storage/xwiki/Main/WebHome/~this/attachments/some.file/some.file
        return new File(this.attachmentDir, FileSystemStoreUtils.encode(this.attachmentFileName, false));
    }

    /**
     * {@inheritDoc}
     * <p>
     * This will be a file named ~METADATA.xml which will reside in the attachment directory.
     * </p>
     *
     * @see AttachmentFileProvider#getAttachmentVersioningMetaFile()
     */
    @Override
    public File getAttachmentVersioningMetaFile()
    {
        return new File(this.attachmentDir, ATTACH_ARCHIVE_META_FILENAME);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Get a file corresponding to this version of this attachment. If the file has one or more dots ('.') in it then
     * the version number is inserted before the last dot. Otherwise it is appended to the end. Version numbers always
     * have "~v" prepended to prevent collision.
     * <ul>
     * <li>version 1.1 of an attachment called file.txt will be stored as file~v1.1.txt</li>
     * <li>version 1.2 of an attachment called noExtension will be stored as noExtension~v1.2</li>
     * </ul>
     *
     * @see AttachmentFileProvider#getAttachmentVersioningMetaFile()
     */
    @Override
    public File getAttachmentVersionContentFile(final String versionName)
    {
        return new File(this.attachmentDir,
            GenericFileUtils.getVersionedFilename(this.attachmentFileName, versionName));
    }
}
