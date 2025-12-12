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
package org.xwiki.rest.test;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.Relations;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.rest.model.jaxb.Space;
import org.xwiki.rest.model.jaxb.Spaces;
import org.xwiki.rest.model.jaxb.Wiki;
import org.xwiki.rest.model.jaxb.Wikis;
import org.xwiki.rest.resources.wikis.WikisResource;
import org.xwiki.rest.test.framework.AbstractHttpIT;

public class PagesResourceIT extends AbstractHttpIT
{
    @Override
    @Test
    public void testRepresentation() throws Exception
    {
        CloseableHttpResponse response = executeGet(getFullUri(WikisResource.class));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        Wikis wikis = (Wikis) unmarshaller.unmarshal(response.getEntity().getContent());
        Assert.assertTrue(wikis.getWikis().size() > 0);

        Wiki wiki = wikis.getWikis().get(0);
        Link link = getFirstLinkByRelation(wiki, Relations.SPACES);
        Assert.assertNotNull(link);

        response = executeGet(link.getHref());
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        Spaces spaces = (Spaces) unmarshaller.unmarshal(response.getEntity().getContent());
        Assert.assertTrue(spaces.getSpaces().size() > 0);

        Space space = spaces.getSpaces().get(0);
        link = getFirstLinkByRelation(space, Relations.PAGES);
        Assert.assertNotNull(link);

        response = executeGet(link.getHref());
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        Pages pages = (Pages) unmarshaller.unmarshal(response.getEntity().getContent());
        Assert.assertTrue(pages.getPageSummaries().size() > 0);

        checkLinks(pages);
    }

    @Test
    public void testPagesResourcePaginationAndErrors() throws Exception
    {
        // Setup: Ensure at least 2 pages exist in a space
        String spaceName = getTestClassName();
        String page1 = getTestMethodName() + "A";
        String page2 = getTestMethodName() + "B";
        DocumentReference ref1 = new DocumentReference(getWiki(), spaceName, page1);
        DocumentReference ref2 = new DocumentReference(getWiki(), spaceName, page2);
        try {
            this.testUtils.rest().delete(ref1);
            this.testUtils.rest().delete(ref2);
            this.testUtils.rest().savePage(ref1, "content1", "title1");
            this.testUtils.rest().savePage(ref2, "content2", "title2");

            // Test: number=-1 should return error
            CloseableHttpResponse response = executeGet(
                "%s?number=-1".formatted(buildURI(org.xwiki.rest.resources.pages.PagesResource.class, getWiki(), spaceName)));
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_MINUS_1, EntityUtils.toString(response.getEntity()));

            // Test: number=1001 should return error
            response = executeGet(
                "%s?number=1001".formatted(buildURI(org.xwiki.rest.resources.pages.PagesResource.class, getWiki(), spaceName)));
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_1001, EntityUtils.toString(response.getEntity()));

            // Test: pagination with number=1
            response = executeGet(
                "%s?number=1".formatted(buildURI(org.xwiki.rest.resources.pages.PagesResource.class, getWiki(), spaceName)));
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            Pages pages = (Pages) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertEquals(1, pages.getPageSummaries().size());

            String firstName = pages.getPageSummaries().get(0).getName();

            // Test: pagination with number=1 and start=1
            response = executeGet(
                "%s?number=1&start=1".formatted(buildURI(org.xwiki.rest.resources.pages.PagesResource.class, getWiki(), spaceName)));
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            pages = (Pages) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertEquals(1, pages.getPageSummaries().size());
            Assert.assertNotEquals(firstName, pages.getPageSummaries().get(0).getName());
        } finally {
            this.testUtils.rest().delete(ref1);
            this.testUtils.rest().delete(ref2);
        }
    }
}
