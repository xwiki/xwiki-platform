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
package org.xwiki.activeinstalls2;

import java.util.List;

import org.xwiki.activeinstalls2.internal.data.Ping;
import org.xwiki.component.annotation.Role;

/**
 * Provides access to stored ping data.
 *
 * @version $Id$
 * @since 5.2M2
 */
@Role
public interface DataManager
{
    /**
     * Executes a Search query for Active Installs.
     *
     * @param jsonQuery the Elastic Search JSON query used to search for installs. For example:
     *        <pre>{@code
     *            {
     *                "term": { "distributionVersion" : "5.2" }
     *            }
     *        }</pre>
     * @return the parsed JSON result coming from Elastic Search, as a list of {@link Ping} object. Passing an empty
     *      or null json string results in returning all data found in the index (i.e no query constraint)
     * @throws Exception when an error happens while retrieving the data
     * @since 14.4RC1
     */
    List<Ping> searchInstalls(String jsonQuery) throws Exception;

    /**
     * Executes a Count query for Active Installs.
     *
     * @param jsonQuery the Elastic Search JSON query used to search for installs. Passing an empty
     *      or null json string results in returning a count of all data in the index (i.e no query constraint)
     * @return the count number
     * @throws Exception when an error happens while retrieving the data
     * @since 14.4RC1
     */
    long countInstalls(String jsonQuery) throws Exception;
}
