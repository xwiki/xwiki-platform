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
package com.xpn.xwiki.plugin.webdav.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.webdav.lib.methods.DeleteMethod;
import org.apache.webdav.lib.methods.MkcolMethod;
import org.apache.webdav.lib.methods.MoveMethod;
import org.apache.webdav.lib.methods.PropFindMethod;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The integration test suite for webdav.
 * 
 * @version $Id$
 */
public class XWikiDavITest
{
    /**
     * Root of the webdav server.
     */
    private static final String ROOT_URL = "http://localhost:8080/xwiki/webdav";

    /**
     * The {@link HttpClient} used to invoke various methods on the webdav server.
     */
    private static HttpClient client;

    /**
     * Initializes the http client.
     */
    @BeforeClass
    public static void setUp()
    {
        client = new HttpClient();
        client.getState().setCredentials(
            new AuthScope(AuthScope.ANY_HOST,
                AuthScope.ANY_PORT,
                AuthScope.ANY_REALM,
                AuthScope.ANY_SCHEME), new UsernamePasswordCredentials("Admin", "admin"));
        client.getHttpConnectionManager().getParams().setConnectionTimeout(10000);
    }

    /**
     * Test PROPFIND request on root.
     */
    @Test
    public void testPropFind()
    {
        PropFindMethod propFindMethod = new PropFindMethod(ROOT_URL);
        propFindMethod.setDoAuthentication(true);
        propFindMethod.setDepth(PropFindMethod.DEPTH_1);
        try {
            int status = client.executeMethod(propFindMethod);
            Assert.assertEquals(DavServletResponse.SC_MULTI_STATUS, status);
        } catch (HttpException ex) {
            Assert.fail(ex.getMessage());
        } catch (IOException ex) {
            Assert.fail(ex.getMessage());
        }
    }

    /**
     * Test create and delete space.
     */
    @Test
    public void testCreateAndDeleteSpace()
    {
        String spaceUrl = ROOT_URL + "/pages/TestSpace";
        DeleteMethod deleteMethod = new DeleteMethod();
        deleteMethod.setDoAuthentication(true);
        MkcolMethod mkColMethod = new MkcolMethod();
        mkColMethod.setDoAuthentication(true);
        try {
            deleteMethod.setPath(spaceUrl);
            Assert.assertEquals(DavServletResponse.SC_NO_CONTENT, client
                .executeMethod(deleteMethod));
            mkColMethod.setPath(spaceUrl);
            Assert.assertEquals(DavServletResponse.SC_CREATED, client.executeMethod(mkColMethod));
            deleteMethod.setPath(spaceUrl);
            Assert.assertEquals(DavServletResponse.SC_NO_CONTENT, client
                .executeMethod(deleteMethod));
        } catch (HttpException ex) {
            Assert.fail(ex.getMessage());
        } catch (IOException ex) {
            Assert.fail(ex.getMessage());
        }
    }

    /**
     * Test rename space.
     */
    @Test
    public void testRenameSpace()
    {
        String spaceUrl = ROOT_URL + "/pages/TestSpace";
        String relativeDestinationPath = "/xwiki/webdav/pages/RenamedTestSpace";
        String movedSpaceUrl = ROOT_URL + "/pages/RenamedTestSpace";
        DeleteMethod deleteMethod = new DeleteMethod();
        deleteMethod.setDoAuthentication(true);
        MkcolMethod mkColMethod = new MkcolMethod();
        mkColMethod.setDoAuthentication(true);
        MoveMethod moveMethod = new MoveMethod();
        moveMethod.setDoAuthentication(true);
        try {
            deleteMethod.setPath(spaceUrl);
            Assert.assertEquals(DavServletResponse.SC_NO_CONTENT, client
                .executeMethod(deleteMethod));
            deleteMethod.setPath(movedSpaceUrl);
            Assert.assertEquals(DavServletResponse.SC_NO_CONTENT, client
                .executeMethod(deleteMethod));
            mkColMethod.setPath(spaceUrl);
            Assert.assertEquals(DavServletResponse.SC_CREATED, client.executeMethod(mkColMethod));
            moveMethod.setPath(spaceUrl);
            moveMethod.setDestination(relativeDestinationPath);
            Assert.assertEquals(DavServletResponse.SC_CREATED, client.executeMethod(moveMethod));
            deleteMethod.setPath(movedSpaceUrl);
            Assert.assertEquals(DavServletResponse.SC_NO_CONTENT, client
                .executeMethod(deleteMethod));
        } catch (HttpException ex) {
            Assert.fail(ex.getMessage());
        } catch (IOException ex) {
            Assert.fail(ex.getMessage());
        }
    }

    /**
     * Test create and delete page.
     */
    @Test
    public void testCreateAndDeletePage()
    {
        String spaceUrl = ROOT_URL + "/pages/TestSpace";
        String pageUrl = spaceUrl + "/TestPage";
        DeleteMethod deleteMethod = new DeleteMethod();
        deleteMethod.setDoAuthentication(true);
        MkcolMethod mkColMethod = new MkcolMethod();
        mkColMethod.setDoAuthentication(true);
        try {
            deleteMethod.setPath(spaceUrl);
            Assert.assertEquals(DavServletResponse.SC_NO_CONTENT, client
                .executeMethod(deleteMethod));
            mkColMethod.setPath(spaceUrl);
            Assert.assertEquals(DavServletResponse.SC_CREATED, client.executeMethod(mkColMethod));
            mkColMethod.setPath(pageUrl);
            Assert.assertEquals(DavServletResponse.SC_CREATED, client.executeMethod(mkColMethod));
            deleteMethod.setPath(pageUrl);
            Assert.assertEquals(DavServletResponse.SC_NO_CONTENT, client
                .executeMethod(deleteMethod));
            deleteMethod.setPath(spaceUrl);
            Assert.assertEquals(DavServletResponse.SC_NO_CONTENT, client
                .executeMethod(deleteMethod));
        } catch (HttpException ex) {
            Assert.fail(ex.getMessage());
        } catch (IOException ex) {
            Assert.fail(ex.getMessage());
        }
    }

    /**
     * Test get page content.
     */
    @Test
    public void testGetPageWikiContent()
    {
        String spaceUrl = ROOT_URL + "/pages/TestSpace";
        String pageUrl = spaceUrl + "/TestPage";
        String wikiTextFileUrl = pageUrl + "/wiki.txt";
        String wikiXMLFileUrl = pageUrl + "/wiki.xml";
        DeleteMethod deleteMethod = new DeleteMethod();
        deleteMethod.setDoAuthentication(true);
        MkcolMethod mkColMethod = new MkcolMethod();
        mkColMethod.setDoAuthentication(true);
        GetMethod getMethod = new GetMethod();
        getMethod.setDoAuthentication(true);
        try {
            deleteMethod.setPath(spaceUrl);
            Assert.assertEquals(DavServletResponse.SC_NO_CONTENT, client
                .executeMethod(deleteMethod));
            mkColMethod.setPath(spaceUrl);
            Assert.assertEquals(DavServletResponse.SC_CREATED, client.executeMethod(mkColMethod));
            mkColMethod.setPath(pageUrl);
            Assert.assertEquals(DavServletResponse.SC_CREATED, client.executeMethod(mkColMethod));
            getMethod.setPath(wikiTextFileUrl);
            Assert.assertEquals(DavServletResponse.SC_OK, client.executeMethod(getMethod));
            Assert.assertTrue(getMethod.getResponseBodyAsStream().read() != -1);
            getMethod.setPath(wikiXMLFileUrl);
            Assert.assertEquals(DavServletResponse.SC_OK, client.executeMethod(getMethod));
            Assert.assertTrue(getMethod.getResponseBodyAsStream().read() != -1);
            deleteMethod.setPath(pageUrl);
            Assert.assertEquals(DavServletResponse.SC_NO_CONTENT, client
                .executeMethod(deleteMethod));
            deleteMethod.setPath(spaceUrl);
            Assert.assertEquals(DavServletResponse.SC_NO_CONTENT, client
                .executeMethod(deleteMethod));
        } catch (HttpException ex) {
            Assert.fail(ex.getMessage());
        } catch (IOException ex) {
            Assert.fail(ex.getMessage());
        }
    }

    /**
     * Test update page content.
     */
    @Test
    public void testUpdatePageWikiContent()
    {
        String spaceUrl = ROOT_URL + "/pages/TestSpace";
        String pageUrl = spaceUrl + "/TestPage";
        String wikiTextFileUrl = pageUrl + "/wiki.txt";
        String wikiXMLFileUrl = pageUrl + "/wiki.xml";
        String newContent = "New Content";
        DeleteMethod deleteMethod = new DeleteMethod();
        deleteMethod.setDoAuthentication(true);
        MkcolMethod mkColMethod = new MkcolMethod();
        mkColMethod.setDoAuthentication(true);
        PutMethod putMethod = new PutMethod();
        putMethod.setDoAuthentication(true);
        GetMethod getMethod = new GetMethod();
        getMethod.setDoAuthentication(true);
        try {
            deleteMethod.setPath(spaceUrl);
            Assert.assertEquals(DavServletResponse.SC_NO_CONTENT, client
                .executeMethod(deleteMethod));
            mkColMethod.setPath(spaceUrl);
            Assert.assertEquals(DavServletResponse.SC_CREATED, client.executeMethod(mkColMethod));
            mkColMethod.setPath(pageUrl);
            Assert.assertEquals(DavServletResponse.SC_CREATED, client.executeMethod(mkColMethod));
            putMethod.setPath(wikiTextFileUrl);
            putMethod
                .setRequestEntity(new InputStreamRequestEntity(new ByteArrayInputStream(newContent
                    .getBytes())));
            // Already existing resource, in which case SC_NO_CONTENT will be the return status.
            Assert
                .assertEquals(DavServletResponse.SC_NO_CONTENT, client.executeMethod(putMethod));
            getMethod.setPath(wikiTextFileUrl);
            Assert.assertEquals(DavServletResponse.SC_OK, client.executeMethod(getMethod));
            Assert.assertEquals(newContent, getMethod.getResponseBodyAsString());
            putMethod.setPath(wikiXMLFileUrl);
            putMethod
                .setRequestEntity(new InputStreamRequestEntity(new ByteArrayInputStream(newContent
                    .getBytes())));
            // XML saving is not allowed, should return BAD_REQUEST status.
            Assert.assertEquals(DavServletResponse.SC_METHOD_NOT_ALLOWED, client
                .executeMethod(putMethod));
            deleteMethod.setPath(pageUrl);
            Assert.assertEquals(DavServletResponse.SC_NO_CONTENT, client
                .executeMethod(deleteMethod));
            deleteMethod.setPath(spaceUrl);
            Assert.assertEquals(DavServletResponse.SC_NO_CONTENT, client
                .executeMethod(deleteMethod));
        } catch (HttpException ex) {
            Assert.fail(ex.getMessage());
        } catch (IOException ex) {
            Assert.fail(ex.getMessage());
        }
    }

    /**
     * Test making attachment.
     */
    @Test
    public void testMakingAttachment()
    {
        String spaceUrl = ROOT_URL + "/pages/TestSpace";
        String pageUrl = spaceUrl + "/TestPage";
        String attachmentUrl = pageUrl + "/attachment.txt";
        String attachmentContent = "Attachment Content";
        DeleteMethod deleteMethod = new DeleteMethod();
        deleteMethod.setDoAuthentication(true);
        MkcolMethod mkColMethod = new MkcolMethod();
        mkColMethod.setDoAuthentication(true);
        PutMethod putMethod = new PutMethod();
        putMethod.setDoAuthentication(true);
        GetMethod getMethod = new GetMethod();
        getMethod.setDoAuthentication(true);
        try {
            deleteMethod.setPath(spaceUrl);
            Assert.assertEquals(DavServletResponse.SC_NO_CONTENT, client
                .executeMethod(deleteMethod));
            mkColMethod.setPath(spaceUrl);
            Assert.assertEquals(DavServletResponse.SC_CREATED, client.executeMethod(mkColMethod));
            mkColMethod.setPath(pageUrl);
            Assert.assertEquals(DavServletResponse.SC_CREATED, client.executeMethod(mkColMethod));
            getMethod.setPath(attachmentUrl);
            Assert.assertEquals(DavServletResponse.SC_NOT_FOUND, client.executeMethod(getMethod));
            putMethod.setPath(attachmentUrl);
            putMethod
                .setRequestEntity(new InputStreamRequestEntity(new ByteArrayInputStream(attachmentContent
                    .getBytes())));
            Assert.assertEquals(DavServletResponse.SC_CREATED, client.executeMethod(putMethod));
            getMethod.setPath(attachmentUrl);
            Assert.assertEquals(DavServletResponse.SC_OK, client.executeMethod(getMethod));
            Assert.assertEquals(attachmentContent, getMethod.getResponseBodyAsString());
            deleteMethod.setPath(attachmentUrl);
            Assert.assertEquals(DavServletResponse.SC_NO_CONTENT, client
                .executeMethod(deleteMethod));
            deleteMethod.setPath(pageUrl);
            Assert.assertEquals(DavServletResponse.SC_NO_CONTENT, client
                .executeMethod(deleteMethod));
            deleteMethod.setPath(spaceUrl);
            Assert.assertEquals(DavServletResponse.SC_NO_CONTENT, client
                .executeMethod(deleteMethod));
        } catch (HttpException ex) {
            Assert.fail(ex.getMessage());
        } catch (IOException ex) {
            Assert.fail(ex.getMessage());
        }
    }
}
