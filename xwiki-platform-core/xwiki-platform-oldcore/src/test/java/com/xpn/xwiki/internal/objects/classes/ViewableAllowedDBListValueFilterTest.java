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
package com.xpn.xwiki.internal.objects.classes;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ViewableAllowedDBListValueFilter}.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
public class ViewableAllowedDBListValueFilterTest
{
    @Rule
    public MockitoComponentMockingRule<QueryFilter> mocker =
        new MockitoComponentMockingRule<QueryFilter>(ViewableAllowedDBListValueFilter.class);

    private ContextualAuthorizationManager authorization;

    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Before
    public void configure() throws Exception
    {
        this.authorization = this.mocker.getInstance(ContextualAuthorizationManager.class);
        this.documentReferenceResolver = this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");

        DocumentReference aliceReference = new DocumentReference("wiki", "User", "alice");
        when(this.documentReferenceResolver.resolve("alice")).thenReturn(aliceReference);
        when(this.authorization.hasAccess(Right.VIEW, aliceReference)).thenReturn(false);

        DocumentReference bobReference = new DocumentReference("wiki", "User", "bob");
        when(this.documentReferenceResolver.resolve("bob")).thenReturn(bobReference);
        when(this.authorization.hasAccess(Right.VIEW, bobReference)).thenReturn(true);
    }

    @Test
    public void filterStatement() throws Exception
    {
        String statement = "select doc.fullName from XWikiDocument doc";
        assertEquals("The statement should not be filtered", statement,
            this.mocker.getComponentUnderTest().filterStatement(statement, Query.HQL));
    }

    @Test
    public void filterResultsStrings() throws Exception
    {
        assertEquals(Arrays.asList("bob"),
            this.mocker.getComponentUnderTest().filterResults(Arrays.asList("alice", "bob")));
    }

    @Test
    public void filterResultsArray() throws Exception
    {
        List<?> filteredResults = this.mocker.getComponentUnderTest()
            .filterResults(Arrays.asList(new Object[] {"alice", 13}, new Object[] {"bob", 7}));
        assertEquals(1, filteredResults.size());
        Object[] result = (Object[]) filteredResults.get(0);
        assertEquals(1, result.length);
        assertEquals(7, result[0]);
    }
}
