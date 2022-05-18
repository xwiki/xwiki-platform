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

import java.util.Map;
import java.util.function.Function;

import org.xwiki.activeinstalls2.internal.data.Ping;
import org.xwiki.component.annotation.Role;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.util.ObjectBuilder;

/**
 * Allows providing additional data in the ping sent to the server.
 *
 * @version $Id$
 * @since 6.1M1
 */
@Role
public interface PingDataProvider
{
    /**
     * Execute some setup before creating the index, the mappings and the data storing.
     *
     * @param client the Elasticsearch client to use to perform any setup (like crate an ingest pipeline, etc)
     * @throws Exception if an error occurred during ElasticSearch operations
     * @since 14.4RC1
     */
    void setup(ElasticsearchClient client) throws Exception;

    /**
     * @return the lambda to create index settings (if need be)
     * @since 14.4RC1
     */
    Function<IndexSettings.Builder, ObjectBuilder<IndexSettings>> provideIndexSettings();

    /**
     * @return the ElasticSearch mapping, represented as a Map
     * @since 14.4RC1
     */
    Map<String, Property> provideMapping();

    /**
     * @param ping the Ping object to populate with data
     * @since 14.4RC1
     */
    void provideData(Ping ping);
}
