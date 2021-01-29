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
package org.xwiki.panels.internal.script;

import java.util.Arrays;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of {@link PanelsInternalScriptService}.
 *
 * @version $Id$
 * @since 13.1RC1
 * @since 12.10.4
 * @since 12.6.8
 */
@ComponentTest
class PanelsInternalScriptServiceTest
{
    @InjectMockComponents
    private PanelsInternalScriptService panelsInternalScriptService;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    @Named("hidden/document")
    private QueryFilter hiddenDocumentQueryFilter;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @Test
    void listOrphaned() throws Exception
    {
        Query query = mock(Query.class);
        DocumentReference currentUser = new DocumentReference("xwiki", "XWiki", "U1");
        DocumentReference ok1 = new DocumentReference("xwiki", "XWiki", "ok1");
        DocumentReference ok2 = new DocumentReference("xwiki", "XWiki", "ok2");
        DocumentReference ok3 = new DocumentReference("xwiki", "XWiki", "ok3");
        DocumentReference ok4 = new DocumentReference("xwiki", "XWiki", "ok4");
        DocumentReference nok1 = new DocumentReference("xwiki", "XWiki", "nok1");
        DocumentReference nok2 = new DocumentReference("xwiki", "XWiki", "nok2");

        when(this.queryManager.createQuery(any(String.class), eq(Query.XWQL))).thenReturn(query);
        when(query.setLimit(4)).thenReturn(query);
        when(query.setOffset(5)).thenReturn(query);
        when(query.setOffset(9)).thenReturn(query);
        when(query.addFilter(this.hiddenDocumentQueryFilter)).thenReturn(query);
        when(query.bindValue("homepage", "homePageVal")).thenReturn(query);
        when(query.execute()).thenReturn(
            Arrays.asList("ok1", "nok1", "ok2", "ok3"),
            Arrays.asList("nok2", "ok4", "ok5", "ok6")
        );
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(currentUser);
        when(this.documentReferenceResolver.resolve("ok1")).thenReturn(ok1);
        when(this.documentReferenceResolver.resolve("ok2")).thenReturn(ok2);
        when(this.documentReferenceResolver.resolve("ok3")).thenReturn(ok3);
        when(this.documentReferenceResolver.resolve("ok4")).thenReturn(ok4);
        when(this.documentReferenceResolver.resolve("nok1")).thenReturn(nok1);
        when(this.documentReferenceResolver.resolve("nok2")).thenReturn(nok2);

        when(this.authorizationManager.hasAccess(Right.VIEW, currentUser, ok1)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.VIEW, currentUser, ok2)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.VIEW, currentUser, ok3)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.VIEW, currentUser, ok4)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.VIEW, currentUser, nok1)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.VIEW, currentUser, nok2)).thenReturn(false);

        OrphanedPagesItem actual = this.panelsInternalScriptService.listOrphaned(4, 5, "homePageVal");

        assertEquals(new OrphanedPagesItem(Arrays.asList("ok1", "ok2", "ok3", "ok4"), 11, true), actual);
    }

    @Test
    void listOrphanedReachEnd() throws Exception
    {
        Query query = mock(Query.class);
        DocumentReference currentUser = new DocumentReference("xwiki", "XWiki", "U1");
        DocumentReference ok1 = new DocumentReference("xwiki", "XWiki", "ok1");
        DocumentReference ok2 = new DocumentReference("xwiki", "XWiki", "ok2");
        DocumentReference nok1 = new DocumentReference("xwiki", "XWiki", "nok1");
        DocumentReference nok2 = new DocumentReference("xwiki", "XWiki", "nok2");
        DocumentReference nok3 = new DocumentReference("xwiki", "XWiki", "nok3");

        when(this.queryManager.createQuery(any(String.class), eq(Query.XWQL))).thenReturn(query);
        when(query.setLimit(3)).thenReturn(query);
        when(query.setOffset(5)).thenReturn(query);
        when(query.setOffset(8)).thenReturn(query);
        when(query.setOffset(10)).thenReturn(query);
        when(query.addFilter(this.hiddenDocumentQueryFilter)).thenReturn(query);
        when(query.bindValue("homepage", "homePageVal")).thenReturn(query);
        when(query.execute()).thenReturn(
            Arrays.asList("ok1", "nok1", "nok2"),
            Arrays.asList("ok2", "nok3"),
            Arrays.asList()
        );
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(currentUser);
        when(this.documentReferenceResolver.resolve("ok1")).thenReturn(ok1);
        when(this.documentReferenceResolver.resolve("ok2")).thenReturn(ok2);
        when(this.documentReferenceResolver.resolve("nok1")).thenReturn(nok1);
        when(this.documentReferenceResolver.resolve("nok2")).thenReturn(nok2);
        when(this.documentReferenceResolver.resolve("nok3")).thenReturn(nok3);

        when(this.authorizationManager.hasAccess(Right.VIEW, currentUser, ok1)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.VIEW, currentUser, ok2)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.VIEW, currentUser, nok1)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.VIEW, currentUser, nok2)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.VIEW, currentUser, nok3)).thenReturn(false);

        OrphanedPagesItem actual = this.panelsInternalScriptService.listOrphaned(3, 5, "homePageVal");

        assertEquals(new OrphanedPagesItem(Arrays.asList("ok1", "ok2"), 10, false), actual);
    }
}