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
package org.xwiki.test.rest;

import java.io.StringReader;
import java.util.Arrays;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.engine.io.ReaderInputStream;
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
import org.xwiki.rest.resources.wikis.WikisResource;
import org.xwiki.test.rest.framework.AbstractHttpTest;

public class SpacesResourceTest extends AbstractHttpTest
{
    @Override
    @Test
    public void testRepresentation() throws Exception
    {
        // Create a subspace
        createPageIfDoesntExist(Arrays.asList("SpaceA", "SpaceB", "SpaceC"), "MyPage", "some content");

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

        GetMethod getMethod = executeGet(String.format("%s?q=somethingthatcannotpossiblyexist",
            buildURI(SpaceSearchResource.class, getWiki(), Arrays.asList(getTestClassName()))));
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        Assert.assertEquals(0, searchResults.getSearchResults().size());

        getMethod = executeGet(String.format("%s?q=%s",
            buildURI(SpaceSearchResource.class, getWiki(), Arrays.asList(getTestClassName())), getTestMethodName()));
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

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
            new ReaderInputStream(new StringReader("content")), true);

        // Matches Sandbox.WebHome@XWikLogo.png
        GetMethod getMethod = executeGet(String.format("%s",
            buildURI(SpaceAttachmentsResource.class, getWiki(), Arrays.asList(getTestClassName()))));
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Attachments attachments = (Attachments) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        Assert.assertEquals(getAttachmentsInfo(attachments), 1, attachments.getAttachments().size());

        for (Attachment attachment : attachments.getAttachments()) {
            checkLinks(attachment);
        }

    }
}
