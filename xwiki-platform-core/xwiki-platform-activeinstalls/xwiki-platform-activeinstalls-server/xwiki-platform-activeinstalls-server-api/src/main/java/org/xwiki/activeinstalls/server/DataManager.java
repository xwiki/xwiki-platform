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
package org.xwiki.activeinstalls.server;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Provides access to stored ping data.
 *
 * @version $Id$
 * @since 5.2M2
 */
@Role
@Unstable
public interface DataManager
{
    /**
     * @return the total number of XWiki installs (active or not)
     * @throws Exception when an error happens while retrieving the data
     */
    long getTotalInstalls() throws Exception;

    /**
     * @return the total number of active installs (i.e. we've received a ping from them in the past N months - defined
     *         in the Active Installs module configuration)
     * @throws Exception when an error happens while retrieving the data
     */
    long getActiveInstalls() throws Exception;
}
