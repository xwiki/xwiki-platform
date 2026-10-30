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
package org.xwiki.flamingo.test.docker;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.ScreenshotComparator;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Verify the styles of the skin by comparing screenshots of the page with reference screenshots committed in the test
 * resources (see {@link ScreenshotComparator}).
 * <p>
 * Each test covers one area of the skin styles, on a page of the {@code SkinVisualIT} space built to show all the
 * cases those styles handle. Those pages are provided by {@code xwiki-platform-flamingo-skin-test-content} and are
 * displayed with the Iceberg color theme and the Font Awesome icon theme, the defaults of XWiki Standard. When a
 * stylesheet is migrated, e.g. from LESS to CSS, generate the reference screenshots with the styles from before the
 * migration, by running the test with {@code -Dxwiki.test.ui.screenshots.update=true}, then check that the test
 * still passes with the migrated styles.
 * <p>
 * This test is not part of {@link AllIT}: it runs on an XWiki instance of its own, because the icon theme has to be
 * configured before the instance starts, while the other tests of the skin rely on the default icon theme.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@UITest(properties = {
    // The icon theme has to be configured before the browser loads any page, because some icons, such as the one of
    // the section edit links, are rendered in the JavaScript of the skin, which the browser then keeps in its cache.
    "xwikiPropertiesAdditionalProperties=iconTheme=IconThemes.FontAwesome"
})
class SkinVisualIT
{
    private static final String SPACE = "SkinVisualIT";

    @BeforeAll
    void beforeAll(TestUtils setup) throws Exception
    {
        // Only the users who can edit the pages see the section edit links.
        setup.loginAsSuperAdmin();
        setup.setWikiPreference("colorTheme", "FlamingoThemes.Iceberg");
    }

    /**
     * Verify the layout of the headings of all levels and of their section edit links, displayed at the end of the
     * heading row, including next to a floated box.
     */
    @Test
    @Order(1)
    void headings(TestUtils setup, ScreenshotComparator screenshots) throws IOException
    {
        ViewPage viewPage = setup.gotoPage(new LocalDocumentReference(SPACE, "Headings"));
        screenshots.assertMatches("headings", viewPage.getContentContainer());
    }

    /**
     * Verify the layout of the vertical menus, with their group headers and items, including the current item, the
     * current group and a group with a single item.
     */
    @Test
    @Order(2)
    void verticalMenus(TestUtils setup, ScreenshotComparator screenshots) throws IOException
    {
        ViewPage viewPage = setup.gotoPage(new LocalDocumentReference(SPACE, "VerticalMenus"));
        screenshots.assertMatches("verticalMenus", viewPage.getContentContainer());
    }
}
