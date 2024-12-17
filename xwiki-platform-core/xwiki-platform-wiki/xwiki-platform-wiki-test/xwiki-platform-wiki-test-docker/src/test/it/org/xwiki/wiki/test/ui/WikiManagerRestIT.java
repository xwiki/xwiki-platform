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
package org.xwiki.wiki.test.ui;

import java.io.InputStream;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.rest.model.jaxb.Wiki;
import org.xwiki.rest.model.jaxb.Wikis;
import org.xwiki.rest.resources.wikis.WikisResource;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.wiki.rest.WikiManagerREST;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the Wiki manager REST API.
 *
 * @version $Id$
 */
@UITest
class WikiManagerRestIT
{
    @Test
    @Order(1)
    void testCreateWiki(TestUtils setup) throws Exception
    {
        setup.createUser("CreateWikiTest", "CreateWikiTestPWD", null);
        setup.login("CreateWikiTest", "CreateWikiTestPWD");
        String wikiId = "foo";

        Wiki wiki = new Wiki();
        wiki.setId(wikiId);
        wiki.setName("test");
        wiki.setName("Some description");
        PostMethod postMethod = setup.rest().executePost(WikiManagerREST.class, wiki);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, postMethod.getStatusCode());

        // Need admin right to create a wiki
        setup.setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);
        postMethod = setup.rest().executePost(WikiManagerREST.class, wiki);
        assertEquals(HttpStatus.SC_CREATED, postMethod.getStatusCode());

        try (InputStream stream = postMethod.getResponseBodyAsStream()) {
            wiki = setup.rest().toResource(stream);
        }
        assertEquals(wikiId, wiki.getId());

        // Back to guest
        setup.setDefaultCredentials(null);
        Wikis wikis = setup.rest().get(WikisResource.class, true);

        boolean found = false;
        for (Wiki w : wikis.getWikis()) {
            if (wikiId.equals(w.getId())) {
                found = true;
                break;
            }
        }

        assertTrue(found);
    }
}
