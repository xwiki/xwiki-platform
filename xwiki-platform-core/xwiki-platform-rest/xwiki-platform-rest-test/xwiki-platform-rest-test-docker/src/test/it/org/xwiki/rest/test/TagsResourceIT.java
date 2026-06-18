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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.junit.jupiter.api.Test;
import org.xwiki.rest.Relations;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.PageSummary;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.rest.model.jaxb.Tag;
import org.xwiki.rest.model.jaxb.Tags;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.pages.PageTagsResource;
import org.xwiki.rest.resources.tags.PagesForTagsResource;
import org.xwiki.rest.resources.tags.TagsResource;
import org.xwiki.rest.test.framework.AbstractHttpIT;
import org.xwiki.rest.test.framework.TestConstants;
import org.xwiki.test.ui.TestUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @version $Id$
 */
class TagsResourceIT extends AbstractHttpIT
{
    @Override
    @Test
    protected void testRepresentation() throws Exception
    {
        String tagName = UUID.randomUUID().toString();

        createPageIfDoesntExist(TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME, "Test");

        GetMethod getMethod = executeGet(
            buildURI(PageResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME)
                .toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Tags tags = objectFactory.createTags();
        Tag tag = objectFactory.createTag();
        tag.setName(tagName);
        tags.getTags().add(tag);

        PutMethod putMethod = executePutXml(
            buildURI(PageTagsResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME)
                .toString(),
            tags, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, putMethod.getStatusCode(), getHttpMethodInfo(putMethod));

        getMethod = executeGet(
            buildURI(PageTagsResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME)
                .toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        tags = (Tags) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        boolean found = false;
        for (Tag t : tags.getTags()) {
            if (tagName.equals(t.getName())) {
                found = true;
                break;
            }
        }
        assertTrue(found);

        getMethod = executeGet(buildURI(TagsResource.class, getWiki()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        tags = (Tags) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        found = false;
        for (Tag t : tags.getTags()) {
            if (tagName.equals(t.getName())) {
                found = true;
                break;
            }
        }
        assertTrue(found);

        getMethod = executeGet(buildURI(PagesForTagsResource.class, getWiki(), tagName).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Pages pages = (Pages) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        found = false;
        for (PageSummary pageSummary : pages.getPageSummaries()) {
            if (pageSummary.getFullName()
                .equals(String.format("%s.%s", TestConstants.TEST_SPACE_NAME.get(0), TestConstants.TEST_PAGE_NAME))) {
                found = true;
            }
        }
        assertTrue(found);

        getMethod = executeGet(
            buildURI(PageResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME)
                .toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        Link tagsLink = getFirstLinkByRelation(page, Relations.TAGS);
        assertNotNull(tagsLink);
    }

    @Test
    void testPUTTagsWithTextPlain() throws Exception
    {
        createPageIfDoesntExist(TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME, "Test");

        String tagName = UUID.randomUUID().toString();

        GetMethod getMethod = executeGet(
            buildURI(PageResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME)
                .toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        PutMethod putMethod = executePut(
            buildURI(PageTagsResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME)
                .toString(),
            tagName, MediaType.TEXT_PLAIN, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, putMethod.getStatusCode(), getHttpMethodInfo(putMethod));

        getMethod = executeGet(
            buildURI(PageTagsResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME)
                .toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Tags tags = (Tags) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        boolean found = false;
        for (Tag t : tags.getTags()) {
            if (tagName.equals(t.getName())) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    void testPUTTagsFormUrlEncoded() throws Exception
    {
        createPageIfDoesntExist(TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME, "Test");

        String tagName = UUID.randomUUID().toString();

        GetMethod getMethod = executeGet(
            buildURI(PageResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME)
                .toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        NameValuePair[] nameValuePairs = new NameValuePair[1];
        nameValuePairs[0] = new NameValuePair("tags", tagName);

        PostMethod postMethod =
            executePostForm(
                String.format("%s?method=PUT",
                    buildURI(PageTagsResource.class, getWiki(), TestConstants.TEST_SPACE_NAME,
                        TestConstants.TEST_PAGE_NAME).toString()),
                nameValuePairs, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
                TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, postMethod.getStatusCode(), getHttpMethodInfo(postMethod));

        getMethod = executeGet(
            buildURI(PageTagsResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME)
                .toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Tags tags = (Tags) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        boolean found = false;
        for (Tag t : tags.getTags()) {
            if (tagName.equals(t.getName())) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    void testPagesForTagsMultipleTagsAndPagination() throws Exception
    {
        // Setup: create three pages with different tags
        String tagA = "TagA_" + UUID.randomUUID();
        String tagB = "TagB_" + UUID.randomUUID();

        String[] pages = { "PageForTagA", "PageForTagB", "PageForTagAB" };

        // Create test pages
        for (String page : pages) {
            createPageIfDoesntExist(TestConstants.TEST_SPACE_NAME, page, "Test");
        }

        // Add tags to pages
        Tags tags1 = this.objectFactory.createTags();
        tags1.getTags().add(this.objectFactory.createTag().withName(tagA));

        // Add tagA to page1
        executePutXml(
            buildURI(PageTagsResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, pages[0]),
            tags1, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());

        // Add tagB to page2
        Tags tags2 = this.objectFactory.createTags();
        tags2.getTags().add(this.objectFactory.createTag().withName(tagB));
        executePutXml(
            buildURI(PageTagsResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, pages[1]),
            tags2, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());

        // Add both tags to page3
        Tags tags3 = this.objectFactory.createTags();
        for (String tagName : new String[] { tagA, tagB }) {
            tags3.getTags().add(this.objectFactory.createTag().withName(tagName));
        }
        executePutXml(
            buildURI(PageTagsResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, pages[2]),
            tags3, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());

        // Query for both tags
        String tagQuery = tagA + "," + tagB;
        GetMethod getMethod = executeGet(
            buildURI(PagesForTagsResource.class, getWiki(), tagQuery));
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
        Pages returnedPages = (Pages) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        // Verify all pages are returned in alphabetical order
        List<String> expectedOrder = Arrays.stream(pages).sorted().collect(Collectors.toList());
        List<String> actualOrder = returnedPages.getPageSummaries().stream()
            .map(PageSummary::getName)
            .toList();
        assertEquals(expectedOrder, actualOrder);

        // Test pagination: number=1
        getMethod = executeGet(
            buildURI(PagesForTagsResource.class, getWiki(), tagQuery) + "?number=1");
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
        returnedPages = (Pages) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        assertEquals(1, returnedPages.getPageSummaries().size());
        assertEquals(expectedOrder.get(0), returnedPages.getPageSummaries().get(0).getName());

        // Test pagination: number=1, start=1
        getMethod = executeGet(
            buildURI(PagesForTagsResource.class, getWiki(), tagQuery) + "?number=1&start=1");
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
        returnedPages = (Pages) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        assertEquals(1, returnedPages.getPageSummaries().size());
        assertEquals(expectedOrder.get(1), returnedPages.getPageSummaries().get(0).getName());

        // Test error: number=-1
        getMethod = executeGet(
            buildURI(PagesForTagsResource.class, getWiki(), tagQuery) + "?number=-1");
        assertEquals(400, getMethod.getStatusCode());
        assertEquals(INVALID_LIMIT_MINUS_1, getMethod.getResponseBodyAsString());

        // Test error: number=1001
        getMethod = executeGet(
            buildURI(PagesForTagsResource.class, getWiki(), tagQuery) + "?number=1001");
        assertEquals(400, getMethod.getStatusCode());
        assertEquals(INVALID_LIMIT_1001, getMethod.getResponseBodyAsString());
    }
}
