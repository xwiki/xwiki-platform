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
package org.xwiki.query;

/**
 * Extends {@link Query} with various security related options.
 * 
 * @version $Id$
 * @since 7.2M2
 */
public interface SecureQuery extends Query
{
    /**
     * @return true if the right of the current author (usually the content author of the context document or secure
     *         document) should be checked
     */
    boolean isCurrentAuthorChecked();

    /**
     * @param checkCurrentAuthor true if the right of the current author (usually the content author of the context
     *            document or secure document) should be checked
     * @return this query.
     */
    SecureQuery checkCurrentAuthor(boolean checkCurrentAuthor);

    /**
     * @return true if the right of the current author should be checked (for example to filter results)
     */
    boolean isCurrentUserChecked();

    /**
     * @param checkCurrentUser true if the right of the current user should be checked (for example to filter results)
     * @return this query.
     */
    SecureQuery checkCurrentUser(boolean checkCurrentUser);
}
