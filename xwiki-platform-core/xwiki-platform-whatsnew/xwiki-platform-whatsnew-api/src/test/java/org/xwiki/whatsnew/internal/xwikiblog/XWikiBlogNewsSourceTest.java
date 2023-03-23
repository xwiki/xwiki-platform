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
package org.xwiki.whatsnew.internal.xwikiblog;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.whatsnew.NewsCategory;
import org.xwiki.whatsnew.NewsSourceItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiBlogNewsSource}.
 *
 * @version $Id$
 * @since 15.1RC1
 */
@ComponentTest
class XWikiBlogNewsSourceTest
{
    @Mock
    private RSSContentCleaner cleaner;

    @BeforeEach
    void before()
    {
        when(this.cleaner.clean(any(String.class))).thenAnswer(
            (Answer) invocation -> invocation.getArgument(0));
    }

    @Test
    void buildWithNoConstraint() throws Exception
    {
        XWikiBlogNewsSource source =new XWikiBlogNewsSource(
            XWikiBlogNewsSource.class.getClassLoader().getResourceAsStream("blogrss.xml"), this.cleaner);
        List<NewsSourceItem> items = source.build();

        assertEquals(10, items.size());
        assertEquals("XWiki 15.0 Release Candidate 1 Released", items.get(0).getTitle().get());
        assertEquals("<p>The XWiki development team is proud to announce the availability of the first release "
            + "candidate of XWiki 15.0. This release consists mostly of dependency upgrades and bug fixes including "
            + "security fixes with some small new features for admins and developers. <span class=\"wikilink\">"
            + "<a title=\"Read the full entry\" href=\"/xwiki/bin/view/Blog/XWiki%2015"
            + ".0%20Release%20Candidate%201%20Released\">...</a></span></p>",
            items.get(0).getDescription().get().getContent());
        assertEquals(Syntax.HTML_5_0, items.get(0).getDescription().get().getSyntax());
        assertEquals("Michael Hamann", items.get(0).getAuthor().get());
        assertEquals(NewsCategory.ADMIN_USER, items.get(0).getCategories().iterator().next());
        assertEquals(1674448455000L, items.get(0).getPublishedDate().get().getTime());
        assertEquals("https://www.xwiki.org:443/xwiki/bin/view/Blog/XWiki%2015.0%20Release%20Candidate%201%20Released"
            + "?language=en", items.get(0).getOriginURL().get());
        assertEquals("http://localhost:8080/xwiki/bin/download/Blog/BlogIntroduction/blog-post-thumbnail.jpg?rev=1.1",
            items.get(0).getImageURL().get());
    }

    @Test
    void buildWithAdminUserCategory() throws Exception
    {
        XWikiBlogNewsSource source =new XWikiBlogNewsSource(
            XWikiBlogNewsSource.class.getClassLoader().getResourceAsStream("blogrss-admin.xml"), this.cleaner);
        List<NewsSourceItem> items = source.forCategories(Set.of(NewsCategory.ADMIN_USER)).build();

        assertEquals(8, items.size());
        assertEquals("XWiki 15.0 Release Candidate 1 Released", items.get(0).getTitle().get());
        assertEquals(1, items.get(0).getCategories().size());
        assertEquals(NewsCategory.ADMIN_USER, items.get(0).getCategories().iterator().next());
    }

    @Test
    void buildWithSimpleUserCategory() throws Exception
    {
        XWikiBlogNewsSource source =new XWikiBlogNewsSource(
            XWikiBlogNewsSource.class.getClassLoader().getResourceAsStream("blogrss-simple.xml"), this.cleaner);
        List<NewsSourceItem> items = source.forCategories(Set.of(NewsCategory.SIMPLE_USER)).build();

        assertEquals(2, items.size());
        assertEquals("Highlights of XWiki 14.x cycle", items.get(0).getTitle().get());
        assertEquals(1, items.get(0).getCategories().size());
        assertEquals(NewsCategory.SIMPLE_USER, items.get(0).getCategories().iterator().next());
    }
}
