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
package org.xwiki.test.ui.tag;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.tag.test.po.AddTagsPane;
import org.xwiki.tag.test.po.TagPage;
import org.xwiki.tag.test.po.TaggablePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Several tests for adding and removing tags to/from a wiki page.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@UITest
public class AddRemoveTagsTest
{
    // @Rule
    // public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(testUtils);

    /**
     * The test page.
     */
    private TaggablePage taggablePage;

    private TagPage tagPage;

    @BeforeEach
    void setUp(TestUtils testUtils, TestReference testReference)
    {
        // Create a new test page.
        testUtils.deletePage(testReference);
        testUtils.createPage(testReference, null, null);
        this.taggablePage = new TaggablePage();
    }

    /**
     * Adds and removes a tag.
     */
    @Test
    @Order(1)
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    void testAddRemoveTag()
    {
        String tag = RandomStringUtils.randomAlphanumeric(4);
        assertFalse(this.taggablePage.hasTag(tag));
        AddTagsPane addTagsPane = this.taggablePage.addTags();
        addTagsPane.setTags(tag);
        assertTrue(addTagsPane.add());
        assertTrue(this.taggablePage.hasTag(tag));
        this.taggablePage.removeTag(tag);
        assertFalse(this.taggablePage.hasTag(tag));
    }

    /**
     * Open the add tag panel, cancel then open again the add tag panel and add a new tag.
     */
    @Test
    @Order(2)
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    void testCancelAddTag()
    {
        String firstTag = RandomStringUtils.randomAlphanumeric(4);
        assertFalse(this.taggablePage.hasTag(firstTag));
        AddTagsPane addTagsPane = this.taggablePage.addTags();
        addTagsPane.setTags(firstTag);
        addTagsPane.cancel();

        String secondTag = RandomStringUtils.randomAlphanumeric(4);
        assertFalse(this.taggablePage.hasTag(secondTag));
        addTagsPane = this.taggablePage.addTags();
        addTagsPane.setTags(secondTag);
        assertTrue(addTagsPane.add());
        assertTrue(this.taggablePage.hasTag(secondTag));
        assertFalse(this.taggablePage.hasTag(firstTag));
    }

    /**
     * Add many tags and remove one of them.
     */
    @Test
    @Order(3)
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177") })
    void testAddManyRemoveOneTag()
    {
        String firstTag = RandomStringUtils.randomAlphanumeric(4);
        assertFalse(this.taggablePage.hasTag(firstTag));
        String secondTag = RandomStringUtils.randomAlphanumeric(4);
        assertFalse(this.taggablePage.hasTag(secondTag));

        AddTagsPane addTagsPane = this.taggablePage.addTags();
        addTagsPane.setTags(firstTag + "," + secondTag);
        assertTrue(addTagsPane.add());
        assertTrue(this.taggablePage.hasTag(firstTag));
        assertTrue(this.taggablePage.hasTag(secondTag));

        assertTrue(this.taggablePage.removeTag(firstTag));
        assertTrue(this.taggablePage.hasTag(secondTag));
    }

    /**
     * Tests that a tag can't be added twice to the same page.
     */
    @Test
    @Order(4)
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177") })
    void testAddExistingTag()
    {
        String tag = RandomStringUtils.randomAlphanumeric(4);
        assertFalse(this.taggablePage.hasTag(tag));
        AddTagsPane addTagsPane = this.taggablePage.addTags();
        addTagsPane.setTags(tag);
        assertTrue(addTagsPane.add());
        assertTrue(this.taggablePage.hasTag(tag));

        addTagsPane = this.taggablePage.addTags();
        addTagsPane.setTags(tag);
        assertFalse(addTagsPane.add());
        addTagsPane.cancel();
    }

    /**
     * Add a tag that contains the pipe character, which is used to separate stored tags.
     */
    @Test
    @Order(5)
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    void testAddTagContainingPipe(TestUtils testUtils, TestReference testReference)
    {
        String tag = RandomStringUtils.randomAlphanumeric(3) + "|" + RandomStringUtils.randomAlphanumeric(3);
        assertFalse(this.taggablePage.hasTag(tag));
        AddTagsPane addTagsPane = this.taggablePage.addTags();
        addTagsPane.setTags(tag);
        assertTrue(addTagsPane.add());
        assertTrue(this.taggablePage.hasTag(tag));

        // Reload the page and test again.
        testUtils.gotoPage(testReference);
        this.taggablePage = new TaggablePage();
        assertTrue(this.taggablePage.hasTag(tag));
    }

    /**
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-3843">XWIKI-3843</a>: Strip leading and trailing white
     *      spaces to tags when white space is not the separator
     */
    @Test
    @Order(6)
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    void testStripLeadingAndTrailingSpacesFromTags()
    {
        String firstTag = RandomStringUtils.randomAlphanumeric(4);
        assertFalse(this.taggablePage.hasTag(firstTag));
        String secondTag = RandomStringUtils.randomAlphanumeric(4);
        assertFalse(this.taggablePage.hasTag(secondTag));

        AddTagsPane addTagsPane = this.taggablePage.addTags();
        addTagsPane.setTags("   " + firstTag + " ,  " + secondTag + "    ");
        assertTrue(addTagsPane.add());
        assertTrue(this.taggablePage.hasTag(firstTag));
        assertTrue(this.taggablePage.hasTag(secondTag));
    }

    /**
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-6549">XWIKI-6549</a>: Prevent adding new tags that are
     *      equal ignoring case with existing tags
     */
    @Test
    @Order(7)
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    void testTagCaseIsIgnored()
    {
        String firstTag = "taG1";
        assertFalse(this.taggablePage.hasTag(firstTag));
        // Second tag is same as first tag but with different uppercase/lowercase chars.
        String secondTag = "Tag1";
        assertFalse(this.taggablePage.hasTag(secondTag));
        String thirdTag = "tag3";
        assertFalse(this.taggablePage.hasTag(thirdTag));

        AddTagsPane addTagsPane = this.taggablePage.addTags();
        addTagsPane.setTags(firstTag + "," + thirdTag + "," + secondTag);
        assertTrue(addTagsPane.add());
        assertTrue(this.taggablePage.hasTag(firstTag));
        assertFalse(this.taggablePage.hasTag(secondTag));
        assertTrue(this.taggablePage.hasTag(thirdTag));
    }

    @Test
    @Order(8)
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177") })
    void addAndRenameTagFromTagPage()
    {
        String tag = "MyTag";
        AddTagsPane addTagsPane = this.taggablePage.addTags();
        addTagsPane.setTags(tag);
        assertTrue(addTagsPane.add());
        assertTrue(this.taggablePage.hasTag(tag));
        this.tagPage = this.taggablePage.clickOnTag(tag);
        this.tagPage.clickRenameButton();
        this.tagPage.setNewTagName("MyTagRenamed");
        this.tagPage.clickConfirmRenameTagButton();
        assertTrue(this.tagPage.hasTagHighlight("MyTagRenamed"));
    }

    @Test
    @Order(9)
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177") })
    void addAndDeleteTagFromTagPage()
    {
        String tag = "MyTagToBeDeleted";
        AddTagsPane addTagsPane = this.taggablePage.addTags();
        addTagsPane.setTags(tag);
        assertTrue(addTagsPane.add());
        assertTrue(this.taggablePage.hasTag(tag));
        this.tagPage = this.taggablePage.clickOnTag(tag);
        this.tagPage.clickDeleteButton();
        this.tagPage.clickConfirmDeleteTag();
        assertTrue(this.tagPage.hasConfirmationMessage(tag));
    }
}
