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
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.Right;

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
     * Check if authentication is always required for a given right.
     *
     * @param right the right to be checked.
     * @return {@code true} if authentication is needed.
     * @since 6.1RC1
     */
    boolean needsAuthentication(Right right);

    /**
     * Right now the security module logic only works with DOCUMENT based reference so PAGE reference need to be
     * converted.
     * This also removes parameters from the reference as they aren't supported by the security module.
     * 
     * @param reference the reference
     * @return the compatible reference
     * @since 10.6RC1
     */
    EntityReference toCompatibleEntityReference(EntityReference reference);
}
