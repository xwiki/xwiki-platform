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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.like.test.po.LikeButton;
import org.xwiki.like.test.po.UserProfileLikedPagesPage;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.like.test.po.UserProfileLikedPagesPage.LIKES_COLUMN_NAME;
import static org.xwiki.like.test.po.UserProfileLikedPagesPage.TITLE_COLUMN_NAME;

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
class LikeIT
{
    private static final String USER1 = "LikeUser1";
    private static final String USER2 = "LikeUser2";
    private static final DocumentReference LIKE_CONFIGURATION_REFERENCE =
        new DocumentReference("xwiki", asList("XWiki", "Like"), "LikeConfiguration");
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
    void likeUnlikeDefaultConfiguration(TestUtils testUtils, TestReference testReference)
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

        // Goes to the user profile and verify that the liked pages tables displays the liked pages. 
        UserProfileLikedPagesPage userProfileLikedPagesPage = new UserProfileLikedPagesPage(USER2);
        userProfileLikedPagesPage.gotoPage();
        TableLayoutElement tableLayout = userProfileLikedPagesPage.getLiveData().getTableLayout();
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertCellWithLink(TITLE_COLUMN_NAME, testUtils.serializeReference(testReference),
            testUtils.getURL(testReference.getLastSpaceReference()));
        tableLayout.assertRow(LIKES_COLUMN_NAME, "2");

        testUtils.login(USER1, USER1);
        testUtils.gotoPage(testReference);
        likeButton = new LikeButton();
        assertTrue(likeButton.isDisplayed());
        assertEquals(2, likeButton.getLikeNumber());
        likeButton.clickToUnlike();
        assertEquals(1, likeButton.getLikeNumber());

        // Check that the value remains after reload
        testUtils.gotoPage(testReference);
        likeButton = new LikeButton();
        assertTrue(likeButton.isDisplayed());
        assertEquals(1, likeButton.getLikeNumber());
    }
}
