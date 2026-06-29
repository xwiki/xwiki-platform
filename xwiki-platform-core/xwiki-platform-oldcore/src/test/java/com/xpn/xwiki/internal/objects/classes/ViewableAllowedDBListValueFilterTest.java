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

import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ViewableAllowedDBListValueFilter}.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@ComponentTest
class ViewableAllowedDBListValueFilterTest
{
    @InjectMockComponents
    private ViewableAllowedDBListValueFilter filter;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @BeforeEach
    void configure()
    {
        DocumentReference aliceReference = new DocumentReference("wiki", "User", "alice");
        when(this.documentReferenceResolver.resolve("alice")).thenReturn(aliceReference);
        when(this.authorization.hasAccess(Right.VIEW, aliceReference)).thenReturn(false);

        DocumentReference bobReference = new DocumentReference("wiki", "User", "bob");
        when(this.documentReferenceResolver.resolve("bob")).thenReturn(bobReference);
        when(this.authorization.hasAccess(Right.VIEW, bobReference)).thenReturn(true);
    }

    @Test
    void filterStatement()
    {
        String statement = "select doc.fullName from XWikiDocument doc";
        assertEquals(statement, this.filter.filterStatement(statement, Query.HQL),
            "The statement should not be filtered");
    }

    @Test
    void filterResultsStrings()
    {
        assertEquals(List.of("bob"), this.filter.filterResults(List.of("alice", "bob")));
    }

    @Test
    void filterResultsArray()
    {
        List<?> filteredResults = this.filter.filterResults(
            List.of(new Object[] {"alice", 13}, new Object[] {"bob", 7}));
        assertEquals(1, filteredResults.size());
        Object[] result = (Object[]) filteredResults.getFirst();
        assertEquals(1, result.length);
        assertEquals(7, result[0]);
    }
}
