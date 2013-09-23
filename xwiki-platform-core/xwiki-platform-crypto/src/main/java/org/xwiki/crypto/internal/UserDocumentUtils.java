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
package org.xwiki.crypto.internal;

import org.xwiki.component.annotation.Role;

/**
 * Means by which to get a string representation of the user page for the current user for inclusion in a certificate.
 * This component has no cryptographic code.
 * 
 * @version $Id$
 * @since 2.5M1
 */
@Role
public interface UserDocumentUtils
{
    /** @return The fully qualified name of the current user's document eg: xwiki:XWiki.JohnSmith. */
    String getCurrentUser();

    /**
     * Get the external URL pointing to the given user document.
     *
     * @param userDocName the string representation of the document reference for the user document.
     * @return A string representation of the external URL for the user doc.
     */
    String getUserDocURL(final String userDocName);
}
