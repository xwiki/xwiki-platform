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
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.io.input.ReaderInputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.Relations;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.PageSummary;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.rest.model.jaxb.SearchResult;
import org.xwiki.rest.model.jaxb.SearchResults;
import org.xwiki.rest.model.jaxb.Wiki;
import org.xwiki.rest.model.jaxb.Wikis;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.wikis.WikiAttachmentsResource;
import org.xwiki.rest.resources.wikis.WikiChildrenResource;
import org.xwiki.rest.resources.wikis.WikiPagesResource;
import org.xwiki.rest.resources.wikis.WikiResource;
import org.xwiki.rest.resources.wikis.WikiSearchQueryResource;
import org.xwiki.rest.resources.wikis.WikiSearchResource;
import org.xwiki.rest.resources.wikis.WikisResource;
import org.xwiki.rest.resources.wikis.WikisSearchQueryResource;
import org.xwiki.rest.test.framework.AbstractHttpIT;
import org.xwiki.test.ui.TestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WikisResourceIT extends AbstractHttpIT
{
    private String wikiName;

    private List<String> spaces;

    private String pageName;

    private String fullName;

    private DocumentReference reference;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.wikiName = getWiki();
        this.spaces = Arrays.asList(getTestClassName());
        this.pageName = getTestMethodName();
        this.fullName = getTestClassName() + '.' + getTestMethodName();

        this.reference = new DocumentReference(this.wikiName, this.spaces, this.pageName);
    }

    private SearchResults search(int expectedSize, String query)
    {
        try {
            GetMethod getMethod = executeGet(URIUtil.encodeQuery(query));
            Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

            SearchResults searchResults =
                (SearchResults) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

            int resultSize = searchResults.getSearchResults().size();
            if (resultSize == expectedSize) {
                return searchResults;
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }

        return null;
    }

    @Override
    @Test
    public void testRepresentation() throws Exception
    {
        GetMethod getMethod = executeGet(getFullUri(WikisResource.class));
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Wikis wikis = (Wikis) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        Assert.assertTrue(getHttpMethodInfo(getMethod), wikis.getWikis().size() > 0);

        for (Wiki wiki : wikis.getWikis()) {
            Link link = getFirstLinkByRelation(wiki, Relations.SPACES);
            Assert.assertNotNull(link);

            link = getFirstLinkByRelation(wiki, Relations.CLASSES);
            Assert.assertNotNull(link);

            link = getFirstLinkByRelation(wiki, Relations.MODIFICATIONS);
            Assert.assertNotNull(link);

            link = getFirstLinkByRelation(wiki, Relations.SEARCH);
            Assert.assertNotNull(link);

            link = getFirstLinkByRelation(wiki, Relations.QUERY);
            Assert.assertNotNull(link);

            checkLinks(wiki);
        }
    }

    @Test
    public void testSearchWikisName() throws Exception
    {
        this.testUtils.rest().delete(reference);
        this.testUtils.rest().savePage(reference, "Name Content", "Name Title " + this.pageName);

        GetMethod getMethod = executeGet(
            String.format("%s?scope=name&q=" + this.pageName, buildURI(WikiSearchResource.class, getWiki())));
        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        // Ensure that the terminal page is found by its name.
        int resultSize = searchResults.getSearchResults().size();
        assertEquals(1, resultSize);
        assertEquals(this.fullName, searchResults.getSearchResults().get(0).getPageFullName());

        // Create a non-terminal page with the same "name" but this time as last space.
        List<String> nonTerminalSpaces = List.of(this.spaces.get(0), this.pageName);
        DocumentReference nonTerminalReference = new DocumentReference(this.wikiName, nonTerminalSpaces, "WebHome");
        String nonTerminalFullName = String.join(".", nonTerminalSpaces) + "." + "WebHome";
        this.testUtils.rest().savePage(nonTerminalReference, "content2" + this.pageName, "title2" + this.pageName);

        getMethod = executeGet(
            String.format("%s?scope=name&q=" + this.pageName, buildURI(WikiSearchResource.class, getWiki())));
        searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        // Ensure that searching by name finds both terminal and non-terminal page.
        resultSize = searchResults.getSearchResults().size();
        assertEquals(2, resultSize);
        List<String> foundPages = searchResults.getSearchResults().stream()
            .map(SearchResult::getPageFullName)
            .collect(Collectors.toList());
        assertTrue(foundPages.contains(this.fullName));
        assertTrue(foundPages.contains(nonTerminalFullName));

        // Ensure that searching by space finds neither the terminal nor the non-terminal page.
        getMethod =
            executeGet(String.format("%s?scope=name&q=" + this.spaces.get(0),
                buildURI(WikiSearchResource.class, getWiki())));
        searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        assertEquals(0, searchResults.getSearchResults().size());
    }

    @Test
    public void testSearchWikis() throws Exception
    {
        this.testUtils.rest().delete(reference);
        this.testUtils.rest().savePage(reference, "content" + this.pageName, "title" + this.pageName);

        GetMethod getMethod =
            executeGet(String.format("%s?q=content" + this.pageName, buildURI(WikiSearchResource.class, getWiki())));
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        int resultSize = searchResults.getSearchResults().size();
        assertEquals(1, resultSize);

        for (SearchResult searchResult : searchResults.getSearchResults()) {
            checkLinks(searchResult);
        }

        getMethod = executeGet(
            String.format("%s?q=" + this.pageName + "&scope=name", buildURI(WikiSearchResource.class, getWiki())));
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        resultSize = searchResults.getSearchResults().size();
        assertEquals(1, resultSize);

        for (SearchResult searchResult : searchResults.getSearchResults()) {
            checkLinks(searchResult);
        }

        // Search in titles
        getMethod = executeGet(String.format("%s?q=title" + this.pageName + "&scope=title",
            buildURI(WikiSearchResource.class, getWiki())));
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        resultSize = searchResults.getSearchResults().size();
        assertEquals(1, resultSize);

        for (SearchResult searchResult : searchResults.getSearchResults()) {
            checkLinks(searchResult);
        }

        // Search for space names
        getMethod = executeGet(String.format("%s?q=" + this.spaces.get(0) + "&scope=spaces",
            buildURI(WikiSearchResource.class, getWiki())));
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        resultSize = searchResults.getSearchResults().size();
        assertEquals(1, resultSize);

        for (SearchResult searchResult : searchResults.getSearchResults()) {
            checkLinks(searchResult);
        }
    }

    @Test
    public void testObjectSearchNotAuthenticated() throws Exception
    {
        /* Check search for an object containing XWiki.Admin (i.e., the admin profile) */
        GetMethod getMethod =
            executeGet(String.format("%s?q=XWiki.Admin&scope=objects", buildURI(WikiSearchResource.class, getWiki())));
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        int resultSize = searchResults.getSearchResults().size();
        Assert.assertTrue(String.format("Found %s results", resultSize), resultSize == 0);
    }

    @Test
    public void testObjectSearchAuthenticated() throws Exception
    {
        /* Check search for an object containing XWiki.Admin (i.e., the admin profile) */
        GetMethod getMethod = executeGet(
            String.format("%s?q=XWiki.XWikiGuest&scope=objects", buildURI(WikiSearchResource.class, getWiki())),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        /*
         * We get more results because previous tests have also created comments on behalf of XWiki.Admin. They will
         * appear in the results.
         */
        int resultSize = searchResults.getSearchResults().size();
        Assert.assertTrue(String.format("Found %s results", resultSize), resultSize >= 1);
    }

    @Test
    public void testPages() throws Exception
    {
        // Create a clean test page.
        this.testUtils.rest().delete(this.reference);
        this.testUtils.rest().savePage(this.reference);

        // Get all pages
        GetMethod getMethod = executeGet(String.format("%s", buildURI(WikiPagesResource.class, getWiki())));
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Pages pages = (Pages) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        Assert.assertTrue(pages.getPageSummaries().size() > 0);

        for (PageSummary pageSummary : pages.getPageSummaries()) {
            checkLinks(pageSummary);
        }

        // Get all pages having a document name that contains "WebHome" (for all spaces)
        getMethod = executeGet(String.format("%s?name=" + this.pageName, buildURI(WikiPagesResource.class, getWiki())));
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        pages = (Pages) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        List<PageSummary> pageSummaries = pages.getPageSummaries();
        Assert.assertTrue(pageSummaries.size() == 1);
        PageSummary pageSummary = pageSummaries.get(0);
        Assert.assertEquals(this.fullName, pageSummary.getFullName());
        checkLinks(pageSummary);

        // Get all pages having a document name that contains "WebHome" and a space with an "s" in its name.
        getMethod = executeGet(String.format("%s?name=" + this.pageName + "&space=" + this.fullName.charAt(2),
            buildURI(WikiPagesResource.class, getWiki())));
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        pages = (Pages) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        pageSummaries = pages.getPageSummaries();
        Assert.assertTrue(pageSummaries.size() == 1);
        pageSummary = pageSummaries.get(0);
        Assert.assertEquals(this.fullName, pageSummary.getFullName());
        checkLinks(pageSummary);
    }

    @Test
    public void testAttachments() throws Exception
    {
        this.testUtils.rest().delete(reference);
        this.testUtils.rest().attachFile(new AttachmentReference(getTestClassName() + ".txt", reference),
            new ReaderInputStream(new StringReader("attachment content"), StandardCharsets.UTF_8), true);

        // Verify there are attachments in the whole wiki
        GetMethod getMethod = executeGet(buildURI(WikiAttachmentsResource.class, getWiki()).toString());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Attachments attachments = (Attachments) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        Assert.assertTrue(attachments.getAttachments().size() > 0);

        for (Attachment attachment : attachments.getAttachments()) {
            checkLinks(attachment);
        }

        // Verify we can search for a specific attachment name in the whole wiki
        getMethod = executeGet(
            String.format("%s?name=" + getTestClassName(), buildURI(WikiAttachmentsResource.class, getWiki())));
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        attachments = (Attachments) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        Assert.assertEquals(getAttachmentsInfo(attachments), 1, attachments.getAttachments().size());

        for (Attachment attachment : attachments.getAttachments()) {
            checkLinks(attachment);
        }

        // Verify we can search for all attachments in a given space (sandbox)
        // Also verify that a space can be looked up independtly of its case ("sandbox" will match the "Sandbox" space)
        getMethod = executeGet(
            String.format("%s?space=" + getTestClassName(), buildURI(WikiAttachmentsResource.class, getWiki())));
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        attachments = (Attachments) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        Assert.assertEquals(getAttachmentsInfo(attachments), 1, attachments.getAttachments().size());

        for (Attachment attachment : attachments.getAttachments()) {
            checkLinks(attachment);
        }

        // Verify we can search for an attachment in a given space (sandbox)
        getMethod = executeGet(String.format("%s?name=" + getTestClassName() + "&space=" + getTestClassName(),
            buildURI(WikiAttachmentsResource.class, getWiki())));
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        attachments = (Attachments) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        Assert.assertEquals(getAttachmentsInfo(attachments), 1, attachments.getAttachments().size());

        for (Attachment attachment : attachments.getAttachments()) {
            checkLinks(attachment);
        }
    }

    @Test
    public void testHQLQuerySearch() throws Exception
    {
        this.testUtils.rest().delete(this.reference);
        this.testUtils.rest().savePage(this.reference);

        GetMethod getMethod = executeGet(URIUtil
            .encodeQuery(String.format("%s?q=where doc.name='" + this.pageName + "' order by doc.space desc&type=hql",
                buildURI(WikiSearchQueryResource.class, getWiki()))));
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        int resultSize = searchResults.getSearchResults().size();
        Assert.assertEquals(1, resultSize);
        Assert.assertEquals(this.fullName, searchResults.getSearchResults().get(0).getPageFullName());
    }

    @Test
    public void testHQLQuerySearchWithClassnameAuthenticated() throws Exception
    {
        GetMethod getMethod = executeGet(
            URIUtil.encodeQuery(String.format(
                "%s?q=where doc.space='XWiki' and doc.name='XWikiPreferences'&type=hql&className=XWiki.XWikiGlobalRights",
                buildURI(WikiSearchQueryResource.class, getWiki()))),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        int resultSize = searchResults.getSearchResults().size();
        Assert.assertEquals(1, resultSize);
        assertNotNull(searchResults.getSearchResults().get(0).getObject());
    }

    @Test
    public void testHQLQuerySearchWithClassnameNotAuthenticated() throws Exception
    {
        GetMethod getMethod = executeGet(URIUtil.encodeQuery(String.format(
            "%s?q=where doc.space='XWiki' and doc.name='XWikiPreferences'&type=hql&className=XWiki.XWikiGlobalRights",
            buildURI(WikiSearchQueryResource.class, getWiki()))));
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        int resultSize = searchResults.getSearchResults().size();
        Assert.assertEquals(1, resultSize);
        assertNull(searchResults.getSearchResults().get(0).getObject());
    }

    @Test
    public void testSolrSearch() throws Exception
    {
        this.testUtils.rest().delete(this.reference);
        this.testUtils.rest().savePage(this.reference);

        this.solrUtils.waitEmptyQueue();

        GetMethod getMethod = executeGet(URIUtil.encodeQuery(String.format("%s?q=\"" + this.pageName + "\"&type=solr",
            buildURI(WikiSearchQueryResource.class, getWiki()))));
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        int resultSize = searchResults.getSearchResults().size();
        Assert.assertEquals(1, resultSize);
        Assert.assertEquals(this.fullName, searchResults.getSearchResults().get(0).getPageFullName());
    }

    @Test
    public void testGlobalSearch() throws Exception
    {
        this.testUtils.rest().delete(this.reference);
        this.testUtils.rest().savePage(this.reference);

        // Wait for the Solr queue to be empty
        this.solrUtils.waitEmptyQueue();

        String query = String.format("%s?q=\"%s\"", buildURI(WikisSearchQueryResource.class, getWiki()), this.pageName);
        // Even if the Solr queue appear to be empty we also make sure to wait for the number of results we expect, in
        // case there is some race condition on server side
        SearchResults searchResults = this.testUtils.getDriver().waitUntilCondition(d -> search(1, query));

        Assert.assertEquals(this.fullName, searchResults.getSearchResults().get(0).getPageFullName());
    }

    @Test
    public void testImportXAR() throws Exception
    {
        InputStream is = this.getClass().getResourceAsStream("/Main.Foo.xar");
        String wiki = getWiki();

        PostMethod postMethod = executePost(buildURI(WikiResource.class, wiki).toString(), is,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpMethodInfo(postMethod), HttpStatus.SC_OK, postMethod.getStatusCode());

        GetMethod getMethod = executeGet(buildURI(PageResource.class, wiki, Arrays.asList("Main"), "Foo").toString(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        Assert.assertEquals(wiki, page.getWiki());
        Assert.assertEquals("Main", page.getSpace());
        Assert.assertEquals("Foo", page.getName());
        Assert.assertEquals("Foo", page.getContent());
    }

    @Test
    public void testAttachmentsNumberParameter() throws Exception
    {
        // Setup: Ensure at least 2 attachments exist
        this.testUtils.rest().delete(this.reference);

        try {
            this.testUtils.rest().attachFile(new AttachmentReference(getTestClassName() + "1.txt", this.reference),
                new ByteArrayInputStream("attachment content 1".getBytes(StandardCharsets.UTF_8)), true);
            this.testUtils.rest().attachFile(new AttachmentReference(getTestClassName() + "2.txt", this.reference),
                new ByteArrayInputStream("attachment content 2".getBytes(StandardCharsets.UTF_8)), true);

            // Test: number=-1 should return error
            GetMethod getMethod = executeGet(
                String.format("%s?number=-1", buildURI(WikiAttachmentsResource.class, getWiki())));
            Assert.assertEquals(400, getMethod.getStatusCode());
            Assert.assertEquals(INVALID_LIMIT_MINUS_1, getMethod.getResponseBodyAsString());

            // Test: number=1001 should return error
            getMethod = executeGet(
                String.format("%s?number=1001", buildURI(WikiAttachmentsResource.class, getWiki())));
            Assert.assertEquals(400, getMethod.getStatusCode());
            Assert.assertEquals(INVALID_LIMIT_1001, getMethod.getResponseBodyAsString());

            // Test: pagination with number=1
            getMethod = executeGet(
                String.format("%s?number=1", buildURI(WikiAttachmentsResource.class, getWiki())));
            Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());
            Attachments attachments = (Attachments) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            Assert.assertEquals(1, attachments.getAttachments().size());

            String firstName = attachments.getAttachments().get(0).getName();

            // Test: pagination with number=1 and start=1
            getMethod = executeGet(
                String.format("%s?number=1&start=1", buildURI(WikiAttachmentsResource.class, getWiki())));
            Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());
            attachments = (Attachments) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            Assert.assertEquals(1, attachments.getAttachments().size());
            // Check that we got a different attachment
            Assert.assertNotEquals(firstName, attachments.getAttachments().get(0).getName());
        } finally {
            // Clean up attachments
            this.testUtils.rest().delete(this.reference);
        }
    }

    @Test
    public void testPagesNumberParameter() throws Exception
    {
        // Setup: Ensure at least 2 pages exist
        DocumentReference ref1 = new DocumentReference(this.wikiName, this.spaces, this.pageName + "1");
        DocumentReference ref2 = new DocumentReference(this.wikiName, this.spaces, this.pageName + "2");
        try {
            this.testUtils.rest().delete(ref1);
            this.testUtils.rest().delete(ref2);
            this.testUtils.rest().savePage(ref1, "content1", "title1");
            this.testUtils.rest().savePage(ref2, "content2", "title2");

            // Test: number=-1 should return error
            GetMethod getMethod = executeGet(
                String.format("%s?number=-1", buildURI(WikiPagesResource.class, getWiki())));
            Assert.assertEquals(400, getMethod.getStatusCode());
            Assert.assertEquals(INVALID_LIMIT_MINUS_1, getMethod.getResponseBodyAsString());

            // Test: number=1001 should return error
            getMethod = executeGet(
                String.format("%s?number=1001", buildURI(WikiPagesResource.class, getWiki())));
            Assert.assertEquals(400, getMethod.getStatusCode());
            Assert.assertEquals(INVALID_LIMIT_1001, getMethod.getResponseBodyAsString());

            // Test: pagination with number=1
            getMethod = executeGet(
                String.format("%s?number=1", buildURI(WikiPagesResource.class, getWiki())));
            Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());
            Pages pages = (Pages) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            Assert.assertEquals(1, pages.getPageSummaries().size());

            String firstName = pages.getPageSummaries().get(0).getName();

            // Test: pagination with number=1 and start=1
            getMethod = executeGet(
                String.format("%s?number=1&start=1", buildURI(WikiPagesResource.class, getWiki())));
            Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());
            pages = (Pages) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            Assert.assertEquals(1, pages.getPageSummaries().size());
            // Check that we got a different page
            Assert.assertNotEquals(firstName, pages.getPageSummaries().get(0).getName());
        } finally {
            // Clean up pages
            this.testUtils.rest().delete(ref1);
            this.testUtils.rest().delete(ref2);
        }
    }

    @Test
    public void testWikiChildrenLimitParameter() throws Exception
    {
        // Setup: Ensure at least 2 top-level pages exist
        DocumentReference ref1 = new DocumentReference(this.wikiName, List.of("ChildSpace1"), "WebHome");
        DocumentReference ref2 = new DocumentReference(this.wikiName, List.of("ChildSpace2"), "WebHome");
        try {
            this.testUtils.rest().delete(ref1);
            this.testUtils.rest().delete(ref2);
            this.testUtils.rest().savePage(ref1, "content1", "title1");
            this.testUtils.rest().savePage(ref2, "content2", "title2");

            // Test: limit=-1 should return error.
            GetMethod getMethod = executeGet("%s?limit=-1".formatted(buildURI(WikiChildrenResource.class, getWiki())));
            Assert.assertEquals(400, getMethod.getStatusCode());
            Assert.assertEquals(INVALID_LIMIT_MINUS_1, getMethod.getResponseBodyAsString());

            // Test: limit=1001 should return error.
            getMethod = executeGet("%s?limit=1001".formatted(buildURI(WikiChildrenResource.class, getWiki())));
            Assert.assertEquals(400, getMethod.getStatusCode());
            Assert.assertEquals(INVALID_LIMIT_1001, getMethod.getResponseBodyAsString());

            // Test: pagination with limit=1.
            getMethod = executeGet("%s?limit=1".formatted(buildURI(WikiChildrenResource.class, getWiki())));
            Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());
            Pages pages = (Pages) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            Assert.assertEquals(1, pages.getPageSummaries().size());
            Assert.assertEquals("ChildSpace1.WebHome", pages.getPageSummaries().get(0).getFullName());

            // Test: pagination with limit=1 and offset=1.
            getMethod = executeGet("%s?limit=1&offset=1".formatted(buildURI(WikiChildrenResource.class, getWiki())));
            Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());
            pages = (Pages) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            Assert.assertEquals(1, pages.getPageSummaries().size());
            Assert.assertEquals("ChildSpace2.WebHome", pages.getPageSummaries().get(0).getFullName());
        } finally {
            // Clean up pages
            this.testUtils.rest().delete(ref1);
            this.testUtils.rest().delete(ref2);
        }
    }

    @Test
    public void testWikiSearchNumberParameter() throws Exception
    {
        // Setup: Ensure at least 2 pages exist for search
        DocumentReference ref1 = new DocumentReference(this.wikiName, this.spaces, this.pageName + "A");
        DocumentReference ref2 = new DocumentReference(this.wikiName, this.spaces, this.pageName + "B");
        try {
            this.testUtils.rest().delete(ref1);
            this.testUtils.rest().delete(ref2);
            this.testUtils.rest().savePage(ref1, "searchcontent", "searchtitleA");
            this.testUtils.rest().savePage(ref2, "searchcontent", "searchtitleB");

            this.solrUtils.waitEmptyQueue();

            // Test: number=-1 should return error
            GetMethod getMethod = executeGet(
                "%s?q=searchcontent&number=-1".formatted(buildURI(WikiSearchResource.class, getWiki())));
            Assert.assertEquals(400, getMethod.getStatusCode());
            Assert.assertEquals(INVALID_LIMIT_MINUS_1, getMethod.getResponseBodyAsString());

            // Test: number=1001 should return error
            getMethod = executeGet(
                "%s?q=searchcontent&number=1001".formatted(buildURI(WikiSearchResource.class, getWiki())));
            Assert.assertEquals(400, getMethod.getStatusCode());
            Assert.assertEquals(INVALID_LIMIT_1001, getMethod.getResponseBodyAsString());

            // Test: pagination with number=1
            getMethod = executeGet(
                "%s?q=searchcontent&number=1&scope=content".formatted(buildURI(WikiSearchResource.class, getWiki())));
            Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());
            SearchResults results = (SearchResults) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            Assert.assertEquals(1, results.getSearchResults().size());
            Assert.assertEquals(ref1.getName(), results.getSearchResults().get(0).getPageName());

            // Test: pagination with number=1 and start=1
            getMethod = executeGet(
                "%s?q=searchcontent&number=1&start=1&scope=content".formatted(
                    buildURI(WikiSearchResource.class, getWiki())));
            Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());
            results = (SearchResults) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            Assert.assertEquals(1, results.getSearchResults().size());
            Assert.assertEquals(ref2.getName(), results.getSearchResults().get(0).getPageName());
        } finally {
            this.testUtils.rest().delete(ref1);
            this.testUtils.rest().delete(ref2);
        }
    }

    @Test
    public void testWikiSearchQueryNumberParameter() throws Exception
    {
        // Setup: Ensure at least 2 pages exist for query search
        DocumentReference ref1 = new DocumentReference(this.wikiName, this.spaces, this.pageName + "Q1");
        DocumentReference ref2 = new DocumentReference(this.wikiName, this.spaces, this.pageName + "Q2");
        try {
            this.testUtils.rest().delete(ref1);
            this.testUtils.rest().delete(ref2);
            this.testUtils.rest().savePage(ref1, "querycontent", "querytitle1");
            this.testUtils.rest().savePage(ref2, "querycontent", "querytitle2");

            this.solrUtils.waitEmptyQueue();

            // Test: number=-1 should return error
            GetMethod getMethod = executeGet(
                "%s?q=querycontent1&number=-1".formatted(buildURI(WikiSearchQueryResource.class, getWiki())));
            Assert.assertEquals(400, getMethod.getStatusCode());
            Assert.assertEquals(INVALID_LIMIT_MINUS_1, getMethod.getResponseBodyAsString());

            // Test: number=1001 should return error
            getMethod = executeGet(
                "%s?q=querycontent1&number=1001".formatted(buildURI(WikiSearchQueryResource.class, getWiki())));
            Assert.assertEquals(400, getMethod.getStatusCode());
            Assert.assertEquals(INVALID_LIMIT_1001, getMethod.getResponseBodyAsString());

            // Test: pagination with number=1
            getMethod = executeGet(
                "%s?q=querycontent&number=1&type=solr".formatted(buildURI(WikiSearchQueryResource.class, getWiki())));
            Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());
            SearchResults results = (SearchResults) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            Assert.assertEquals(1, results.getSearchResults().size());
            Assert.assertEquals(ref1.getName(), results.getSearchResults().get(0).getPageName());

            // Test: pagination with number=1 and start=1
            getMethod = executeGet(
                "%s?q=querycontent&number=1&start=1&type=solr".formatted(buildURI(WikiSearchQueryResource.class,
                    getWiki())));
            Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());
            results = (SearchResults) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            Assert.assertEquals(1, results.getSearchResults().size());
            Assert.assertEquals(ref2.getName(), results.getSearchResults().get(0).getPageName());
        } finally {
            this.testUtils.rest().delete(ref1);
            this.testUtils.rest().delete(ref2);
        }
    }
}
