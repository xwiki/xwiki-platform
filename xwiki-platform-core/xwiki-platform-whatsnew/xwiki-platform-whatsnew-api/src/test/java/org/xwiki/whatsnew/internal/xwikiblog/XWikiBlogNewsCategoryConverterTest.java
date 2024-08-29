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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.whatsnew.NewsCategory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link XWikiBlogNewsCategoryConverter}.
 *
 * @version $Id$
 */
class XWikiBlogNewsCategoryConverterTest
{
    @Test
    void convertFromRSS()
    {
        List<String> rssCategories = List.of(
            "Blog.What's New for XWiki: Advanced User",
            "Blog.What's New for XWiki: Simple User",
            "Blog.What's New for XWiki: Admin User",
            "Blog.What's New for XWiki: Extension",
            "not known");
        Set<NewsCategory> results = new XWikiBlogNewsCategoryConverter().convertFromRSS(rssCategories);

        assertEquals(5, results.size());
        assertTrue(results.contains(NewsCategory.ADMIN_USER));
        assertTrue(results.contains(NewsCategory.SIMPLE_USER));
        assertTrue(results.contains(NewsCategory.ADVANCED_USER));
        assertTrue(results.contains(NewsCategory.EXTENSION));
        assertTrue(results.contains(NewsCategory.UNKNOWN));
    }

    @Test
    void convertToQueryString()
    {
        Set<NewsCategory> categories = new LinkedHashSet<>();
        categories.add(NewsCategory.SIMPLE_USER);
        categories.add(NewsCategory.ADMIN_USER);
        categories.add(NewsCategory.ADVANCED_USER);
        categories.add(NewsCategory.EXTENSION);
        categories.add(NewsCategory.UNKNOWN);
        String result = new XWikiBlogNewsCategoryConverter().convertToQueryString(categories);

        // TODO: Until https://jira.xwiki.org/browse/BLOG-198 is implemented, the XWiki Blog app only supports
        // a single category. Thus FTM, we use the "Blog.What's New for XWiki" one to get all news under that
        // category.
        // Once it's fixed, uncomment the following line:
        //   assertEquals("category=Blog.What%27s+New+for+XWiki%3A+Simple+User&"
        //       + "category=Blog.What%27s+New+for+XWiki%3A+Admin+User&"
        //       + "category=Blog.What%27s+New+for+XWiki%3A+Advanced+User&"
        //       + "category=Blog.What%27s+New+for+XWiki%3A+Extension", result);
        assertEquals("category=Blog.What%27s+New+for+XWiki", result);
    }
}
