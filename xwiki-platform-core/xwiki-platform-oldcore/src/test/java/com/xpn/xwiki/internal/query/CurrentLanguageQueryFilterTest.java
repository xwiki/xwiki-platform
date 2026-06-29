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
package com.xpn.xwiki.internal.query;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.query.Query;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CurrentLanguageQueryFilter}.
 *
 * @version $Id$
 * @since 5.1M2
 */
@ComponentTest
class CurrentLanguageQueryFilterTest
{
    @InjectMockComponents
    private CurrentLanguageQueryFilter filter;

    @MockComponent
    private Execution execution;

    @Test
    void filterStatement()
    {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(this.execution.getContext()).thenReturn(executionContext);
        XWikiContext xwikiContext = mock(XWikiContext.class);
        when(executionContext.getProperty("xwikicontext")).thenReturn(xwikiContext);
        com.xpn.xwiki.XWiki xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xwikiContext.getWiki()).thenReturn(xwiki);
        when(xwiki.getDefaultLocale(any(XWikiContext.class))).thenReturn(Locale.FRANCE);

        String result = this.filter.filterStatement(
            "select doc.fullName from XWikiDocument doc where 1=1", Query.HQL);
        assertEquals("select doc.fullName from XWikiDocument doc where (doc.language is null or doc.language = '' or "
            + "doc.language = 'fr_FR') and (1=1)", result);
    }
}
