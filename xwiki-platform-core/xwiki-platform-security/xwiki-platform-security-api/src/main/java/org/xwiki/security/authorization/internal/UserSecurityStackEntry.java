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
package org.xwiki.security.authorization.internal;

import org.xwiki.model.reference.DocumentReference;

/**
 * This is an entry that holds an explicit user reference.
 *
 * @version $Id$
 * @since 4.3M2
 */
public class UserSecurityStackEntry implements SecurityStackEntry
{

    /**
     * The literal user that will be considered "content author" while this entry is at the top of the security
     * stack. 
     */
    private final DocumentReference userReference;

    /**
     * @param userReference {@see userReference}
     */
    public UserSecurityStackEntry(DocumentReference userReference)
    {
        this.userReference = userReference;
    }

    @Override
    public boolean grantProgrammingRight()
    {
        return false;
    }

    @Override
    public DocumentReference getContentAuthor()
    {
        return userReference;
    }

}
