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
package org.xwiki.security.internal;

import org.xwiki.model.reference.DocumentReference;

/**
 * A right cache entry that represents a group.
 * @version $Id: $
 */
class GroupEntry extends ObjectEntry
{
    /** Store the document reference that represents the group. */
    private final DocumentReference groupReference;

    /** @param groupReference The document reference that represents the group. */
    public GroupEntry(DocumentReference groupReference)
    {
        this.groupReference = groupReference;
    }

    /** @return the document reference that represents the group. */
    public DocumentReference getGroupReference()
    {
        return groupReference;
    }
}
