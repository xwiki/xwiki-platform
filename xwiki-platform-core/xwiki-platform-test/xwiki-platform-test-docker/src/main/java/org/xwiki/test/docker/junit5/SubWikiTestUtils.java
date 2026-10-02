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
package org.xwiki.test.docker.junit5;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.internal.converter.ExtensionIdConverter;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.internal.junit5.DockerTestUtils;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.wiki.user.MembershipType;
import org.xwiki.wiki.user.UserScope;

/**
 * Utility methods to set up subwikis in functional tests: create a subwiki (much faster than going through the wiki
 * creation wizard) and configure its membership type and user scope.
 * <p>
 * Inject it as a test (or {@code @BeforeAll}) method parameter.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class SubWikiTestUtils
{
    private static final String WIKI_NAMESPACE_PREFIX = "wiki:";

    private static final String WIKI_MANAGER_SPACE = "WikiManager";

    private static final String WIKI_USER_CLASS = "WikiManager.WikiUserClass";

    private final TestUtils setup;

    private final ExtensionContext context;

    /**
     * @param setup the test setup
     * @param context the context of the test, used to reach the running XWiki instance
     */
    public SubWikiTestUtils(TestUtils setup, ExtensionContext context)
    {
        this.setup = setup;
        this.context = context;
    }

    /**
     * Create a subwiki, owned by superadmin, and install the given extensions on it. Does nothing if the wiki already
     * exists, except installing the extensions that are missing.
     *
     * @param wikiId the identifier of the wiki to create (also used as its pretty name and alias)
     * @param extensions the extensions to install on the wiki, as {@code groupId:artifactId[:version]} coordinates
     *     (e.g. {@code org.xwiki.platform:xwiki-platform-wiki-ui-wiki} to get the same wiki as the one created by the
     *     wiki creation wizard with the default subwiki flavor)
     * @throws Exception when failing to create the wiki or to install the extensions
     */
    public void createWiki(String wikiId, String... extensions) throws Exception
    {
        DockerTestUtils.getWikiCreator(this.context).createWiki(TestUtils.SUPER_ADMIN_CREDENTIALS, wikiId, false);

        if (extensions.length > 0) {
            Set<ExtensionId> extensionIds = new LinkedHashSet<>(extensions.length);
            for (String coordinate : extensions) {
                extensionIds.add(ExtensionIdConverter.toExtensionId(coordinate, null));
            }
            DockerTestUtils.getExtensionInstaller(this.context).installExtensions(extensionIds,
                TestUtils.SUPER_ADMIN_CREDENTIALS, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
                List.of(WIKI_NAMESPACE_PREFIX + wikiId), false);
        }
    }

    /**
     * Set how users can join the given subwiki. The current user must be allowed to edit the
     * {@code WikiManager.WikiUserConfiguration} page of that wiki (e.g. superadmin).
     *
     * @param wikiId the identifier of the subwiki
     * @param membershipType the membership type to set
     */
    public void setMembershipType(String wikiId, MembershipType membershipType)
    {
        setWikiUserConfiguration(wikiId, "membershipType", membershipType.name());
    }

    /**
     * Set which users (global, local or both) can be used in the given subwiki. The current user must be allowed to
     * edit the {@code WikiManager.WikiUserConfiguration} page of that wiki (e.g. superadmin).
     *
     * @param wikiId the identifier of the subwiki
     * @param userScope the user scope to set
     */
    public void setUserScope(String wikiId, UserScope userScope)
    {
        setWikiUserConfiguration(wikiId, "userScope", userScope.name());
    }

    private void setWikiUserConfiguration(String wikiId, String property, String value)
    {
        // The configuration is stored in a WikiManager.WikiUserClass object on the WikiManager.WikiUserConfiguration
        // page of the subwiki itself, and the values of the class static lists are the lower case enum names.
        this.setup.updateObject(new DocumentReference(wikiId, WIKI_MANAGER_SPACE, "WikiUserConfiguration"),
            WIKI_USER_CLASS, 0, property, value.toLowerCase(Locale.ROOT));
    }
}
