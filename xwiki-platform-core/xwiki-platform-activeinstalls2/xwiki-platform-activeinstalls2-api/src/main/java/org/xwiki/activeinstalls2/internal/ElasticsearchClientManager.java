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
package org.xwiki.activeinstalls2.internal;

import org.xwiki.component.annotation.Role;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

/**
 * Factory to get the {@link ElasticsearchClient} instance to connect to the remote instance.
 *
 * @version $Id$
 * @since 14.4RC1
 */
@Role
public interface ElasticsearchClientManager
{
    /**
     * The elastic search index we use to index pings. The index is suffixed with a number corresponding to the
     * version of our Elastic Search data model. A bit of history:
     * <ul>
     *   <li>The first data model was in an index named "installs" and a type named "install"</li>
     *   <li>The second data model was in an index named "installs" and a type named "install2"</li>
     *   <li>This 3rd data model is only using an index (and no type since that was deprecated in ES 7.x)</li>
     * </ul>
     */
    String INDEX = "installs3";

    /**
     * @return the object to use to connect to the remote ElasticSearch instance and perform operations using a
     *      typed Java API
     */
    ElasticsearchClient getClient();
}
