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
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
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
}
