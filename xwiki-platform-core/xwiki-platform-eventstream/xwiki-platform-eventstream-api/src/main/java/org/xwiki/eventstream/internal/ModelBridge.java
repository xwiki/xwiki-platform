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
package org.xwiki.eventstream.internal;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * This is an internal role that allows requests to the model without having dependencies on xwiki-platform-oldcore.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Role
public interface ModelBridge
{
    /**
     * Get the author reference of the given entity.
     *
     * @param entityReference the document
     * @return the author reference
     * @throws EventStreamException if the author reference could not be retrieved
     */
    DocumentReference getAuthorReference(EntityReference entityReference) throws EventStreamException;

    /**
     * Check that the given source contains one of the XObject given in xObjectTypes. If the list of
     * xObjectTypes is empty, returns true.
     *
     * @param xObjectTypes a list of XObject types
     * @param source the source object
     * @return true if the source is an XWikiDocument and contains at least one of the given XObjects
     */
    boolean checkXObjectPresence(List<String> xObjectTypes, Object source);
}
