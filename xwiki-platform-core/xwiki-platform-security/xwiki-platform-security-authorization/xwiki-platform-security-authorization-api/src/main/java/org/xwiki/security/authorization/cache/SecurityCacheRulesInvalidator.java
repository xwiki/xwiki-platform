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
package org.xwiki.security.authorization.cache;

import org.xwiki.component.annotation.Role;

/**
 * This component instances define how to suspend / resume the cache for invalidating rules.
 * Note that this needs to be done to avoid problems for example: you could have an execution order where
 * <ol>
 * <li>a new security cache access entry is constructed by some cache loader then in another thread </li>
 * <li>the document is updated </li>
 * <li>the cache is invalidated and then the original thread resumes and </li>
 * <li>the security cache access entry still based on old information is inserted back into the cache.</li>
 * </ol>
 *
 * @version $Id$
 * @since 4.0M2
 */
@Role
public interface SecurityCacheRulesInvalidator
{
    /** Suspend delivery of invalidation events. */
    void suspend();

    /** Resume delivery of invalidation events. */
    void resume();
}
