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
import java.util.Collections;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.resources.wikis.WikiResource;
import org.xwiki.rest.resources.wikis.WikisResource;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validate REST API behavior on several wikis.
 *
 * @version $Id$
 */
class WikisIT
{
    @ParameterizedTest
    @WikisSource(mainWiki = false)
    void authenticateOnPathWiki(WikiReference wiki, TestUtils setup) throws Exception
    {
        setup.setCurrentWiki(wiki.getName());

        String user = "user";
        String password = "password";

        // Create a new user
        setup.createUser(user, password, null);

        // Execute a REST request with the right credentials
        setup.setDefaultCredentials(user, password);
        GetMethod get = setup.rest().executeGet(WikiResource.class, wiki.getName());

        try {
            // Make sure the REST request was executed with the right user
            assertEquals(wiki.getName() + ":XWiki." + user, get.getResponseHeader("XWiki-User").getValue());
        } finally {
            get.releaseConnection();
        }
    }

    @Test
    void testImportXAR(TestUtils setup) throws Exception
    {
        // Try with superadmin
        setup.setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);

        try (InputStream is = this.getClass().getResourceAsStream("/Main.Foo.xar")) {
            PostMethod post = setup.rest().executePost(WikiResource.class, is, "xwiki");
            try {
                assertEquals(HttpStatus.SC_OK, post.getStatusCode());
            } finally {
                post.releaseConnection();
            }
        }

        // Try as guest
        setup.setDefaultCredentials(null);

        try (InputStream is = this.getClass().getResourceAsStream("/Main.Foo.xar")) {
            PostMethod post = setup.rest().executePost(WikiResource.class, is, "xwiki");
            try {
                assertEquals(HttpStatus.SC_UNAUTHORIZED, post.getStatusCode());
            } finally {
                post.releaseConnection();
            }
        }

        Page page = setup.rest().get(new LocalDocumentReference("Main", "Foo"));

        assertEquals("xwiki", page.getWiki());
        assertEquals("Main", page.getSpace());
        assertEquals("Foo", page.getName());
        assertEquals("Foo", page.getContent());
    }

    /**
     * Import a XAR uploaded as {@code multipart/form-data} (i.e. as a browser HTML form or {@code curl -F} would send
     * it), which is the scenario of XWIKI-23162. This exercises the whole chain that the multipart body restoration
     * relies on: an upstream filter consumes the request body (through {@code getParameter()}), the Jersey layer
     * restores it from the servlet-cached parts, and the multipart reader extracts the uploaded XAR so that it can be
     * imported. Without the restoration the body reaches the resource empty and nothing is imported.
     */
    @Test
    void testImportXARAsMultipartFormData(TestUtils setup) throws Exception
    {
        setup.setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);

        // Start from a clean state so that a successful assertion really proves that this upload imported the page.
        LocalDocumentReference fooReference = new LocalDocumentReference("Main", "Foo");
        setup.rest().delete(fooReference);

        // A multipart/form-data POST is a "simple" cross-origin request, so a valid CSRF form token is required.
        String formToken;
        GetMethod tokenGet = setup.rest().executeGet(WikisResource.class);
        try {
            assertEquals(HttpStatus.SC_OK, tokenGet.getStatusCode());
            formToken = tokenGet.getResponseHeader("XWiki-Form-Token").getValue();
        } finally {
            tokenGet.releaseConnection();
        }

        byte[] xar;
        try (InputStream is = getClass().getResourceAsStream("/Main.Foo.xar")) {
            xar = IOUtils.toByteArray(is);
        }

        String uri =
            setup.rest().createUri(WikiResource.class, Collections.<String, Object[]>emptyMap(), "xwiki").toString();

        HttpClient httpClient = new HttpClient();
        httpClient.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword()));
        httpClient.getParams().setAuthenticationPreemptive(true);

        PostMethod post = new PostMethod(uri);
        Part[] parts = {new FilePart("file", new ByteArrayPartSource("Main.Foo.xar", xar))};
        post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));
        post.setRequestHeader("XWiki-Form-Token", formToken);
        try {
            httpClient.executeMethod(post);
            assertEquals(HttpStatus.SC_OK, post.getStatusCode());
        } finally {
            post.releaseConnection();
        }

        // The multipart body was restored and its file part extracted, so the XAR was actually imported.
        Page page = setup.rest().get(fooReference);
        assertEquals("xwiki", page.getWiki());
        assertEquals("Main", page.getSpace());
        assertEquals("Foo", page.getName());
        assertEquals("Foo", page.getContent());
    }
}
