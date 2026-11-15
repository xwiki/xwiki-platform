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
package org.xwiki.doc.lock;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * When a user starts editing a document that is locked by another user, they are asked to confirm that they want to
 * take over the lock. The components implementing this role can waive that confirmation, for the cases where taking
 * over the lock is harmless, for instance because both users are going to edit the document together, in a realtime
 * collaboration session.
 * <p>
 * All the unlock rules are evaluated until one of them accepts to unlock the document, so a rule that doesn't apply
 * simply returns {@code false}.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
@Unstable
@Role
public interface UnlockRule
{
    /**
     * @param context describes the document whose lock the current user is about to take over, and how that document is
     *            going to be edited
     * @return {@code true} if the lock can be taken over without asking the user to confirm, {@code false} otherwise
     */
    boolean canUnlock(LockContext context);
}
