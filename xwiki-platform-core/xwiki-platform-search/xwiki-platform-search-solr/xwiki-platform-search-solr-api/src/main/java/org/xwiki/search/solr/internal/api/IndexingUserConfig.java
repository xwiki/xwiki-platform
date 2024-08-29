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
package org.xwiki.search.solr.internal.api;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;

/**
 * Provides a configured indexing user.
 *
 * @version $Id$
 * @since 14.8M1
 */
@Role
public interface IndexingUserConfig
{
    /**
     * Get the user to be used in the context of indexing threads.
     * <br />
     * Velocity code in wiki page titles (or the sheets used by that wiki page)
     * is executed with the rights of this user.
     * Currently it is not used elsewhere.
     * <br />
     * If the method returns null, there is no indexing user configured,
     * and indexing should use the anonymous user.
     * @return a reference to the indexing user
     * @throws XWikiException if loading the configuration fails
     */
    DocumentReference getIndexingUserReference() throws XWikiException;
}
