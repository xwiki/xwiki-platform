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
package org.xwiki.mail;

import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Save, load and search mail results.
 *
 * @version $Id$
 * @since 6.4M3
 */
@Role
@Unstable
public interface MailStatusStore
{
    /**
     * Saves mail status in the store.
     *
     * @param status the mail status to be saved
     * @param parameters some parameters specifying addition context data (for example the current wiki is stored under
     *        the {@code wiki} key)
     * @throws MailStoreException when an error occurs saving the data
     */
    void save(MailStatus status, Map<String, Object> parameters) throws MailStoreException;

    /**
     * Loads all message statuses matching the passed filters.
     *
     * @param filterMap the map of Mail Status parameters to match (e.g. "status", "wiki", "batchId", etc)
     * @return the loaded {@link org.xwiki.mail.MailStatus} instances
     * @throws MailStoreException when an error occurs when loading the data
     */
    List<MailStatus> load(Map<String, Object> filterMap) throws MailStoreException;

    /**
     * Count the number of message statuses matching the passed filters.
     *
     * @param filterMap the map of Mail Status parameters to match (e.g. "status", "wiki", "batchId", etc)
     * @return the number of emails matching the passed filters
     * @throws MailStoreException when an error occurs when loading the data
     */
    long count(Map<String, Object> filterMap) throws MailStoreException;
}
