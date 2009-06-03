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
package com.xpn.xwiki.wysiwyg.client.util;

import com.xpn.xwiki.wysiwyg.client.AbstractWysiwygClientTest;

/**
 * Unit test for the {@link ResourceName} class.
 * 
 * @version $Id$
 */
public class ResourceNameTest extends AbstractWysiwygClientTest
{
    /**
     * First value of a wiki to use in tests.
     */
    private String wiki1 = "wiki";

    /**
     * First value of a space to use in tests.
     */
    private String space1 = "space";

    /**
     * First value of a page to use in tests.
     */
    private String page1 = "page";

    /**
     * First value of a space to use in tests.
     */
    private String file1 = "file";

    /**
     * Second value of a wiki to use in tests.
     */
    private String wiki2 = "xwwiki";

    /**
     * Second value of a space to use in tests.
     */
    private String space2 = "wspace";

    /**
     * Second value of a page to use in tests.
     */
    private String page2 = "wpage";

    /**
     * Second value of a file to use in tests.
     */
    private String file2 = "wfile";

    /**
     * The default value for the wiki.
     */
    private String defaultWiki = "xwiki";

    /**
     * The resource to transform to relative resource.
     */
    private ResourceName toRelativize;

    /**
     * The reference relative which to transform.
     */
    private ResourceName reference;

    /**
     * The built relative reference.
     */
    private ResourceName relative;

    /**
     * Tests {@link ResourceName#getRelativeTo(ResourceName)} for the cases when only the file should remain in the
     * relative resource.
     */
    public void testGetRelativeToReturnFile()
    {
        // 1. everything is the same, file has to stay in relative
        toRelativize = new ResourceName(wiki1, space1, page1, file1);
        reference = new ResourceName(wiki1, space1, page1, file1);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, null, null, file1);

        // 2. everything is the same up to file, file has to stay in relative
        toRelativize = new ResourceName(wiki1, space1, page1, file1);
        reference = new ResourceName(wiki1, space1, page1, file2);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, null, null, file1);

        // 3. only file is specified, has to stay in relative
        toRelativize = new ResourceName(null, null, null, file1);
        reference = new ResourceName(wiki2, space1, page1, file1);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, null, null, file1);

        // 4. only file is specified with relative reference so file has to stay in relative
        toRelativize = new ResourceName(null, null, null, file1);
        reference = new ResourceName(null, null, page1, file1);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, null, null, file1);

        // 5. only file is specified with relative reference so file has to stay in relative
        toRelativize = new ResourceName(null, null, null, file1);
        reference = new ResourceName(null, null, null, file2);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, null, null, file1);

        // 6. only file is specified but different, has to stay in relative
        toRelativize = new ResourceName(null, null, null, file1);
        reference = new ResourceName(null, space1, page1, null);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, null, null, file1);

        // 7. only file, page and space are specified space and page are the same, so only file has to stay in relative
        toRelativize = new ResourceName(null, space1, page1, file1);
        reference = new ResourceName(wiki2, space1, page1, null);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, null, null, file1);
    }

    /**
     * Tests {@link ResourceName#getRelativeTo(ResourceName)} for the cases when only the page should remain in the
     * relative resource.
     */
    public void testGetRelativeToReturnPage()
    {
        // 1. everything is the same, page has to stay in relative
        toRelativize = new ResourceName(wiki1, space1, page1, null);
        reference = new ResourceName(wiki1, space1, page1, null);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, null, page1, null);

        // 2. everything is the same up to page, page has to stay in relative
        toRelativize = new ResourceName(wiki1, space1, page1, null);
        reference = new ResourceName(wiki1, space1, page2, file2);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, null, page1, null);

        // 3. only page is specified, has to stay in relative
        toRelativize = new ResourceName(null, null, page1, null);
        reference = new ResourceName(wiki1, space1, page1, null);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, null, page1, null);

        // 4. only page and space are specified, but space is the same so only page has to stay in relative
        toRelativize = new ResourceName(null, space1, page1, null);
        reference = new ResourceName(wiki1, space1, page2, file2);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, null, page1, null);

        // 5. only page and space are specified, with relative reference, but space is the same so only page has to stay
        // in relative
        toRelativize = new ResourceName(null, space1, page1, null);
        reference = new ResourceName(null, space1, page2, null);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, null, page1, null);

        // 6. only page and space are specified with relative reference and pages are equal so only page has to stay in
        // relative
        toRelativize = new ResourceName(null, space1, page1, null);
        reference = new ResourceName(null, space1, page1, file2);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, null, page1, null);

        // 7. only page is specified with relative reference so page has to stay
        toRelativize = new ResourceName(null, null, page1, null);
        reference = new ResourceName(null, null, page2, file2);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, null, page1, null);
    }

    /**
     * Tests {@link ResourceName#getRelativeTo(ResourceName)} for the cases when only the page and file should remain in
     * the relative resource.
     */
    public void testGetRelativeToReturnPageAndFile()
    {
        // 1. everything is the same up to page and file, page and file have to stay in relative
        toRelativize = new ResourceName(wiki1, space1, page1, file1);
        reference = new ResourceName(wiki1, space1, page2, file2);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, null, page1, file1);

        // 2. only file and page are specified with same file but different page, to page and file have to stay in
        // relative
        toRelativize = new ResourceName(null, null, page1, file1);
        reference = new ResourceName(wiki2, space1, page2, file1);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, null, page1, file1);

        // 3. only file is specified with relative reference so page and file have to stay in relative
        toRelativize = new ResourceName(null, null, page1, file1);
        reference = new ResourceName(null, null, null, file1);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, null, page1, file1);

        // 4. only file, page and space are specified, space is the same, so only page and file have to stay in relative
        toRelativize = new ResourceName(null, space1, page1, file1);
        reference = new ResourceName(wiki2, space1, page2, file2);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, null, page1, file1);

        // 5. only file, page and space are specified, space is the same, so only page and file have to stay in relative
        toRelativize = new ResourceName(null, space1, page1, file1);
        reference = new ResourceName(null, space1, page2, file2);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, null, page1, file1);
    }

    /**
     * Tests {@link ResourceName#getRelativeTo(ResourceName)} for the cases when only the space and page should remain
     * in the relative resource.
     */
    public void testGetRelativeToReturnSpaceAndPage()
    {
        // 1. everything is the same up to space and page, space and page have to stay in relative
        toRelativize = new ResourceName(wiki1, space1, page1, null);
        reference = new ResourceName(wiki1, space2, page2, file2);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, space1, page1, null);

        // 2. everything is the same except for the space, space and page have to stay in relative
        toRelativize = new ResourceName(wiki1, space1, page1, null);
        reference = new ResourceName(wiki1, space2, page1, file2);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, space1, page1, null);

        // 3. everything unspecified except for the space, space and page have to stay in relative
        toRelativize = new ResourceName(null, space1, page1, null);
        reference = new ResourceName(wiki1, space2, page1, null);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, space1, page1, null);

        // 4. everything unspecified, in reference too, except for the space, space and page have to stay in relative
        toRelativize = new ResourceName(null, space1, page1, null);
        reference = new ResourceName(null, space2, page2, null);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, space1, page1, null);
    }

    /**
     * Tests {@link ResourceName#getRelativeTo(ResourceName)} for the cases when only the space and page should remain
     * in the relative resource.
     */
    public void testGetRelativeToReturnSpacePageAndFile()
    {
        // 1. everything is the same except for the space, page and file so space, page and file have to stay in
        // relative
        toRelativize = new ResourceName(wiki1, space1, page1, file1);
        reference = new ResourceName(wiki1, space2, page2, file1);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, space1, page1, file1);

        // 2. everything is the same except for the space, so space, page and file have to stay in relative
        toRelativize = new ResourceName(wiki1, space1, page1, file1);
        reference = new ResourceName(wiki1, space2, page1, file1);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, space1, page1, file1);

        // 3. everything is unspecified except for the space, page and file, and spaces and page are not the same so
        // space, page and file have to stay in relative,
        toRelativize = new ResourceName(null, space1, page1, file1);
        reference = new ResourceName(wiki1, space2, page2, null);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, space1, page1, file1);

        // 4. wiki is unspecified, and spaces and page are not the same so space, page and file have to stay in
        // relative,
        toRelativize = new ResourceName(null, space1, page1, file1);
        reference = new ResourceName(null, space2, page2, null);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, space1, page1, file1);

        // 5. wiki is specified but default, reference is relative, and spaces and page are not the same so space, page
        // and file have to stay in relative
        toRelativize = new ResourceName(defaultWiki, space1, page1, file1);
        reference = new ResourceName(null, space2, page2, null);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(null, space1, page1, file1);
    }

    /**
     * Tests {@link ResourceName#getRelativeTo(ResourceName)} for the cases when the whole reference without a file
     * should be returned in relative.
     */
    public void testGetRelativeToReturnWikiSpaceAndPage()
    {
        // 1. everything is different so they all stay in relative
        toRelativize = new ResourceName(wiki1, space1, page1, null);
        reference = new ResourceName(wiki2, space2, page2, file2);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(wiki1, space1, page1, null);

        // 1. everything is different so they all stay in relative, with relative reference
        toRelativize = new ResourceName(wiki1, space1, page1, null);
        reference = new ResourceName(null, space2, page2, null);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(wiki1, space1, page1, null);
    }

    /**
     * Tests {@link ResourceName#getRelativeTo(ResourceName)} for the cases when the whole reference should be returned
     * in relative.
     */
    public void testGetRelativeToReturnWikiSpacePageAndFile()
    {
        // 1. everything is different so they all stay in relative
        toRelativize = new ResourceName(wiki1, space1, page1, file1);
        reference = new ResourceName(wiki2, space2, page2, file2);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(wiki1, space1, page1, file1);

        // 1. everything is different so they all stay in relative, with relative reference
        toRelativize = new ResourceName(wiki1, space1, page1, file1);
        reference = new ResourceName(null, space2, page2, file2);
        relative = toRelativize.getRelativeTo(reference);

        assertRelative(wiki1, space1, page1, file1);
    }

    /**
     * Checks the relative resource to match the passed values. Whichever is {@code null} is checked to be empty in the
     * relative.
     * 
     * @param wiki the expected wiki
     * @param space the expected space
     * @param page the expected page
     * @param file the expected file
     */
    private void assertRelative(String wiki, String space, String page, String file)
    {
        if (wiki != null) {
            assertEquals(wiki, relative.getWiki());
        } else {
            assertTrue(StringUtils.isEmpty(relative.getWiki()));
        }

        if (space != null) {
            assertEquals(space, relative.getSpace());
        } else {
            assertTrue(StringUtils.isEmpty(relative.getSpace()));
        }
        if (page != null) {
            assertEquals(page, relative.getPage());
        } else {
            assertTrue(StringUtils.isEmpty(relative.getPage()));
        }

        if (file != null) {
            assertEquals(file, relative.getFile());
        } else {
            assertTrue(StringUtils.isEmpty(relative.getFile()));
        }
    }
}
