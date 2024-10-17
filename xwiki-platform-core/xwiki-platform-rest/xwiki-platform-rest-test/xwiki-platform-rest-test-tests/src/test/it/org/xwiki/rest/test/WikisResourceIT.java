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

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.eclipse.jetty.util.URIUtil;
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
            CloseableHttpResponse response = executeGet(URIUtil.encodeQuery(query));
            assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

            SearchResults searchResults =
                (SearchResults) this.unmarshaller.unmarshal(response.getEntity().getContent());

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
        CloseableHttpResponse response = executeGet(getFullUri(WikisResource.class));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        Wikis wikis = (Wikis) unmarshaller.unmarshal(response.getEntity().getContent());
        Assert.assertTrue(getHttpResponseInfo(response), wikis.getWikis().size() > 0);

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

        CloseableHttpResponse getMethod = executeGet(
            String.format("%s?scope=name&q=%s", buildURI(WikiSearchResource.class, getWiki()), this.pageName));
        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getEntity().getContent());

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
            String.format("%s?scope=name&q=%s", buildURI(WikiSearchResource.class, getWiki()), this.pageName));
        searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        // Ensure that searching by name finds both terminal and non-terminal page.
        resultSize = searchResults.getSearchResults().size();
        assertEquals(2, resultSize);
        List<String> foundPages =
            searchResults.getSearchResults().stream().map(SearchResult::getPageFullName).collect(Collectors.toList());
        assertTrue(foundPages.contains(this.fullName));
        assertTrue(foundPages.contains(nonTerminalFullName));

        // Ensure that searching by space finds neither the terminal nor the non-terminal page.
        getMethod = executeGet(
            String.format("%s?scope=name&q=%s", buildURI(WikiSearchResource.class, getWiki()), this.spaces.get(0)));
        searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getEntity().getContent());
        assertEquals(0, searchResults.getSearchResults().size());
    }

    @Test
    public void testSearchWikis() throws Exception
    {
        this.testUtils.rest().delete(reference);
        this.testUtils.rest().savePage(reference, "content" + this.pageName, "title" + this.pageName);

        CloseableHttpResponse response =
            executeGet(String.format("%s?q=content%s", buildURI(WikiSearchResource.class, getWiki()), this.pageName));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(response.getEntity().getContent());

        int resultSize = searchResults.getSearchResults().size();
        assertEquals(1, resultSize);

        for (SearchResult searchResult : searchResults.getSearchResults()) {
            checkLinks(searchResult);
        }

        response = executeGet(
            String.format("%s?q=%s&scope=name", buildURI(WikiSearchResource.class, getWiki()), this.pageName));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        searchResults = (SearchResults) unmarshaller.unmarshal(response.getEntity().getContent());

        resultSize = searchResults.getSearchResults().size();
        assertEquals(1, resultSize);

        for (SearchResult searchResult : searchResults.getSearchResults()) {
            checkLinks(searchResult);
        }

        // Search in titles
        response = executeGet(
            String.format("%s?q=title%s&scope=title", buildURI(WikiSearchResource.class, getWiki()), this.pageName));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        searchResults = (SearchResults) unmarshaller.unmarshal(response.getEntity().getContent());

        resultSize = searchResults.getSearchResults().size();
        assertEquals(1, resultSize);

        for (SearchResult searchResult : searchResults.getSearchResults()) {
            checkLinks(searchResult);
        }

        // Search for space names
        response = executeGet(
            String.format("%s?q=%s&scope=spaces", buildURI(WikiSearchResource.class, getWiki()), this.spaces.get(0)));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        searchResults = (SearchResults) unmarshaller.unmarshal(response.getEntity().getContent());

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
        CloseableHttpResponse response =
            executeGet(String.format("%s?q=XWiki.Admin&scope=objects", buildURI(WikiSearchResource.class, getWiki())));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(response.getEntity().getContent());

        int resultSize = searchResults.getSearchResults().size();
        Assert.assertTrue(String.format("Found %s results", resultSize), resultSize == 0);
    }

    @Test
    public void testObjectSearchAuthenticated() throws Exception
    {
        /* Check search for an object containing XWiki.Admin (i.e., the admin profile) */
        CloseableHttpResponse response = executeGet(
            String.format("%s?q=XWiki.XWikiGuest&scope=objects", buildURI(WikiSearchResource.class, getWiki())),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(response.getEntity().getContent());

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
        CloseableHttpResponse response = executeGet(String.format("%s", buildURI(WikiPagesResource.class, getWiki())));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        Pages pages = (Pages) unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertTrue(pages.getPageSummaries().size() > 0);

        for (PageSummary pageSummary : pages.getPageSummaries()) {
            checkLinks(pageSummary);
        }

        // Get all pages having a document name that contains "WebHome" (for all spaces)
        response = executeGet(String.format("%s?name=%s", buildURI(WikiPagesResource.class, getWiki()), this.pageName));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        pages = (Pages) unmarshaller.unmarshal(response.getEntity().getContent());

        List<PageSummary> pageSummaries = pages.getPageSummaries();
        Assert.assertTrue(pageSummaries.size() == 1);
        PageSummary pageSummary = pageSummaries.get(0);
        assertEquals(this.fullName, pageSummary.getFullName());
        checkLinks(pageSummary);

        // Get all pages having a document name that contains "WebHome" and a space with an "s" in its name.
        response = executeGet(String.format("%s?name=%s&space=%s", buildURI(WikiPagesResource.class, getWiki()),
            this.pageName, this.fullName.charAt(2)));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        pages = (Pages) unmarshaller.unmarshal(response.getEntity().getContent());

        pageSummaries = pages.getPageSummaries();
        Assert.assertTrue(pageSummaries.size() == 1);
        pageSummary = pageSummaries.get(0);
        assertEquals(this.fullName, pageSummary.getFullName());
        checkLinks(pageSummary);
    }

    @Test
    public void testAttachments() throws Exception
    {
        this.testUtils.rest().delete(reference);
        this.testUtils.rest().attachFile(new AttachmentReference(getTestClassName() + ".txt", reference),
            new ReaderInputStream(new StringReader("attachment content"), StandardCharsets.UTF_8), true);

        // Verify there are attachments in the whole wiki
        CloseableHttpResponse response = executeGet(buildURI(WikiAttachmentsResource.class, getWiki()));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        Attachments attachments = (Attachments) unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertTrue(attachments.getAttachments().size() > 0);

        for (Attachment attachment : attachments.getAttachments()) {
            checkLinks(attachment);
        }

        // Verify we can search for a specific attachment name in the whole wiki
        response = executeGet(
            String.format("%s?name=%s", buildURI(WikiAttachmentsResource.class, getWiki()), getTestClassName()));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        attachments = (Attachments) unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertEquals(getAttachmentsInfo(attachments), 1, attachments.getAttachments().size());

        for (Attachment attachment : attachments.getAttachments()) {
            checkLinks(attachment);
        }

        // Verify we can search for all attachments in a given space (sandbox)
        // Also verify that a space can be looked up independtly of its case ("sandbox" will match the "Sandbox" space)
        response = executeGet(
            String.format("%s?space=%s", buildURI(WikiAttachmentsResource.class, getWiki()), getTestClassName()));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        attachments = (Attachments) unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertEquals(getAttachmentsInfo(attachments), 1, attachments.getAttachments().size());

        for (Attachment attachment : attachments.getAttachments()) {
            checkLinks(attachment);
        }

        // Verify we can search for an attachment in a given space (sandbox)
        response = executeGet(String.format("%s?name=%s&space=%s",
            buildURI(WikiAttachmentsResource.class, getWiki(), getTestClassName(), getTestClassName())));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        attachments = (Attachments) unmarshaller.unmarshal(response.getEntity().getContent());

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

        CloseableHttpResponse response =
            executeGet(URIUtil.encodeQuery(String.format("%s?q=where doc.name='%s' order by doc.space desc&type=hql",
                buildURI(WikiSearchQueryResource.class, getWiki(), this.pageName))));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(response.getEntity().getContent());

        int resultSize = searchResults.getSearchResults().size();
        assertEquals(1, resultSize);
        Assert.assertEquals(this.fullName, searchResults.getSearchResults().get(0).getPageFullName());
    }

    @Test
    public void testHQLQuerySearchWithClassnameAuthenticated() throws Exception
    {
        CloseableHttpResponse response = executeGet(URIUtil.encodeQuery(String.format(
            "%s?q=where doc.space='XWiki' and doc.name='XWikiPreferences'&type=hql&className=XWiki.XWikiGlobalRights",
            buildURI(WikiSearchQueryResource.class, getWiki()))), TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(response.getEntity().getContent());

        int resultSize = searchResults.getSearchResults().size();
        assertEquals(1, resultSize);
        assertNotNull(searchResults.getSearchResults().get(0).getObject());
    }

    @Test
    public void testHQLQuerySearchWithClassnameNotAuthenticated() throws Exception
    {
        CloseableHttpResponse response = executeGet(URIUtil.encodeQuery(String.format(
            "%s?q=where doc.space='XWiki' and doc.name='XWikiPreferences'&type=hql&className=XWiki.XWikiGlobalRights",
            buildURI(WikiSearchQueryResource.class, getWiki()))));
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(response.getEntity().getContent());

        int resultSize = searchResults.getSearchResults().size();
        assertEquals(1, resultSize);
        assertNull(searchResults.getSearchResults().get(0).getObject());
    }

    @Test
    public void testSolrSearch() throws Exception
    {
        this.testUtils.rest().delete(this.reference);
        this.testUtils.rest().savePage(this.reference);

        this.solrUtils.waitEmptyQueue();

        CloseableHttpResponse getMethod = executeGet(URIUtil.encodeQuery(
            String.format("%s?q=\"%s\"&type=solr", buildURI(WikiSearchQueryResource.class, getWiki()), this.pageName)));
        Assert.assertEquals(getHttpResponseInfo(getMethod), HttpStatus.SC_OK, getMethod.getCode());

        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        int resultSize = searchResults.getSearchResults().size();
        assertEquals(1, resultSize);
        assertEquals(this.fullName, searchResults.getSearchResults().get(0).getPageFullName());
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

        assertEquals(this.fullName, searchResults.getSearchResults().get(0).getPageFullName());
    }

    @Test
    public void testImportXAR() throws Exception
    {
        InputStream is = this.getClass().getResourceAsStream("/Main.Foo.xar");
        String wiki = getWiki();

        CloseableHttpResponse response = executePost(buildURI(WikiResource.class, wiki), is,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        response = executeGet(buildURI(PageResource.class, wiki, Arrays.asList("Main"), "Foo"),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        Page page = (Page) unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertEquals(wiki, page.getWiki());
        Assert.assertEquals("Main", page.getSpace());
        Assert.assertEquals("Foo", page.getName());
        Assert.assertEquals("Foo", page.getContent());
    }
}
