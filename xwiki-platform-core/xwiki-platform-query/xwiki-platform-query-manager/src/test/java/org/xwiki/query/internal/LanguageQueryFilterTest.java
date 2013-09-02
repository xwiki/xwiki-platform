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
package org.xwiki.query.internal;

import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.query.Query;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Tests for {@link org.xwiki.query.internal.LanguageQueryFilter}
 *
 * @version $Id$
 * @since 5.1M2
 */
public class LanguageQueryFilterTest
{
    @Rule
    public MockitoComponentMockingRule<LanguageQueryFilter> mocker =
        new MockitoComponentMockingRule<LanguageQueryFilter>(LanguageQueryFilter.class);

    @Test
    public void filterStatementWhenStatementMatches() throws Exception
    {
        String result = this.mocker.getComponentUnderTest().filterStatement(
            "select doc.fullName from XWikiDocument doc ...", Query.HQL);
        assertEquals("select doc.fullName, doc.language from XWikiDocument doc ...", result);
    }

    @Test
    public void filterStatementWhenStatementDoesntMatches() throws Exception
    {
        String result = this.mocker.getComponentUnderTest().filterStatement("select whatever", Query.HQL);
        assertEquals("select whatever", result);
    }

    @Test
    public void filterStatementWhenStatementIsNotHQL() throws Exception
    {
        String result = this.mocker.getComponentUnderTest().filterStatement(
            "select doc.fullName from XWikiDocument doc ...", Query.XWQL);
        assertEquals("select doc.fullName from XWikiDocument doc ...", result);
    }

    @Test
    public void ffilterResults() throws Exception
    {
        List<String> items = Arrays.asList("one", "two");
        List<String> result = this.mocker.getComponentUnderTest().filterResults(items);
        assertSame(items, result);
    }
}
