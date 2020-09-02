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
package org.xwiki.like.test.ui;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.like.test.po.LikeButton;
import org.xwiki.like.test.po.LikeModal;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UITest(
    properties = {
        // Required for filters preferences
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-eventstream-store-hibernate",
        // The Solr store is not ready yet to be installed as extension
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr"
    }, resolveExtraJARs = true)
public class LikeIT
{
    private static final String USER1 = "LikeUser1";
    private static final String USER2 = "LikeUser2";
    private static final DocumentReference LIKE_CONFIGURATION_REFERENCE =
        new DocumentReference("xwiki", Arrays.asList("XWiki", "Like"), "LikeConfiguration");
    private static final String LIKE_CONFIGURATION_CLASSNAME = "XWiki.Like.LikeConfigurationClass";

    @BeforeEach
    public void setup(TestUtils testUtils)
    {
        testUtils.createUser(USER1, USER1, null);
        testUtils.createUser(USER2, USER2, null);
    }

    private void updateLikeConfiguration(TestUtils testUtils, Object... properties)
    {
        testUtils.updateObject(LIKE_CONFIGURATION_REFERENCE, LIKE_CONFIGURATION_CLASSNAME, 0, properties);
    }

    /**
     * Check that guest user can only see the button if the configuration is set to force displaying it and
     * can never interact with it.
     */
    @Test
    @Order(1)
    void guestUser(TestUtils testUtils, TestReference testReference)
    {
        testUtils.loginAsSuperAdmin();
        testUtils.createPage(testReference, "some content");
        updateLikeConfiguration(testUtils, "alwaysDisplayButton", 0);
        testUtils.forceGuestUser();
        testUtils.gotoPage(testReference);
        LikeButton likeButton = new LikeButton();
        assertFalse(likeButton.isDisplayed());

        testUtils.loginAsSuperAdmin();
        updateLikeConfiguration(testUtils, "alwaysDisplayButton", 1);
        testUtils.forceGuestUser();
        testUtils.gotoPage(testReference);
        likeButton = new LikeButton();
        assertTrue(likeButton.isDisplayed());
        assertFalse(likeButton.canBeClicked());
    }

    @Test
    @Order(2)
    void likeUnlikeDefaultConfiguration(TestUtils testUtils, TestReference testReference) throws Exception
    {
        testUtils.login(USER1, USER1);
        testUtils.createPage(testReference, "some content");
        LikeButton likeButton = new LikeButton();
        assertTrue(likeButton.isDisplayed());
        assertTrue(likeButton.canBeClicked());
        assertEquals(0, likeButton.getLikeNumber());
        likeButton.clickToLike();
        assertEquals(1, likeButton.getLikeNumber());

        testUtils.login(USER2, USER2);
        testUtils.gotoPage(testReference);
        likeButton = new LikeButton();
        assertTrue(likeButton.isDisplayed());
        assertEquals(1, likeButton.getLikeNumber());
        likeButton.clickToLike();
        assertEquals(2, likeButton.getLikeNumber());

        testUtils.login(USER1, USER1);
        testUtils.gotoPage(testReference);
        likeButton = new LikeButton();
        assertTrue(likeButton.isDisplayed());
        assertEquals(2, likeButton.getLikeNumber());
        LikeModal likeModal = likeButton.clickToUnlike();
        assertTrue(likeModal.isDisplayed());
        assertTrue(likeModal.isUnlikeButtonDisplayed());
        likeModal.clickUnlikeButton();
        assertFalse(likeModal.isDisplayed());
        assertEquals(1, likeButton.getLikeNumber());

        // Check that the value remains after reload
        testUtils.gotoPage(testReference);
        likeButton = new LikeButton();
        assertTrue(likeButton.isDisplayed());
        assertEquals(1, likeButton.getLikeNumber());
    }
}
