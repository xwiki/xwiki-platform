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
package org.xwiki.notifications.filters.internal.status;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.filters.expression.generics.AbstractOperatorNode;

/**
 * Special node to filter on events listened by a specific user.
 *
 * @version $Id$
 * @since 12.1RC1
 */
public class ForUserNode extends AbstractOperatorNode
{
    private final DocumentReference user;

    private final Boolean read;

    /**
     * @param user the user
     * @param read true if only read status should be included, false for only unread status and null for all
     */
    public ForUserNode(DocumentReference user, Boolean read)
    {
        this.user = user;
        this.read = read;
    }

    /**
     * @return the user
     */
    public DocumentReference getUser()
    {
        return user;
    }

    /**
     * @return true if only read status should be included, false for only unread status and null for all
     */
    public Boolean isRead()
    {
        return this.read;
    }

    @Override
    public String toString()
    {
        return "FOR_USER_EVENTS";
    }
}
