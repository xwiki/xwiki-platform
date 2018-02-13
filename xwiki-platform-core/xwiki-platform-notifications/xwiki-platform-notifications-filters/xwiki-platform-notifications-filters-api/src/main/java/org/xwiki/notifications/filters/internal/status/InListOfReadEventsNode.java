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
 * Special node to filter on events that has been read by the given user.
 *
 * @version $Id$
 * @since 10.1RC1
 */
public class InListOfReadEventsNode extends AbstractOperatorNode
{
    private DocumentReference user;

    /**
     * Construct an InListOfReadEventsNode.
     * @param user the user
     */
    public InListOfReadEventsNode(DocumentReference user)
    {
        this.user = user;
    }

    /**
     * @return the user
     */
    public DocumentReference getUser()
    {
        return user;
    }

    @Override
    public String toString()
    {
        return "LIST_OF_READ_EVENTS";
    }
}
