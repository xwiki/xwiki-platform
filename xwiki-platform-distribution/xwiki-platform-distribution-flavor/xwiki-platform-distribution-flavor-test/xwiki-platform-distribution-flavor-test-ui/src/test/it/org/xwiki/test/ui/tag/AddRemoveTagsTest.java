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

import org.junit.Assert;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.tag.test.po.AddTagsPane;
import org.xwiki.tag.test.po.TaggablePage;
import org.xwiki.tag.test.po.TagPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.AdminAuthenticationRule;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;

/**
 * Several tests for adding and removing tags to/from a wiki page.
 * 
 * @version $Id$
 * @since 3.1M1
 */
public class AddRemoveTagsTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(getUtil());

    /**
     * The test page.
     */
    private TaggablePage taggablePage;
    
    private TagPage tagPage;

    @Before
    public void setUp() throws Exception
    {
        // Create a new test page.
        getUtil().rest().deletePage(getTestClassName(), getTestMethodName());
        getUtil().createPage(getTestClassName(), getTestMethodName(), null, null);
        taggablePage = new TaggablePage();
    }

    /**
     * Adds and removes a tag.
     */
    @Test
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    public void testAddRemoveTag()
    {
        String tag = RandomStringUtils.randomAlphanumeric(4);
        Assert.assertFalse(taggablePage.hasTag(tag));
        AddTagsPane addTagsPane = taggablePage.addTags();
        addTagsPane.setTags(tag);
        Assert.assertTrue(addTagsPane.add());
        Assert.assertTrue(taggablePage.hasTag(tag));
        taggablePage.removeTag(tag);
        Assert.assertFalse(taggablePage.hasTag(tag));
    }

    /**
     * Open the add tag panel, cancel then open again the add tag panel and add a new tag.
     */
    @Test
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    public void testCancelAddTag()
    {
        String firstTag = RandomStringUtils.randomAlphanumeric(4);
        Assert.assertFalse(taggablePage.hasTag(firstTag));
        AddTagsPane addTagsPane = taggablePage.addTags();
        addTagsPane.setTags(firstTag);
        addTagsPane.cancel();

        String secondTag = RandomStringUtils.randomAlphanumeric(4);
        Assert.assertFalse(taggablePage.hasTag(secondTag));
        addTagsPane = taggablePage.addTags();
        addTagsPane.setTags(secondTag);
        Assert.assertTrue(addTagsPane.add());
        Assert.assertTrue(taggablePage.hasTag(secondTag));
        Assert.assertFalse(taggablePage.hasTag(firstTag));
    }

    /**
     * Add many tags and remove one of them.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")})
    public void testAddManyRemoveOneTag()
    {
        String firstTag = RandomStringUtils.randomAlphanumeric(4);
        Assert.assertFalse(taggablePage.hasTag(firstTag));
        String secondTag = RandomStringUtils.randomAlphanumeric(4);
        Assert.assertFalse(taggablePage.hasTag(secondTag));

        AddTagsPane addTagsPane = taggablePage.addTags();
        addTagsPane.setTags(firstTag + "," + secondTag);
        Assert.assertTrue(addTagsPane.add());
        Assert.assertTrue(taggablePage.hasTag(firstTag));
        Assert.assertTrue(taggablePage.hasTag(secondTag));

        Assert.assertTrue(taggablePage.removeTag(firstTag));
        Assert.assertTrue(taggablePage.hasTag(secondTag));
    }

    /**
     * Tests that a tag can't be added twice to the same page.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")})
    public void testAddExistingTag()
    {
        String tag = RandomStringUtils.randomAlphanumeric(4);
        Assert.assertFalse(taggablePage.hasTag(tag));
        AddTagsPane addTagsPane = taggablePage.addTags();
        addTagsPane.setTags(tag);
        Assert.assertTrue(addTagsPane.add());
        Assert.assertTrue(taggablePage.hasTag(tag));

        addTagsPane = taggablePage.addTags();
        addTagsPane.setTags(tag);
        Assert.assertFalse(addTagsPane.add());
        addTagsPane.cancel();
    }

    /**
     * Add a tag that contains the pipe character, which is used to separate stored tags.
     */
    @Test
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    public void testAddTagContainingPipe()
    {
        String tag = RandomStringUtils.randomAlphanumeric(3) + "|" + RandomStringUtils.randomAlphanumeric(3);
        Assert.assertFalse(taggablePage.hasTag(tag));
        AddTagsPane addTagsPane = taggablePage.addTags();
        addTagsPane.setTags(tag);
        Assert.assertTrue(addTagsPane.add());
        Assert.assertTrue(taggablePage.hasTag(tag));

        // Reload the page and test again.
        getUtil().gotoPage(getTestClassName(), getTestMethodName());
        taggablePage = new TaggablePage();
        Assert.assertTrue(taggablePage.hasTag(tag));
    }

    /**
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-3843">XWIKI-3843</a>: Strip leading and trailing white
     *      spaces to tags when white space is not the separator
     */
    @Test
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    public void testStripLeadingAndTrailingSpacesFromTags()
    {
        String firstTag = RandomStringUtils.randomAlphanumeric(4);
        Assert.assertFalse(taggablePage.hasTag(firstTag));
        String secondTag = RandomStringUtils.randomAlphanumeric(4);
        Assert.assertFalse(taggablePage.hasTag(secondTag));

        AddTagsPane addTagsPane = taggablePage.addTags();
        addTagsPane.setTags("   " + firstTag + " ,  " + secondTag + "    ");
        Assert.assertTrue(addTagsPane.add());
        Assert.assertTrue(taggablePage.hasTag(firstTag));
        Assert.assertTrue(taggablePage.hasTag(secondTag));
    }

    /**
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-6549">XWIKI-6549</a>: Prevent adding new tags that are
     *      equal ignoring case with existing tags
     */
    @Test
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    public void testTagCaseIsIgnored()
    {
        String firstTag = "taG1";
        Assert.assertFalse(taggablePage.hasTag(firstTag));
        // Second tag is same as first tag but with different uppercase/lowercase chars.
        String secondTag = "Tag1";
        Assert.assertFalse(taggablePage.hasTag(secondTag));
        String thirdTag = "tag3";
        Assert.assertFalse(taggablePage.hasTag(thirdTag));

        AddTagsPane addTagsPane = taggablePage.addTags();
        addTagsPane.setTags(firstTag + "," + thirdTag + "," + secondTag);
        Assert.assertTrue(addTagsPane.add());
        Assert.assertTrue(taggablePage.hasTag(firstTag));
        Assert.assertFalse(taggablePage.hasTag(secondTag));
        Assert.assertTrue(taggablePage.hasTag(thirdTag));
    }

    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")})
    public void addAndRenameTagFromTagPage()
    {
      String tag = "MyTag";
      AddTagsPane addTagsPane = taggablePage.addTags();
      addTagsPane.setTags(tag);
      Assert.assertTrue(addTagsPane.add());
      Assert.assertTrue(taggablePage.hasTag(tag));
      tagPage = taggablePage.clickOnTag(tag);
      tagPage.clickRenameButton();
      tagPage.setNewTagName("MyTagRenamed");
      tagPage.clickConfirmRenameTagButton();
      Assert.assertTrue(tagPage.hasTagHighlight("MyTagRenamed"));
    }
    
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")})
    public void addAndDeleteTagFromTagPage()
    {
      String tag = "MyTagToBeDeleted";
      AddTagsPane addTagsPane = taggablePage.addTags();
      addTagsPane.setTags(tag);
      Assert.assertTrue(addTagsPane.add());
      Assert.assertTrue(taggablePage.hasTag(tag));
      tagPage = taggablePage.clickOnTag(tag);
      tagPage.clickDeleteButton();
      tagPage.clickConfirmDeleteTag();
       Assert.assertTrue(tagPage.hasConfirmationMessage(tag));
    }

}
