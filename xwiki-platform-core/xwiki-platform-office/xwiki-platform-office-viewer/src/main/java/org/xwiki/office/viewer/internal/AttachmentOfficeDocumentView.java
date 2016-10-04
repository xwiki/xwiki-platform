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
package org.xwiki.office.viewer.internal;

import java.io.File;
import java.util.Set;

import org.xwiki.cache.DisposableCacheValue;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.reference.ResourceReference;

/**
 * Holds all the information belonging to an office attachment view. Instances of this class are mainly used for caching
 * office attachment views.
 * 
 * @since 5.4.6
 * @since 6.2.2
 * @version $Id$
 */
public class AttachmentOfficeDocumentView extends OfficeDocumentView implements DisposableCacheValue
{
    /**
     * Reference to the office attachment to which this view belongs.
     */
    private final AttachmentReference attachmentReference;

    /**
     * Specific version of the attachment to which this view corresponds.
     */
    private final String version;

    /**
     * Creates a new {@link AttachmentOfficeDocumentView} instance.
     * 
     * @param reference the reference to the office file to which this view belongs
     * @param attachmentReference reference to the office attachment to which this view belongs
     * @param version version of the attachment to which this view corresponds
     * @param xdom {@link XDOM} representation of the office document
     * @param temporaryFiles temporary files used by this view
     */
    public AttachmentOfficeDocumentView(ResourceReference reference, AttachmentReference attachmentReference,
        String version, XDOM xdom, Set<File> temporaryFiles)
    {
        super(reference, xdom, temporaryFiles);

        this.attachmentReference = attachmentReference;
        this.version = version;
    }

    /**
     * @return a reference to the office attachment that is the source of this view
     */
    public AttachmentReference getAttachmentReference()
    {
        return this.attachmentReference;
    }

    /**
     * @return the version of the attachment that is the source of this view
     */
    public String getVersion()
    {
        return this.version;
    }

    @Override
    public void dispose() throws Exception
    {
        // Cleanup all the temporary files.
        for (File file : getTemporaryFiles()) {
            file.delete();
        }
    }
}
