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

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.Relations;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.SearchResult;
import org.xwiki.rest.model.jaxb.SearchResults;
import org.xwiki.rest.model.jaxb.Space;
import org.xwiki.rest.model.jaxb.Spaces;
import org.xwiki.rest.model.jaxb.Wiki;
import org.xwiki.rest.model.jaxb.Wikis;
import org.xwiki.rest.resources.spaces.SpaceAttachmentsResource;
import org.xwiki.rest.resources.spaces.SpaceSearchResource;
import org.xwiki.rest.resources.spaces.SpacesResource;
import org.xwiki.rest.resources.wikis.WikisResource;
import org.xwiki.rest.test.framework.AbstractHttpIT;

public class SpacesResourceIT extends AbstractHttpIT
{
    @Override
    @Test
    public void testRepresentation() throws Exception
    {
        // Create a subspace
        createPageIfDoesntExist(Arrays.asList("SpaceA", "SpaceB", "SpaceC"), "MyPage", "some content");

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

        boolean nestedSpaceFound = false;

        for (Space space : spaces.getSpaces()) {
            link = getFirstLinkByRelation(space, Relations.SEARCH);
            Assert.assertNotNull(link);

            checkLinks(space);

            if ("xwiki:SpaceA.SpaceB.SpaceC".equals(space.getId())) {
                Assert.assertEquals("SpaceC", space.getName());
                nestedSpaceFound = true;
            }

            /*
             * Check that in the returned spaces there are not spaces not visible to user Guest, for example the
             * "Scheduler" space
             */
            Assert.assertFalse(space.getName().equals("Scheduler"));
        }

        Assert.assertTrue(nestedSpaceFound);
    }

    @Test
    public void testSearch() throws Exception
    {
        DocumentReference reference = new DocumentReference(getWiki(), getTestClassName(), getTestMethodName());
        this.testUtils.rest().delete(reference);
        this.testUtils.rest().savePage(reference, "content " + getTestMethodName(), "title " + getTestMethodName());

        this.solrUtils.waitEmptyQueue();

        CloseableHttpResponse response = executeGet(String.format("%s?q=somethingthatcannotpossiblyexist",
            buildURI(SpaceSearchResource.class, getWiki(), Arrays.asList(getTestClassName()))));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertEquals(0, searchResults.getSearchResults().size());

        response = executeGet(String.format("%s?q=%s",
            buildURI(SpaceSearchResource.class, getWiki(), Arrays.asList(getTestClassName())), getTestMethodName()));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        searchResults = (SearchResults) unmarshaller.unmarshal(response.getEntity().getContent());

        int resultSize = searchResults.getSearchResults().size();
        Assert.assertTrue("Found " + resultSize + " result", resultSize == 1);

        for (SearchResult searchResult : searchResults.getSearchResults()) {
            checkLinks(searchResult);
        }
    }

    @Test
    public void testAttachments() throws Exception
    {
        DocumentReference reference = new DocumentReference(getWiki(), getTestClassName(), getTestMethodName());
        this.testUtils.rest().delete(reference);
        this.testUtils.rest().savePage(reference);
        this.testUtils.rest().attachFile(new AttachmentReference("attachment.txt", reference),
            new ReaderInputStream(new StringReader("content"), StandardCharsets.UTF_8), true);

        // Matches Sandbox.WebHome@XWikLogo.png
        CloseableHttpResponse response = executeGet(String.format("%s",
            buildURI(SpaceAttachmentsResource.class, getWiki(), Arrays.asList(getTestClassName()))));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        Attachments attachments = (Attachments) unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertEquals(getAttachmentsInfo(attachments), 1, attachments.getAttachments().size());

        for (Attachment attachment : attachments.getAttachments()) {
            checkLinks(attachment);
        }
    }

    @Test
    public void testSpacesNumberParameter() throws Exception
    {
        // Setup: Ensure at least 2 spaces exist
        DocumentReference ref1 = new DocumentReference(getWiki(), getTestClassName() + "A", "WebHome");
        DocumentReference ref2 = new DocumentReference(getWiki(), getTestClassName() + "B", "WebHome");
        try {
            this.testUtils.rest().delete(ref1);
            this.testUtils.rest().delete(ref2);
            this.testUtils.rest().savePage(ref1, "content1", "title1");
            this.testUtils.rest().savePage(ref2, "content2", "title2");

            // Test: number=-1 should return error
            CloseableHttpResponse response = executeGet("%s?number=-1".formatted(buildURI(SpacesResource.class, getWiki())));
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_MINUS_1, EntityUtils.toString(response.getEntity()));

            // Test: number=1001 should return error
            response =  executeGet("%s?number=1001".formatted(buildURI(SpacesResource.class, getWiki())));
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_1001, EntityUtils.toString(response.getEntity()));

            // Test: pagination with number=1
            response =  executeGet("%s?number=1".formatted(buildURI(SpacesResource.class, getWiki())));
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            Spaces spaces = (Spaces) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertEquals(1, spaces.getSpaces().size());

            String firstName = spaces.getSpaces().get(0).getName();

            // Test: pagination with number=1 and start=1
            response =  executeGet("%s?number=1&start=1".formatted(buildURI(SpacesResource.class, getWiki())));
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            spaces = (Spaces) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertEquals(1, spaces.getSpaces().size());
            Assert.assertNotEquals(firstName, spaces.getSpaces().get(0).getName());
        } finally {
            this.testUtils.rest().delete(ref1);
            this.testUtils.rest().delete(ref2);
        }
    }

    @Test
    public void testSpaceSearchNumberParameter() throws Exception
    {
        // Setup: Ensure at least 2 pages exist for search
        String spaceName = getTestClassName();
        DocumentReference ref1 = new DocumentReference(getWiki(), spaceName, getTestMethodName() + "A");
        DocumentReference ref2 = new DocumentReference(getWiki(), spaceName, getTestMethodName() + "B");
        try {
            this.testUtils.rest().delete(ref1);
            this.testUtils.rest().delete(ref2);
            this.testUtils.rest().savePage(ref1, "searchcontent", "searchtitleA");
            this.testUtils.rest().savePage(ref2, "searchcontent", "searchtitleB");

            this.solrUtils.waitEmptyQueue();

            // Test: number=-1 should return error
            CloseableHttpResponse response = executeGet(
                "%s?q=searchcontent&number=-1".formatted(buildURI(SpaceSearchResource.class, getWiki(),
                    List.of(spaceName))));
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_MINUS_1, EntityUtils.toString(response.getEntity()));

            // Test: number=1001 should return error
            response =  executeGet(
                "%s?q=searchcontent&number=1001".formatted(
                    buildURI(SpaceSearchResource.class, getWiki(), List.of(spaceName))));
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_1001, EntityUtils.toString(response.getEntity()));

            // Test: pagination with number=1
            response =  executeGet(
                "%s?q=searchcontent&number=1".formatted(
                    buildURI(SpaceSearchResource.class, getWiki(), List.of(spaceName))));
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            SearchResults results = (SearchResults) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertEquals(1, results.getSearchResults().size());

            String firstName = results.getSearchResults().get(0).getPageName();

            // Test: pagination with number=1 and start=1
            response =  executeGet(
                "%s?q=searchcontent&number=1&start=1".formatted(
                    buildURI(SpaceSearchResource.class, getWiki(), List.of(spaceName))));
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            results = (SearchResults) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertEquals(1, results.getSearchResults().size());
            Assert.assertNotEquals(firstName, results.getSearchResults().get(0).getPageName());
        } finally {
            this.testUtils.rest().delete(ref1);
            this.testUtils.rest().delete(ref2);
        }
    }

    @Test
    public void testSpaceAttachmentsNumberParameter() throws Exception
    {
        // Setup: Ensure at least 2 attachments exist in the space
        String spaceName = getTestClassName();
        DocumentReference ref = new DocumentReference(getWiki(), spaceName, getTestMethodName());
        this.testUtils.rest().delete(ref);
        this.testUtils.rest().savePage(ref);

        try {
            this.testUtils.rest().attachFile(new AttachmentReference("att1.txt", ref),
                new ByteArrayInputStream("content1".getBytes(StandardCharsets.UTF_8)), true);
            this.testUtils.rest().attachFile(new AttachmentReference("att2.txt", ref),
                new ByteArrayInputStream("content2".getBytes(StandardCharsets.UTF_8)), true);
            // Test: number=-1 should return error
            CloseableHttpResponse response = executeGet(
                "%s?number=-1".formatted(buildURI(SpaceAttachmentsResource.class, getWiki(), List.of(spaceName))));
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_MINUS_1, EntityUtils.toString(response.getEntity()));

            // Test: number=1001 should return error
            response =  executeGet(
                "%s?number=1001".formatted(buildURI(SpaceAttachmentsResource.class, getWiki(),
                    List.of(spaceName))));
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_1001, EntityUtils.toString(response.getEntity()));

            // Test: pagination with number=1
            response =  executeGet(
                "%s?number=1".formatted(buildURI(SpaceAttachmentsResource.class, getWiki(), List.of(spaceName))));
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            Attachments attachments = (Attachments) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertEquals(1, attachments.getAttachments().size());

            String firstName = attachments.getAttachments().get(0).getName();

            // Test: pagination with number=1 and start=1
            response =  executeGet(
                "%s?number=1&start=1".formatted(buildURI(SpaceAttachmentsResource.class, getWiki(),
                    List.of(spaceName))));
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            attachments = (Attachments) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertEquals(1, attachments.getAttachments().size());
            Assert.assertNotEquals(firstName, attachments.getAttachments().get(0).getName());
        } finally {
            // Clean up
            this.testUtils.rest().delete(ref);
        }
    }
}
