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

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.Assert;
import org.junit.Test;
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

/**
 * @version $Id$
 */
public class TagsResourceIT extends AbstractHttpIT
{
    @Override
    @Test
    public void testRepresentation() throws Exception
    {
        String tagName = UUID.randomUUID().toString();

        createPageIfDoesntExist(TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME, "Test");

        CloseableHttpResponse response = executeGet(
            buildURI(PageResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        Tags tags = objectFactory.createTags();
        Tag tag = objectFactory.createTag();
        tag.setName(tagName);
        tags.getTags().add(tag);

        CloseableHttpResponse putMethod = executePutXml(
            buildURI(PageTagsResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME),
            tags, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpResponseInfo(putMethod), HttpStatus.SC_ACCEPTED, putMethod.getCode());

        response = executeGet(
            buildURI(PageTagsResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        tags = (Tags) unmarshaller.unmarshal(response.getEntity().getContent());
        boolean found = false;
        for (Tag t : tags.getTags()) {
            if (tagName.equals(t.getName())) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);

        response = executeGet(buildURI(TagsResource.class, getWiki()));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        tags = (Tags) unmarshaller.unmarshal(response.getEntity().getContent());
        found = false;
        for (Tag t : tags.getTags()) {
            if (tagName.equals(t.getName())) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);

        response = executeGet(buildURI(PagesForTagsResource.class, getWiki(), tagName));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        Pages pages = (Pages) unmarshaller.unmarshal(response.getEntity().getContent());

        found = false;
        for (PageSummary pageSummary : pages.getPageSummaries()) {
            if (pageSummary.getFullName()
                .equals(String.format("%s.%s", TestConstants.TEST_SPACE_NAME.get(0), TestConstants.TEST_PAGE_NAME))) {
                found = true;
            }
        }
        Assert.assertTrue(found);

        response = executeGet(
            buildURI(PageResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        Page page = (Page) unmarshaller.unmarshal(response.getEntity().getContent());
        Link tagsLink = getFirstLinkByRelation(page, Relations.TAGS);
        Assert.assertNotNull(tagsLink);
    }

    @Test
    public void testPUTTagsWithTextPlain() throws Exception
    {
        createPageIfDoesntExist(TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME, "Test");

        String tagName = UUID.randomUUID().toString();

        CloseableHttpResponse response = executeGet(
            buildURI(PageResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        response =
            executePut(
                buildURI(PageTagsResource.class, getWiki(), TestConstants.TEST_SPACE_NAME,
                    TestConstants.TEST_PAGE_NAME),
                tagName, MediaType.TEXT_PLAIN, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
                TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_ACCEPTED, response.getCode());

        response = executeGet(
            buildURI(PageTagsResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        Tags tags = (Tags) unmarshaller.unmarshal(response.getEntity().getContent());
        boolean found = false;
        for (Tag t : tags.getTags()) {
            if (tagName.equals(t.getName())) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);
    }

    @Test
    public void testPUTTagsFormUrlEncoded() throws Exception
    {
        createPageIfDoesntExist(TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME, "Test");

        String tagName = UUID.randomUUID().toString();

        CloseableHttpResponse response = executeGet(
            buildURI(PageResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        response = executePostForm(
            String.format("%s?method=PUT",
                buildURI(PageTagsResource.class, getWiki(), TestConstants.TEST_SPACE_NAME,
                    TestConstants.TEST_PAGE_NAME)),
            List.of(new BasicNameValuePair("tags", tagName)), TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_ACCEPTED, response.getCode());

        response = executeGet(
            buildURI(PageTagsResource.class, getWiki(), TestConstants.TEST_SPACE_NAME, TestConstants.TEST_PAGE_NAME));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        Tags tags = (Tags) unmarshaller.unmarshal(response.getEntity().getContent());
        boolean found = false;
        for (Tag t : tags.getTags()) {
            if (tagName.equals(t.getName())) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);
    }

    @Test
    public void testPagesForTagsMultipleTagsAndPagination() throws Exception
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
        CloseableHttpResponse response = executeGet(
            buildURI(PagesForTagsResource.class, getWiki(), tagQuery));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());
        Pages returnedPages = (Pages) this.unmarshaller.unmarshal(response.getEntity().getContent());

        // Verify all pages are returned in alphabetical order
        List<String> expectedOrder = Arrays.stream(pages).sorted().collect(Collectors.toList());
        List<String> actualOrder = returnedPages.getPageSummaries().stream()
            .map(PageSummary::getName)
            .toList();
        Assert.assertEquals(expectedOrder, actualOrder);

        // Test pagination: number=1
        response = executeGet(
            buildURI(PagesForTagsResource.class, getWiki(), tagQuery) + "?number=1");
        Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
        returnedPages = (Pages) this.unmarshaller.unmarshal(response.getEntity().getContent());
        Assert.assertEquals(1, returnedPages.getPageSummaries().size());
        Assert.assertEquals(expectedOrder.get(0), returnedPages.getPageSummaries().get(0).getName());

        // Test pagination: number=1, start=1
        response = executeGet(
            buildURI(PagesForTagsResource.class, getWiki(), tagQuery) + "?number=1&start=1");
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());
        returnedPages = (Pages) this.unmarshaller.unmarshal(response.getEntity().getContent());
        Assert.assertEquals(1, returnedPages.getPageSummaries().size());
        Assert.assertEquals(expectedOrder.get(1), returnedPages.getPageSummaries().get(0).getName());

        // Test error: number=-1
        response = executeGet(
            buildURI(PagesForTagsResource.class, getWiki(), tagQuery) + "?number=-1");
        Assert.assertEquals(400, response.getCode());
        Assert.assertEquals(INVALID_LIMIT_MINUS_1, EntityUtils.toString(response.getEntity()));

        // Test error: number=1001
        response = executeGet(
            buildURI(PagesForTagsResource.class, getWiki(), tagQuery) + "?number=1001");
        Assert.assertEquals(400, response.getCode());
        Assert.assertEquals(INVALID_LIMIT_1001, EntityUtils.toString(response.getEntity()));
    }
}
