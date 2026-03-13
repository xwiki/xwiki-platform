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

import java.util.Optional;

import javax.xml.bind.JAXBContext;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.jupiter.params.ParameterizedTest;
import org.openqa.selenium.By;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rest.Relations;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.user.rest.model.jaxb.UserSummary;
import org.xwiki.user.rest.model.jaxb.Users;
import org.xwiki.user.rest.resources.UsersResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate behavior of the users REST endpoint.
 *
 * @since 18.2.0RC1
 * @version $Id$
 */
@UITest
class UsersIT
{
    @ParameterizedTest
    @WikisSource(extensions = { "org.xwiki.platform:xwiki-platform-wiki-user-default" })
    void testListUsers(WikiReference wikiReference, TestUtils setup) throws Exception
    {
        String wiki = wikiReference.getName();
        String[] users = new String[] {"user1", "user2", "user3", "user4"};
        String password = "password";

        boolean isSubWiki = !wiki.equals("xwiki");
        setup.setCurrentWiki(wiki);

        // Using super admin to have advanced user mode enabled.
        setup.loginAsSuperAdmin();

        // We enable local users for the subwiki.
        if (isSubWiki) {
            DocumentReference wikiUserConfigurationRef =
                new DocumentReference(wiki, "WikiManager", "WikiUserConfiguration");
            setup.createPage(wikiUserConfigurationRef, "Hello World");
            ViewPage wikiUserConfiguration = setup.gotoPage(wikiUserConfigurationRef);
            ObjectEditPage wikiUserConfigurationObject = wikiUserConfiguration.editObjects();
            wikiUserConfigurationObject.addObject("WikiManager.WikiUserClass")
                .setCheckBox(By.xpath("//input[@value = 'local_only']"), true);
            wikiUserConfigurationObject.clickSaveAndContinue();
        }

        for (String user : users) {
            setup.createUser(user, password, null);
        }

        GetMethod get = setup.rest().executeGet(UsersResource.class, wiki);
        try {
            assertEquals(HttpStatus.SC_OK, get.getStatusCode());

            JAXBContext userContext = JAXBContext.newInstance(Users.class);
            Users parsedUsers = (Users) userContext.createUnmarshaller().unmarshal(get.getResponseBodyAsStream());

            assertEquals(4, parsedUsers.getUserSummaries().size());
            for (int i = 0; i < parsedUsers.getUserSummaries().size(); i++) {
                UserSummary parsedUser = parsedUsers.getUserSummaries().get(i);
                assertEquals(String.format("XWiki.%s", users[i]), parsedUser.getId());
                assertTrue(parsedUser.getAvatarUrl().startsWith(
                    "http://localhost:8080/xwiki/resources/icons/xwiki/noavatar.png"));
                assertEquals(!isSubWiki, parsedUser.isGlobal());

                Optional<Link> pageLink =
                    parsedUser.getLinks().stream().filter(l -> l.getRel().equals(Relations.PAGE)).findFirst();
                assertTrue(pageLink.isPresent());
                assertEquals(
                    String.format("http://localhost:8080/xwiki/rest/wikis/%s/spaces/XWiki/pages/%s", wiki, users[i]),
                    pageLink.get().getHref());

                Optional<Link> userLink =
                    parsedUser.getLinks().stream().filter(l -> l.getRel().equals(Relations.USER)).findFirst();
                assertTrue(userLink.isPresent());
                assertEquals(
                    String.format("http://localhost:8080/xwiki/rest/wikis/%s/users/XWiki.%s", wiki, users[i]),
                    userLink.get().getHref());
            }

        } finally {
            get.releaseConnection();
        }
    }
}
