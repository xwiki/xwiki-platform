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
package org.xwiki.security.internal;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.UserSecurityReference;

/**
 * Temporary interface to access XWiki information without depending on oldcore.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Role
public interface XWikiBridge
{
    /**
     * @return the wiki reference of the main wiki.
     */
    WikiReference getMainWikiReference();

    /**
     * @return {@code true} if the XWiki server is in read-only maintenance mode.
     */
    boolean isWikiReadOnly();

    /**
     * @param user the user reference to check.
     * @param wikiReference the wiki reference to be checked.
     * @return {@code true} if the user is the owner of the given wiki.
     */
    boolean isWikiOwner(UserSecurityReference user, WikiReference wikiReference);
}
