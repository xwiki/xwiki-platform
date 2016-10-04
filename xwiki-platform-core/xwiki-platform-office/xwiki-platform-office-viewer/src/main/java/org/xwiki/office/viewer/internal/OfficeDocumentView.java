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
public class OfficeDocumentView
{
    /**
     * @see #getResourceReference()
     */
    private final ResourceReference resourceReference;

    /**
     * @see #getXDOM()
     */
    private final XDOM xdom;

    /**
     * @see #getTemporaryFiles()
     */
    private final Set<File> temporaryFiles;

    /**
     * Creates a new {@link OfficeDocumentView} instance.
     * 
     * @param reference the reference to the office file to which this view belongs
     * @param xdom {@link XDOM} representation of the office document
     * @param temporaryFiles temporary files used by this view
     */
    public OfficeDocumentView(ResourceReference reference, XDOM xdom, Set<File> temporaryFiles)
    {
        this.resourceReference = reference;
        this.xdom = xdom;
        this.temporaryFiles = temporaryFiles;
    }

    /**
     * @return a reference to the office file that is the source of this view
     */
    public ResourceReference getResourceReference()
    {
        return this.resourceReference;
    }

    /**
     * @return {@link XDOM} representation of the office document
     */
    public XDOM getXDOM()
    {
        return this.xdom;
    }

    /**
     * @return the temporary files used by this view.
     */
    public Set<File> getTemporaryFiles()
    {
        return this.temporaryFiles;
    }
}
