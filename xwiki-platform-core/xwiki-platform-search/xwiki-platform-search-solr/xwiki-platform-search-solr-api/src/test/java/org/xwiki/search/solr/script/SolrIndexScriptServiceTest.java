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

import java.util.Arrays;

import javax.inject.Provider;

import org.apache.solr.common.SolrDocument;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.reference.SolrEntityReferenceResolver;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
public class SolrIndexScriptServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<SolrIndexScriptService> mocker =
        new MockitoComponentMockingRule<SolrIndexScriptService>(SolrIndexScriptService.class);

    private XWikiContext mockContext;

    private XWiki mockXWiki;

    private XWikiDocument mockCurrentDocument;

    private DocumentReference userReference;

    private SolrIndexScriptService service;

    private Logger logger;

    private AuthorizationManager mockAuthorization;

    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Before
    public void setUp() throws Exception
    {
        this.userReference = new DocumentReference("wiki", "space", "user");

        // Context
        this.mockContext = mock(XWikiContext.class);
        Provider<XWikiContext> xcontextProvider = this.mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.mockContext);

        // XWiki
        this.mockXWiki = mock(XWiki.class);
        when(mockContext.getWiki()).thenReturn(this.mockXWiki);

        this.mockCurrentDocument = mock(XWikiDocument.class);
        when(mockContext.getDoc()).thenReturn(this.mockCurrentDocument);

        when(mockContext.getWikiId()).thenReturn("currentWiki");
        when(mockContext.getUserReference()).thenReturn(userReference);

        // RightService
        this.mockAuthorization = this.mocker.getInstance(AuthorizationManager.class);
        this.contextualAuthorizationManager = this.mocker.getInstance(ContextualAuthorizationManager.class);

        this.service = mocker.getComponentUnderTest();

        // Rights check success. By default we are allowed (no error is thrown)
        this.logger = mocker.getMockedLogger();
        verify(this.logger, never()).error(any(), any(AccessDeniedException.class));
    }

    @Test
    public void indexSingleReferenceChecksRights() throws Exception
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
    public void indexMultipleReferencesChecksRights() throws Exception
    {
        EntityReference wikiReference = new WikiReference("someWiki");

        // Call

        // Note: Faking it. just using one reference but still calling the multiple references method (which is what we
        // wanted anyway)
        this.service.index(Arrays.asList(wikiReference));

        // Assert and verify

        // Actual rights check
        verify(this.mockAuthorization).checkAccess(Right.ADMIN, this.userReference, wikiReference);
        verify(this.contextualAuthorizationManager).checkAccess(Right.PROGRAM);
    }

    @Test
    public void deleteSingleReferenceChecksRights() throws Exception
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
    public void deleteMultipleReferencesChecksRights() throws Exception
    {
        EntityReference wikiReference = new WikiReference("someWiki");

        // Call

        // Note: Faking it. just using one reference but still calling the multiple references method (which is what we
        // wanted anyway)
        this.service.delete(Arrays.asList(wikiReference));

        // Assert and verify

        // Actual rights check
        verify(this.mockAuthorization).checkAccess(Right.ADMIN, this.userReference, wikiReference);
        verify(this.contextualAuthorizationManager).checkAccess(Right.PROGRAM);
    }

    @Test
    public void operationsChecksRightsWithOtherReferences() throws Exception
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
    public void hasWikiAdminButNoProgrammingCausesRightsCheckFailure() throws Exception
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

        // Rights check failure
        verify(this.logger).error(isNull(), any(AccessDeniedException.class));
        verify(this.mockContext).put(eq(SolrIndexScriptService.CONTEXT_LASTEXCEPTION),
            any(AccessDeniedException.class));
    }

    @Test
    public void hasProgrammingButNoWikiAdminCausesRightsCheckFailure() throws Exception
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
        verify(this.contextualAuthorizationManager, times(0)).checkAccess(Right.PROGRAM);

        // Rights check failure.
        verify(this.logger).error(isNull(), any(AccessDeniedException.class));
        verify(this.mockContext).remove(eq(SolrIndexScriptService.CONTEXT_LASTEXCEPTION));
        verify(this.mockContext).put(eq(SolrIndexScriptService.CONTEXT_LASTEXCEPTION),
            any(AccessDeniedException.class));
    }

    @Test
    public void openrationsOnMultipleReferencesOnTheSameWikiChecksRightsOnlyOnceForThatWiki() throws Exception
    {
        // References from the same wiki
        WikiReference wikiReference = new WikiReference("wiki");
        SpaceReference spaceReference = new SpaceReference("space", wikiReference);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "name");
        DocumentReference documentReference2 = new DocumentReference("wiki", "space", "name2");

        // Call
        this.service.index(Arrays.asList(wikiReference, spaceReference, documentReference, documentReference2));

        // Assert and verify

        // Actual rights check
        verify(this.mockAuthorization).checkAccess(Right.ADMIN, this.userReference, wikiReference);
        verify(this.contextualAuthorizationManager).checkAccess(Right.PROGRAM);
    }

    @Test
    public void openrationsOnMultipleReferencesOnDifferentWikisChecksRightsOnEachWiki() throws Exception
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
            Arrays.asList(wikiReference1, spaceReference, wikiReference2, documentReference, documentReference2));

        // Assert and verify

        // Actual rights check, once for each wiki.
        verify(this.mockAuthorization).checkAccess(Right.ADMIN, this.userReference, wikiReference1);
        verify(this.mockAuthorization).checkAccess(Right.ADMIN, this.userReference, wikiReference2);
        verify(this.mockAuthorization).checkAccess(Right.ADMIN, this.userReference, wikiReference3);
        verify(this.contextualAuthorizationManager, times(3)).checkAccess(Right.PROGRAM);
    }

    @Test
    public void resolveWithImplicitType() throws Exception
    {
        SolrDocument document = new SolrDocument();
        Object[] parameters = new Object[] {};

        assertNull(this.service.resolve(document, parameters));

        document.setField(FieldUtils.TYPE, "foo");
        assertNull(this.service.resolve(document, parameters));

        EntityReferenceResolver<SolrDocument> solrEntityReferenceResolver =
            this.mocker.getInstance(SolrEntityReferenceResolver.TYPE);
        EntityReference spaceReference = new EntityReference("bar", EntityType.SPACE);
        when(solrEntityReferenceResolver.resolve(document, EntityType.SPACE, parameters)).thenReturn(spaceReference);

        document.setField(FieldUtils.TYPE, "SPACE");
        assertSame(spaceReference, this.service.resolve(document, parameters));
    }
}
