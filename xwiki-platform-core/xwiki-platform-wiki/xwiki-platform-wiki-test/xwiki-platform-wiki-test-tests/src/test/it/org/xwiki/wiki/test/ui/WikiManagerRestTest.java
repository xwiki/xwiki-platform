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

import java.io.StringWriter;
import java.util.Random;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.util.URIUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xwiki.rest.model.jaxb.ObjectFactory;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.SearchResult;
import org.xwiki.rest.model.jaxb.SearchResults;
import org.xwiki.rest.model.jaxb.Wiki;
import org.xwiki.rest.model.jaxb.Wikis;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.wikis.WikisResource;
import org.xwiki.rest.resources.wikis.WikisSearchQueryResource;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.wiki.rest.WikiManagerREST;

/**
 * Tests for the Wiki manager REST API.
 *
 * @version $Id$
 * @since 5.4RC1
 */
public class WikiManagerRestTest
{
    protected Random random;

    protected Marshaller marshaller;

    protected Unmarshaller unmarshaller;

    protected ObjectFactory objectFactory;

    protected int port = Integer.valueOf(XWikiExecutor.DEFAULT_PORT);

    private static final String RELATIVE_REST_API_ENTRYPOINT = "/xwiki/rest";

    @Before
    public void setUp() throws Exception
    {
        random = new Random();

        JAXBContext context = JAXBContext.newInstance("org.xwiki.rest.model.jaxb");
        marshaller = context.createMarshaller();
        unmarshaller = context.createUnmarshaller();
        objectFactory = new ObjectFactory();

        // Access once the wiki to make sure the DW is run on main wiki before messing with it
        HttpClient httpClient = new HttpClient();
        GetMethod getMethod = new GetMethod("http://localhost:" + port + "/xwiki/");
        getMethod.setFollowRedirects(true);
        httpClient.executeMethod(getMethod);
    }

    @Test
    public void testCreateWiki() throws Exception
    {
        String WIKI_ID = "foo";

        Wiki wiki = objectFactory.createWiki();
        wiki.setId(WIKI_ID);

        PostMethod postMethod = executePost(getFullUri(WikiManagerREST.class), "superadmin", "pass", wiki);
        Assert.assertEquals(HttpStatus.SC_CREATED, postMethod.getStatusCode());

        wiki = (Wiki) unmarshaller.unmarshal(postMethod.getResponseBodyAsStream());
        Assert.assertEquals(WIKI_ID, wiki.getId());

        GetMethod getMethod = executeGet(getFullUri(WikisResource.class));
        Assert.assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());

        Wikis wikis = (Wikis) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        boolean found = false;
        for (Wiki w : wikis.getWikis()) {
            if (WIKI_ID.equals(w.getId())) {
                found = true;
                break;
            }
        }

        Assert.assertTrue(found);
    }

    //FIXME: Test is disabled for the moment. It works if tested against MySQL but with HSQLDB it seems that the
    // Lucene plugin is not triggered. Anyway this should be better to rewrite it, if possible, as a unit test.
    @Ignore("This test doesn't seem to work correctly with HSQLDB but it actually works if run against MySQL.")
    @Test
    public void testMultiwikiSearch() throws Exception
    {
        String WIKI1_ID = "w1";
        String WIKI2_ID = "w2";
        String PAGE_SPACE = "Main";
        String PAGE_NAME = "Test";
        String PAGE1_STRING = "foo";
        String PAGE2_STRING = "bar";

        Wiki wiki = objectFactory.createWiki();
        wiki.setId(WIKI1_ID);

        PostMethod postMethod = executePost(getFullUri(WikiManagerREST.class), "superadmin", "pass", wiki);
        Assert.assertEquals(HttpStatus.SC_CREATED, postMethod.getStatusCode());

        wiki = objectFactory.createWiki();
        wiki.setId(WIKI2_ID);

        postMethod = executePost(getFullUri(WikiManagerREST.class), "superadmin", "pass", wiki);
        Assert.assertEquals(HttpStatus.SC_CREATED, postMethod.getStatusCode());

        /* Store the page */
        Page page1 = objectFactory.createPage();
        page1.setTitle(PAGE1_STRING);
        page1.setContent(PAGE1_STRING);
        PutMethod putMethod =
                executePut(getUriBuilder(PageResource.class).build(WIKI1_ID, PAGE_SPACE, PAGE_NAME).toString(),
                        "superadmin",
                        "pass", page1);
        Assert.assertEquals(HttpStatus.SC_CREATED, putMethod.getStatusCode());
        page1 = (Page) unmarshaller.unmarshal(putMethod.getResponseBodyAsStream());

        /* Retrieve the page to check that it exists */
        GetMethod getMethod =
                executeGet(getUriBuilder(PageResource.class).build(WIKI1_ID, PAGE_SPACE, PAGE_NAME).toString());
        Assert.assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());
        Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        Assert.assertEquals(WIKI1_ID, page.getWiki());
        Assert.assertEquals(PAGE_SPACE, page.getSpace());
        Assert.assertEquals(PAGE_NAME, page.getName());
        Assert.assertEquals(PAGE1_STRING, page.getTitle());
        Assert.assertEquals(PAGE1_STRING, page.getContent());
        Assert.assertEquals(page1.getCreated(), page.getCreated());
        Assert.assertEquals(page1.getModified(), page.getModified());

        /* Store the page */
        Page page2 = objectFactory.createPage();
        page2.setTitle(PAGE2_STRING);
        page2.setContent(PAGE2_STRING);
        putMethod =
                executePut(getUriBuilder(PageResource.class).build(WIKI2_ID, PAGE_SPACE, PAGE_NAME).toString(),
                        "superadmin",
                        "pass", page2);
        Assert.assertEquals(HttpStatus.SC_CREATED, putMethod.getStatusCode());
        page2 = (Page) unmarshaller.unmarshal(putMethod.getResponseBodyAsStream());

        /* Retrieve the page to check that it exists */
        getMethod = executeGet(getUriBuilder(PageResource.class).build(WIKI2_ID, PAGE_SPACE, PAGE_NAME).toString());
        Assert.assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());
        page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        Assert.assertEquals(WIKI2_ID, page.getWiki());
        Assert.assertEquals(PAGE_SPACE, page.getSpace());
        Assert.assertEquals(PAGE_NAME, page.getName());
        Assert.assertEquals(PAGE2_STRING, page.getTitle());
        Assert.assertEquals(PAGE2_STRING, page.getContent());
        Assert.assertEquals(page2.getCreated(), page.getCreated());
        Assert.assertEquals(page2.getModified(), page.getModified());

        /* Wait a bit that the Lucene Indexer indexes the pages. */
        Thread.sleep(5000);

        getMethod = executeGet(URIUtil.encodeQuery(
                String.format("%s?q=\"%s\"&wikis=w1,w2", getFullUri(WikisSearchQueryResource.class), PAGE_NAME)));
        Assert.assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());

        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        Assert.assertEquals(2, searchResults.getSearchResults().size());

        for (SearchResult searchResult : searchResults.getSearchResults()) {
            Page pageToBeCheckedAgainst = null;
            if (searchResult.getWiki().equals(WIKI1_ID)) {
                pageToBeCheckedAgainst = page1;
            } else {
                pageToBeCheckedAgainst = page2;
            }

            Assert.assertEquals(pageToBeCheckedAgainst.getWiki(), searchResult.getWiki());
            Assert.assertEquals(pageToBeCheckedAgainst.getTitle(), searchResult.getTitle());
            Assert.assertEquals(pageToBeCheckedAgainst.getAuthor(), searchResult.getAuthor());
            Assert.assertEquals(pageToBeCheckedAgainst.getModified(), searchResult.getModified());
            Assert.assertEquals(pageToBeCheckedAgainst.getVersion(), searchResult.getVersion());
        }
    }

    protected String getBaseURL()
    {
        return String.format("http://localhost:%s%s", port, RELATIVE_REST_API_ENTRYPOINT);
    }

    protected UriBuilder getUriBuilder(Class<?> resource)
    {
        return UriBuilder.fromUri(getBaseURL()).path(resource);
    }

    protected String getFullUri(Class<?> resourceClass)
    {
        return String.format("%s%s", getBaseURL(), UriBuilder.fromResource(resourceClass).build());
    }

    protected GetMethod executeGet(String uri) throws Exception
    {
        HttpClient httpClient = new HttpClient();

        GetMethod getMethod = new GetMethod(uri);
        getMethod.addRequestHeader("Accept", MediaType.APPLICATION_XML.toString());
        httpClient.executeMethod(getMethod);

        return getMethod;
    }

    protected PostMethod executePost(String uri, String userName, String password, Object object)
            throws Exception
    {
        HttpClient httpClient = new HttpClient();
        httpClient.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password));
        httpClient.getParams().setAuthenticationPreemptive(true);

        PostMethod postMethod = new PostMethod(uri);
        postMethod.addRequestHeader("Accept", MediaType.APPLICATION_XML.toString());

        StringWriter writer = new StringWriter();
        marshaller.marshal(object, writer);
        RequestEntity entity =
                new StringRequestEntity(writer.toString(), MediaType.APPLICATION_XML.toString(), "UTF-8");

        postMethod.setRequestEntity(entity);

        httpClient.executeMethod(postMethod);

        return postMethod;
    }

    protected PutMethod executePut(String uri, String userName, String password, Object object)
            throws Exception
    {
        HttpClient httpClient = new HttpClient();
        httpClient.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password));
        httpClient.getParams().setAuthenticationPreemptive(true);

        PutMethod putMethod = new PutMethod(uri);
        putMethod.addRequestHeader("Accept", MediaType.APPLICATION_XML.toString());

        StringWriter writer = new StringWriter();
        marshaller.marshal(object, writer);
        RequestEntity entity =
                new StringRequestEntity(writer.toString(), MediaType.APPLICATION_XML.toString(), "UTF-8");

        putMethod.setRequestEntity(entity);

        httpClient.executeMethod(putMethod);

        return putMethod;
    }
}
