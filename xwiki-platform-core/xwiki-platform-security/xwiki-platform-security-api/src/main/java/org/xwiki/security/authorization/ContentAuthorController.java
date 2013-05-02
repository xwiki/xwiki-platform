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
package org.xwiki.security.authorization;

import org.xwiki.model.reference.DocumentReference;

import org.xwiki.component.annotation.Role;

/**
 * Interface for changing the content author in the authorization context.
 *
 * The content author controller should always be used in a try-finally statement to ensure that the content author is
 * correctly popped of the stack.
 *
 *
 * @version $Id$
 * @since 4.3M2
 */
@Role
public interface ContentAuthorController
{
    /**
     * Set a new literal content author.
     *
     * @param userReference The new content author reference.
     */
    void pushContentAuthor(DocumentReference userReference);

    /**
     * Remove the current content author from the top of the security stack.
     */
    void popContentAuthor();

}
