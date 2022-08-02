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
package org.xwiki.user;

import java.io.Serializable;

/**
 * Abstracts the concept of User reference. This allows to support several store implementations for users.
 * For example for an implementation storing the users in wiki pages, the internal reference would be a
 * {@link org.xwiki.model.reference.DocumentReference}. The reference allows retrieving all the data about a user.
 * Another internal reference implementation could be an ActivityPub URL for example.
 *
 * @version $Id$
 * @since 12.2
 */
public interface UserReference extends Serializable
{
    /**
     * @return true if this reference points to a global user
     */
    boolean isGlobal();
}
