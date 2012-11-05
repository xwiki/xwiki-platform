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
package org.xwiki.manager.rest.test;

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
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.manager.rest.resources.WikiManagerResource;
import org.xwiki.rest.model.jaxb.ObjectFactory;
import org.xwiki.rest.model.jaxb.Wiki;
import org.xwiki.rest.model.jaxb.Wikis;
import org.xwiki.rest.resources.wikis.WikisResource;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.AbstractTest;

/**
 * Tests for the Wiki manager REST API.
 *
 * @version $Id$
 * @since 4.3M2
 */
public class WikiManagerRestTest extends AbstractTest
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
    }

    @Test
    public void testCreateWiki() throws Exception
    {
        String WIKI_ID = "foo";

        Wiki wiki = objectFactory.createWiki();
        wiki.setId(WIKI_ID);

        PostMethod postMethod = executePost(getFullUri(WikiManagerResource.class), "superadmin", "pass", wiki);
        Assert.assertEquals(HttpStatus.SC_CREATED, postMethod.getStatusCode());

        wiki = (Wiki) unmarshaller.unmarshal(postMethod.getResponseBodyAsStream());
        Assert.assertEquals(WIKI_ID, wiki.getId());

        GetMethod getMethod = executeGet(getFullUri(WikisResource.class));
        Assert.assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());

        Wikis wikis = (Wikis) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        boolean found = false;
        for(Wiki w : wikis.getWikis()) {
            if(WIKI_ID.equals(w.getId())) {
                found = true;
                break;
            }
        }

        Assert.assertTrue(found);
    }

    protected String getBaseURL()
    {
        return String.format("http://localhost:%s%s", port, RELATIVE_REST_API_ENTRYPOINT);
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
}
