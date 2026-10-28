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
package org.xwiki.user.rest.test.docker;

import javax.xml.bind.JAXBContext;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.user.rest.model.jaxb.User;
import org.xwiki.user.rest.resources.CurrentUserResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate behavior of the current user REST endpoint.
 *
 * @since 18.2.0RC1
 * @version $Id$
 */
@UITest
class CurrentUserIT
{
    private String baseURL;

    @BeforeAll
    public void setup(TestUtils setup)
    {
        // Create a base URL without any `/rest` suffix.
        this.baseURL = setup.rest().getBaseURL().replaceAll("/(?:rest)?$", "");
    }

    @Test
    @Order(1)
    void testUnauthenticatedUser(TestUtils setup) throws Exception
    {
        setup.forceGuestUser();
        CloseableHttpResponse get = setup.rest().executeGet(CurrentUserResource.class, "xwiki");

        try {
            assertEquals(HttpStatus.SC_OK, get.getCode());

            JAXBContext userContext = JAXBContext.newInstance(User.class);
            User parsedUser = (User) userContext.createUnmarshaller().unmarshal(get.getEntity().getContent());

            assertEquals("XWiki.XWikiGuest", parsedUser.getId());
            assertEquals("Unknown User", parsedUser.getDisplayName());
            assertTrue(parsedUser.getAvatarUrl().startsWith(
                    String.format("%s/resources/icons/xwiki/noavatar.png", this.baseURL)),
                String.format("Avatar should be XWiki's default: <%s/resources/icons/xwiki/noavatar.png> but was <%s>",
                    this.baseURL, parsedUser.getAvatarUrl()));
            assertTrue(parsedUser.isGlobal(), "User should be global.");
        } finally {
            get.close();
        }
    }

    @Test
    @Order(2)
    void testSuperAdmin(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        CloseableHttpResponse get = setup.rest().executeGet(CurrentUserResource.class, "xwiki");

        try {
            assertEquals(HttpStatus.SC_OK, get.getCode());

            JAXBContext userContext = JAXBContext.newInstance(User.class);
            User parsedUser = (User) userContext.createUnmarshaller().unmarshal(get.getEntity().getContent());

            assertEquals("XWiki.superadmin", parsedUser.getId());
            assertEquals("superadmin", parsedUser.getDisplayName());
            assertTrue(parsedUser.getAvatarUrl().startsWith(
                    String.format("%s/resources/icons/xwiki/noavatar.png", this.baseURL)),
                String.format("Avatar should be XWiki's default: <%s/resources/icons/xwiki/noavatar.png> but was <%s>",
                    this.baseURL, parsedUser.getAvatarUrl()));
            assertTrue(parsedUser.isGlobal(), "User should be global.");
        } finally {
            get.close();
        }
    }

    @Test
    @Order(3)
    void testAuthenticatedUser(TestUtils setup) throws Exception
    {
        String user = "user";
        String password = "password";

        setup.createUserAndLogin(user, password);

        CloseableHttpResponse get = setup.rest().executeGet(CurrentUserResource.class, "xwiki");

        try {
            assertEquals(HttpStatus.SC_OK, get.getCode());

            JAXBContext userContext = JAXBContext.newInstance(User.class);
            User parsedUser = (User) userContext.createUnmarshaller().unmarshal(get.getEntity().getContent());

            assertEquals("xwiki:XWiki.user", parsedUser.getId());
            assertEquals("user", parsedUser.getDisplayName());
            assertTrue(parsedUser.getAvatarUrl().startsWith(
                    String.format("%s/resources/icons/xwiki/noavatar.png", this.baseURL)),
                String.format("Avatar should be XWiki's default: <%s/resources/icons/xwiki/noavatar.png> but was <%s>",
                    this.baseURL, parsedUser.getAvatarUrl()));
            assertTrue(parsedUser.isGlobal(), "User should be global.");
        } finally {
            get.close();
        }

        // Cleaning.
        setup.deletePage("XWiki", user);
    }
}
