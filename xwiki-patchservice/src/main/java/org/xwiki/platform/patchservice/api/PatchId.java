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
 *
 */
package org.xwiki.platform.patchservice.api;

import java.io.Serializable;
import java.util.Date;

/**
 * The ID of the patch. It identifies the affected document, and tries to give an unique timestamp/placestamp that can
 * be used both for managing a collection of patches and ensuring consistency among peers.
 * 
 * @see RWPatchId
 * @version $Id$
 * @since XWikiPlatform 1.3
 */
public interface PatchId extends XmlSerializable, Serializable
{
    /**
     * Get the identifier of the host where the patch was created.
     * 
     * @return The name of the host where the changes occured.
     */
    String getHostId();

    /**
     * Get a logical vectorial timestamp which tries to ensure a global ordering of the distributed events.
     * 
     * @return A logical timestamp of the changes.
     */
    LogicalTime getLogicalTime();

    /**
     * Get the local timestamp of the host when the patch was created.
     * 
     * @return Local timestamp of the changes. Might not be accurate if the host's date is not correctly set.
     */
    Date getTime();

    /**
     * Get the affected document's identifier.
     * 
     * @return The document name.
     */
    String getDocumentId();
}
