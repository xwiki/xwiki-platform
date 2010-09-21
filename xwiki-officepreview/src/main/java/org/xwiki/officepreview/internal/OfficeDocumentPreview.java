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
package org.xwiki.officepreview.internal;

import java.io.File;
import java.util.Set;

import org.xwiki.cache.DisposableCacheValue;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.rendering.block.XDOM;

/**
 * Used for holding all the information belonging to an office attachment preview.
 * 
 * @since 2.5M2
 * @version $Id$
 */
public class OfficeDocumentPreview implements DisposableCacheValue
{
    /**
     * Reference to the attachment to which this preview belongs.
     */
    private AttachmentReference attachmentReference;

    /**
     * Specific version of the attachment to which this preview corresponds.
     */
    private String version;

    /**
     * {@link XDOM} holding the preview document syntax.
     */
    private XDOM xdom;

    /**
     * Temporary files used by this preview.
     */
    private Set<File> temporaryFiles;

    /**
     * Creates a new {@link OfficeDocumentPreview} instance.
     * 
     * @param attachmentReference reference to the attachment to which this preview belongs.
     * @param version version of the attachment to which this preview corresponds.
     * @param xdom {@link XDOM} holding the preview document syntax.
     * @param temporaryFiles temporary files that belongs to this preview.
     */
    public OfficeDocumentPreview(AttachmentReference attachmentReference, String version, XDOM xdom,
        Set<File> temporaryFiles)
    {
        this.attachmentReference = attachmentReference;
        this.version = version;
        this.xdom = xdom;
        this.temporaryFiles = temporaryFiles;
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() throws Exception
    {
        // Cleanup all the temporary files.
        for (File file : temporaryFiles) {
            file.delete();
        }
    }

    /**
     * @return reference to the attachment to which this preview belongs to.
     */
    public AttachmentReference getAttachmentReference()
    {
        return this.attachmentReference;
    }

    /**
     * @return version of the attachment to which this preview corresponds.
     */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * @return {@link XDOM} holding the preview document syntax.
     */
    public XDOM getXDOM()
    {
        return this.xdom;
    }
}
