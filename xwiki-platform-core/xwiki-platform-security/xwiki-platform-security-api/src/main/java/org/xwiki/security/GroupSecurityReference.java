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
package org.xwiki.security;

import org.xwiki.model.reference.DocumentReference;

/**
 * GroupSecurityReference is a {@link SecurityReference} that is used to represent a security group,
 * which is a set of users, such that rights can be assigned to the whole group at once.  A security
 * group entity is a document that represents the group by containing group member objects.
 *
 * @see SecurityReferenceFactory
 * @version $Id$
 * @since 4.0M2
 */
public class GroupSecurityReference extends UserSecurityReference
{
    /** Serialization identifier. */
    private static final long serialVersionUID = 1L;

    /**
     * @param reference the reference to a group
     * @param mainWiki the reference to the main wiki
     */
    GroupSecurityReference(DocumentReference reference, SecurityReference mainWiki)
    {
        super(reference, mainWiki);
        if (this.getOriginalReference() == null) {
            throw new IllegalArgumentException("A security group reference could not be null.");
        }
        // TODO: really check that we have a real group document
    }
}
