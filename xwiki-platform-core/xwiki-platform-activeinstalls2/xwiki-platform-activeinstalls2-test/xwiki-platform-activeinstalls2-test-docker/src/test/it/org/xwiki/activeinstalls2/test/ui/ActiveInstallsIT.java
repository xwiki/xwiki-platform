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
package org.xwiki.activeinstalls2.test.ui;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.xwiki.activeinstalls2.internal.XWikiElasticSearchExtension;
import org.xwiki.test.docker.junit5.UITest;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verify the overall Active Installs application features.
 *
 * @version $Id$
 */
@ExtendWith(XWikiElasticSearchExtension.class)
@ExtendWith(DynamicTestConfigurationExtension.class)
@UITest(properties = {
    // The Instance module contributes a Hibernate mapping that needs to be added to hibernate.cfg.xml
    "xwikiDbHbmCommonExtraMappings=instance.hbm.xml"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-19932
        "org.xwiki.platform:xwiki-platform-instance"
    }
)
class ActiveInstallsIT
{
    @Test
    void verifyActiveInstalls(ElasticsearchContainer container) throws Exception
    {
        // When XWiki was started by @UITest, a page was checked to verify that the XWiki instance was up. This, in
        // turn, triggered the sending of an asynchronous ping to the ES instance (started prior to the XWiki start in
        // XWikiElasticSearchExtension).
        //
        // Since the ping may take some time to be sent to our ES instance, we wait till we have 1 index in ES or
        // till the timeout expires.
        RestClient restClient = RestClient.builder(HttpHost.create(container.getHttpHostAddress()))
            .build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        ElasticsearchClient client = new ElasticsearchClient(transport);

        client.indices().refresh();

        long count = 0;
        long time = System.currentTimeMillis();
        while (count != 1 && (System.currentTimeMillis() - time) < 30000L) {
            count = client.count(b -> b.index("installs3")).count();
            Thread.sleep(100L);
        }
        assertEquals(1, count, "AS ping wasn't sent by the XWiki instance");
    }
}
