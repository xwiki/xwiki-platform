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
 * The security stack stores various items which defines the authorization context.
 *
 * @version $Id$
 * @since 4.3M2
 */
public interface SecurityStackEntry
{
    /** @return {@literal true} if the right service currently should grant programming right permissions. */
    boolean grantProgrammingRight();

    /**
     * @return A the content author, which is the user who is responsible for any content that is be rendered while the
     * content author is at the top of the security stack.
     */
    DocumentReference getContentAuthor();
}
