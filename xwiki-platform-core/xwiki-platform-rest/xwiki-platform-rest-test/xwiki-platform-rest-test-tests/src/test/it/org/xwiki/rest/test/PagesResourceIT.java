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

import java.util.List;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.Relations;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.PageSummary;
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
        GetMethod getMethod = executeGet(getFullUri(WikisResource.class));
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Wikis wikis = (Wikis) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        Assert.assertTrue(wikis.getWikis().size() > 0);

        Wiki wiki = wikis.getWikis().get(0);
        Link link = getFirstLinkByRelation(wiki, Relations.SPACES);
        Assert.assertNotNull(link);

        getMethod = executeGet(link.getHref());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Spaces spaces = (Spaces) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        Assert.assertTrue(spaces.getSpaces().size() > 0);

        Space space = spaces.getSpaces().get(0);
        link = getFirstLinkByRelation(space, Relations.PAGES);
        Assert.assertNotNull(link);

        getMethod = executeGet(link.getHref());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Pages pages = (Pages) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
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
            GetMethod getMethod = executeGet(
                "%s?number=-1".formatted(buildURI(org.xwiki.rest.resources.pages.PagesResource.class, getWiki(), spaceName)));
            Assert.assertEquals(400, getMethod.getStatusCode());
            Assert.assertEquals(INVALID_LIMIT_MINUS_1, getMethod.getResponseBodyAsString());

            // Test: number=1001 should return error
            getMethod = executeGet(
                "%s?number=1001".formatted(buildURI(org.xwiki.rest.resources.pages.PagesResource.class, getWiki(), spaceName)));
            Assert.assertEquals(400, getMethod.getStatusCode());
            Assert.assertEquals(INVALID_LIMIT_1001, getMethod.getResponseBodyAsString());

            // Test: pagination with number=1
            getMethod = executeGet(
                "%s?number=1".formatted(buildURI(org.xwiki.rest.resources.pages.PagesResource.class, getWiki(), spaceName)));
            Assert.assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());
            Pages pages = (Pages) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            Assert.assertEquals(1, pages.getPageSummaries().size());

            String firstName = pages.getPageSummaries().get(0).getName();

            // Test: pagination with number=1 and start=1
            getMethod = executeGet(
                "%s?number=1&start=1".formatted(buildURI(org.xwiki.rest.resources.pages.PagesResource.class, getWiki(), spaceName)));
            Assert.assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());
            pages = (Pages) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            Assert.assertEquals(1, pages.getPageSummaries().size());
            Assert.assertNotEquals(firstName, pages.getPageSummaries().get(0).getName());
        } finally {
            this.testUtils.rest().delete(ref1);
            this.testUtils.rest().delete(ref2);
        }
    }

    /**
     * Check that the pages of a space are returned when they are ordered by date. On Oracle, the empty string is
     * stored as null, so the locale condition of the query needs to accept null, too, or the result is always empty.
     */
    @Test
    void testPagesResourceOrderedByDate() throws Exception
    {
        DocumentReference reference = new DocumentReference(getWiki(), getTestClassName(), getTestMethodName());
        try {
            getUtil().rest().delete(reference);
            getUtil().rest().savePage(reference, "content", "title");

            List<String> names = getPageNames("order=date&number=100");
            assertTrue(names.contains(reference.getName()), names.toString());
        } finally {
            getUtil().rest().delete(reference);
        }
    }

    /**
     * Check that paginating the pages ordered by date returns each page exactly once, i.e., that the order doesn't
     * change between queries with different limit/offset values.
     */
    @Test
    void testPagesResourcePaginationOrderedByDate() throws Exception
    {
        String spaceName = getTestClassName();
        DocumentReference ref1 = new DocumentReference(getWiki(), spaceName, getTestMethodName() + "A");
        DocumentReference ref2 = new DocumentReference(getWiki(), spaceName, getTestMethodName() + "B");
        try {
            getUtil().rest().delete(ref1);
            getUtil().rest().delete(ref2);
            getUtil().rest().savePage(ref1, "content1", "title1");
            getUtil().rest().savePage(ref2, "content2", "title2");

            List<String> allNames = getPageNames("order=date&number=100");
            assertTrue(allNames.containsAll(List.of(ref1.getName(), ref2.getName())), allNames.toString());

            // Fetching the pages one by one must return them in the same order as fetching them all at once.
            for (int i = 0; i < allNames.size(); ++i) {
                assertEquals(List.of(allNames.get(i)), getPageNames("order=date&number=1&start=" + i));
            }
        } finally {
            getUtil().rest().delete(ref1);
            getUtil().rest().delete(ref2);
        }
    }

    private List<String> getPageNames(String queryString) throws Exception
    {
        GetMethod getMethod = executeGet("%s?%s".formatted(
            buildURI(org.xwiki.rest.resources.pages.PagesResource.class, getWiki(), getTestClassName()), queryString));
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
        Pages pages = (Pages) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        return pages.getPageSummaries().stream().map(PageSummary::getName).toList();
    }
}
