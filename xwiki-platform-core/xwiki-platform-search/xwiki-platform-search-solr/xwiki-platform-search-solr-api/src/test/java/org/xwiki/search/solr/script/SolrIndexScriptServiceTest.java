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
package org.xwiki.search.solr.script;

import java.util.List;

import javax.inject.Provider;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link org.xwiki.search.solr.script.SolrIndexScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class SolrIndexScriptServiceTest
{
    @InjectMockComponents
    private SolrIndexScriptService service;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private AuthorizationManager mockAuthorization;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private EntityReferenceResolver<SolrDocument> solrEntityReferenceResolver;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    private DocumentReference userReference;

    private XWikiContext mockContext;

    private XWiki mockXWiki;

    private XWikiDocument mockCurrentDocument;

    @BeforeEach
    void setUp()
    {
        this.userReference = new DocumentReference("wiki", "space", "user");

        // Context
        this.mockContext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.mockContext);

        // XWiki
        this.mockXWiki = mock(XWiki.class);
        when(this.mockContext.getWiki()).thenReturn(this.mockXWiki);

        this.mockCurrentDocument = mock(XWikiDocument.class);
        when(this.mockContext.getDoc()).thenReturn(this.mockCurrentDocument);

        when(this.mockContext.getWikiId()).thenReturn("currentWiki");
        when(this.mockContext.getUserReference()).thenReturn(this.userReference);

        // Rights check success. By default we are allowed (no error is thrown)
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void indexSingleReferenceChecksRights() throws Exception
    {
        EntityReference wikiReference = new WikiReference("someWiki");

        // Call
        this.service.index(wikiReference);

        // Assert and verify

        // Actual rights check
        verify(this.mockAuthorization).checkAccess(Right.ADMIN, this.userReference, wikiReference);
        verify(this.contextualAuthorizationManager).checkAccess(Right.PROGRAM);
    }

    @Test
    void indexMultipleReferencesChecksRights() throws Exception
    {
        EntityReference wikiReference = new WikiReference("someWiki");

        // Call

        // Note: Faking it. just using one reference but still calling the multiple references method (which is what we
        // wanted anyway)
        this.service.index(List.of(wikiReference));

        // Assert and verify

        // Actual rights check
        verify(this.mockAuthorization).checkAccess(Right.ADMIN, this.userReference, wikiReference);
        verify(this.contextualAuthorizationManager).checkAccess(Right.PROGRAM);
    }

    @Test
    void deleteSingleReferenceChecksRights() throws Exception
    {
        EntityReference wikiReference = new WikiReference("someWiki");

        // Call
        this.service.delete(wikiReference);

        // Assert and verify

        // Actual rights check
        verify(this.mockAuthorization).checkAccess(Right.ADMIN, this.userReference, wikiReference);
        verify(this.contextualAuthorizationManager).checkAccess(Right.PROGRAM);
    }

    @Test
    void deleteMultipleReferencesChecksRights() throws Exception
    {
        EntityReference wikiReference = new WikiReference("someWiki");

        // Call

        // Note: Faking it. just using one reference but still calling the multiple references method (which is what we
        // wanted anyway)
        this.service.delete(List.of(wikiReference));

        // Assert and verify

        // Actual rights check
        verify(this.mockAuthorization).checkAccess(Right.ADMIN, this.userReference, wikiReference);
        verify(this.contextualAuthorizationManager).checkAccess(Right.PROGRAM);
    }

    @Test
    void operationsChecksRightsWithOtherReferences() throws Exception
    {
        EntityReference documentReference = new DocumentReference("someWiki", "space", "document");

        // Call
        this.service.index(documentReference);

        // Assert and verify

        // Actual rights check
        EntityReference wikiReference = documentReference.extractReference(EntityType.WIKI);
        verify(this.mockAuthorization).checkAccess(Right.ADMIN, this.userReference, wikiReference);
        verify(this.contextualAuthorizationManager).checkAccess(Right.PROGRAM);
    }

    @Test
    void hasWikiAdminButNoProgrammingCausesRightsCheckFailure() throws Exception
    {
        EntityReference wikiReference = new WikiReference("someWiki");

        // Mock
        doThrow(AccessDeniedException.class).when(this.contextualAuthorizationManager).checkAccess(Right.PROGRAM);

        // Call
        this.service.index(wikiReference);

        // Assert and verify

        // Actual rights check
        verify(this.mockAuthorization).checkAccess(Right.ADMIN, this.userReference, wikiReference);
        verify(this.contextualAuthorizationManager).checkAccess(Right.PROGRAM);

        // Rights check failure (AccessDeniedException has no message, so getMessage() returns null)
        assertEquals(1, this.logCapture.size());
        assertNull(this.logCapture.getMessage(0));
        verify(this.mockContext).put(eq(SolrIndexScriptService.CONTEXT_LASTEXCEPTION),
            any(AccessDeniedException.class));
    }

    @Test
    void hasProgrammingButNoWikiAdminCausesRightsCheckFailure() throws Exception
    {
        EntityReference wikiReference = new WikiReference("someWiki");

        // Mock
        doThrow(AccessDeniedException.class).when(this.mockAuthorization).checkAccess(Right.ADMIN, this.userReference,
            wikiReference);

        // Call
        this.service.index(wikiReference);

        // Assert and verify

        // Actual rights check
        verify(this.mockAuthorization).checkAccess(Right.ADMIN, this.userReference, wikiReference);
        // hasProgrammingRights does not really get to be called, since hasWikiAdminRights already failed at this point
        verify(this.contextualAuthorizationManager, never()).checkAccess(Right.PROGRAM);

        // Rights check failure.
        assertEquals(1, this.logCapture.size());
        assertNull(this.logCapture.getMessage(0));
        verify(this.mockContext).remove(SolrIndexScriptService.CONTEXT_LASTEXCEPTION);
        verify(this.mockContext).put(eq(SolrIndexScriptService.CONTEXT_LASTEXCEPTION),
            any(AccessDeniedException.class));
    }

    @Test
    void openrationsOnMultipleReferencesOnTheSameWikiChecksRightsOnlyOnceForThatWiki() throws Exception
    {
        // References from the same wiki
        WikiReference wikiReference = new WikiReference("wiki");
        SpaceReference spaceReference = new SpaceReference("space", wikiReference);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "name");
        DocumentReference documentReference2 = new DocumentReference("wiki", "space", "name2");

        // Call
        this.service.index(List.of(wikiReference, spaceReference, documentReference, documentReference2));

        // Assert and verify

        // Actual rights check
        verify(this.mockAuthorization).checkAccess(Right.ADMIN, this.userReference, wikiReference);
        verify(this.contextualAuthorizationManager).checkAccess(Right.PROGRAM);
    }

    @Test
    void openrationsOnMultipleReferencesOnDifferentWikisChecksRightsOnEachWiki() throws Exception
    {
        // References from 3 different wikis
        WikiReference wikiReference1 = new WikiReference("wiki");
        SpaceReference spaceReference = new SpaceReference("space", wikiReference1);
        WikiReference wikiReference2 = new WikiReference("wiki2");
        DocumentReference documentReference = new DocumentReference("wiki2", "space", "name");
        WikiReference wikiReference3 = new WikiReference("wiki3");
        DocumentReference documentReference2 = new DocumentReference("wiki3", "space", "name2");

        // Call
        this.service.index(
            List.of(wikiReference1, spaceReference, wikiReference2, documentReference, documentReference2));

        // Assert and verify

        // Actual rights check, once for each wiki.
        verify(this.mockAuthorization).checkAccess(Right.ADMIN, this.userReference, wikiReference1);
        verify(this.mockAuthorization).checkAccess(Right.ADMIN, this.userReference, wikiReference2);
        verify(this.mockAuthorization).checkAccess(Right.ADMIN, this.userReference, wikiReference3);
        verify(this.contextualAuthorizationManager, times(3)).checkAccess(Right.PROGRAM);
    }

    @Test
    void resolveWithImplicitType()
    {
        SolrDocument document = new SolrDocument();
        Object[] parameters = new Object[] {};

        assertNull(this.service.resolve(document, parameters));

        document.setField(FieldUtils.TYPE, "foo");
        assertNull(this.service.resolve(document, parameters));

        EntityReference spaceReference = new EntityReference("bar", EntityType.SPACE);
        when(this.solrEntityReferenceResolver.resolve(document, EntityType.SPACE, parameters))
            .thenReturn(spaceReference);

        document.setField(FieldUtils.TYPE, "SPACE");
        assertSame(spaceReference, this.service.resolve(document, parameters));
    }
}
