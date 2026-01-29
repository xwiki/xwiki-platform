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
package org.xwiki.attachment.test.ui.docker;

import java.io.InputStream;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.resources.wikis.WikiResource;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validate REST API behavior on several wikis.
 *
 * @version $Id$
 */
@UITest
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

        // Switch to superadmin
        setup.setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);

        try (InputStream is = this.getClass().getResourceAsStream("/Main.Foo.xar")) {
            PostMethod post = setup.rest().executePost(WikiResource.class, is, "xwiki");
            try {
                assertEquals(HttpStatus.SC_OK, post.getStatusCode());
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
}
