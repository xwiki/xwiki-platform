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
package org.xwiki.activeinstalls.internal;

import org.xwiki.component.annotation.Role;
import io.searchbox.client.JestClient;

/**
 * Factory to get the {@link JestClient} instance to connect to the remote instance.
 *
 * @version $Id$
 * @since 5.2M2
 */
@Role
public interface JestClientManager
{
    /**
     * The elastic search index we use to index pings.
     */
    String INDEX = "installs";

    /**
     * The elastic search index type we use to index pings. The index is suffixed with a number corresponding to the
     * version of our Elastic Search data model. The first data model was in an index named "install", the second one in
     * "install2" and if we need a third model in the future, it'll go in "install3".
     */
    String TYPE = "install2";

    /**
     * @return the object to use to connect to the remote instance
     */
    JestClient getClient();
}
