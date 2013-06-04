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
package org.xwiki.search.solr.internal;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.search.solr.internal.script.SolrIndexScriptService;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Tests for the {@link SolrIndexScriptService}.
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

    private XWikiRightService mockRightsService;

    private DocumentReference userReference;

    private SolrIndexScriptService service;

    private Logger logger;

    @Before
    public void setUp() throws Exception
    {
        service = mocker.getComponentUnderTest();

        logger = mocker.getMockedLogger();
        userReference = new DocumentReference("wiki", "space", "userName");

        // Context
        mockContext = mock(XWikiContext.class);
        Execution mockExecution = mocker.getInstance(Execution.class);
        ExecutionContext mockExecutionContext = new ExecutionContext();
        mockExecutionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, mockContext);
        when(mockExecution.getContext()).thenReturn(mockExecutionContext);

        // XWiki
        mockXWiki = mock(XWiki.class);
        when(mockContext.getWiki()).thenReturn(mockXWiki);
        when(mockContext.getDatabase()).thenReturn("currentWiki");
        when(mockContext.getUserReference()).thenReturn(userReference);

        // RightService
        mockRightsService = mock(XWikiRightService.class);
        when(mockXWiki.getRightService()).thenReturn(mockRightsService);
        // By default, we have the rights.
        when(mockRightsService.hasWikiAdminRights(mockContext)).thenReturn(true);
        when(mockRightsService.hasProgrammingRights(mockContext)).thenReturn(true);

        // Rights check success. By default we are allowed (no error is thrown)
        verify(logger, never()).error(anyString(), any(IllegalAccessException.class));
    }

    @Test
    public void indexSingleReferenceChecksRights() throws Exception
    {
        EntityReference entityReference = new WikiReference("someWiki");

        // Call
        service.index(entityReference);

        // Assert and verify

        // setDatabase once to check rights on the target wiki and once more to set back the current database
        verify(mockContext, times(2)).setDatabase(anyString());

        // Actual rights check
        verify(mockRightsService).hasWikiAdminRights(mockContext);
        verify(mockRightsService).hasProgrammingRights(mockContext);
    }

    @Test
    public void indexMultipleReferencesChecksRights() throws Exception
    {
        EntityReference entityReference = new WikiReference("someWiki");

        // Call

        // Note: Faking it. just using one reference but still calling the multiple references method (which is what we
        // wanted anyway)
        service.index(Arrays.asList(entityReference));

        // Assert and verify

        // setDatabase once to check rights on the target wiki and once more to set back the current database.
        verify(mockContext, times(2)).setDatabase(anyString());

        // Actual rights check
        verify(mockRightsService).hasWikiAdminRights(mockContext);
        verify(mockRightsService).hasProgrammingRights(mockContext);
    }

    @Test
    public void deleteSingleReferenceChecksRights() throws Exception
    {
        EntityReference entityReference = new WikiReference("someWiki");

        // Call
        service.delete(entityReference);

        // Assert and verify

        // setDatabase once to check rights on the target wiki and once more to set back the current database
        verify(mockContext, times(2)).setDatabase(anyString());

        // Actual rights check
        verify(mockRightsService).hasWikiAdminRights(mockContext);
        verify(mockRightsService).hasProgrammingRights(mockContext);
    }

    @Test
    public void deleteMultipleReferencesChecksRights() throws Exception
    {
        EntityReference entityReference = new WikiReference("someWiki");

        // Call

        // Note: Faking it. just using one reference but still calling the multiple references method (which is what we
        // wanted anyway)
        service.delete(Arrays.asList(entityReference));

        // Assert and verify

        // setDatabase once to check rights on the target wiki and once more to set back the current database
        verify(mockContext, times(2)).setDatabase(anyString());

        // Actual rights check
        verify(mockRightsService).hasWikiAdminRights(mockContext);
        verify(mockRightsService).hasProgrammingRights(mockContext);
    }

    @Test
    public void operationsChecksRightsWithOtherReferences() throws Exception
    {
        EntityReference entityReference = new DocumentReference("someWiki", "space", "document");

        // Call
        service.index(entityReference);

        // Assert and verify

        // setDatabase once to check rights on the target wiki and once more to set back the current database
        verify(mockContext, times(2)).setDatabase(anyString());

        // Actual rights check
        verify(mockRightsService).hasWikiAdminRights(mockContext);
        verify(mockRightsService).hasProgrammingRights(mockContext);
    }

    @Test
    public void hasWikiAdminButNoProgrammingCausesRightsCheckFailure() throws Exception
    {
        EntityReference entityReference = new WikiReference("someWiki");

        // Mock
        when(mockRightsService.hasProgrammingRights(mockContext)).thenReturn(false);

        // Call
        service.index(entityReference);

        // Assert and verify

        // setDatabase once to check rights on the target wiki and once more to set back the current database
        verify(mockContext, times(2)).setDatabase(anyString());

        // Actual rights check
        verify(mockRightsService).hasWikiAdminRights(mockContext);
        verify(mockRightsService).hasProgrammingRights(mockContext);

        // Rights check failure
        String errorMessage =
            String.format("The user '%s' is not allowed to alter the index for the entity '%s'", userReference,
                entityReference);
        verify(logger).error(eq(errorMessage), any(IllegalAccessException.class));
        verify(mockContext).put(eq(SolrIndexScriptService.CONTEXT_LASTEXCEPTION), any(IllegalAccessException.class));
    }

    @Test
    public void hasProgrammingButNoWikiAdminCausesRightsCheckFailure() throws Exception
    {
        EntityReference entityReference = new WikiReference("someWiki");

        // Mock
        when(mockRightsService.hasWikiAdminRights(mockContext)).thenReturn(false);

        // Call
        service.index(entityReference);

        // Assert and verify

        // setDatabase once to check rights on the target wiki and once more to set back the current database
        verify(mockContext, times(2)).setDatabase(anyString());

        // Actual rights check
        verify(mockRightsService).hasWikiAdminRights(mockContext);
        // hasProgrammingRights does not really get to be called, since hasWikiAdminRights already failed at this point
        verify(mockRightsService, atMost(1)).hasProgrammingRights(mockContext);

        // Rights check failure.
        String errorMessage =
            String.format("The user '%s' is not allowed to alter the index for the entity '%s'", userReference,
                entityReference);
        verify(logger).error(eq(errorMessage), any(IllegalAccessException.class));
        verify(mockContext).put(eq(SolrIndexScriptService.CONTEXT_LASTEXCEPTION), any(IllegalAccessException.class));
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
        service.index(Arrays.asList(wikiReference, spaceReference, documentReference, documentReference2));

        // Assert and verify

        // setDatabase once to check rights on the target wiki and once more to set back the current database
        verify(mockContext, times(2)).setDatabase(anyString());

        // Actual rights check
        verify(mockRightsService).hasWikiAdminRights(mockContext);
        verify(mockRightsService).hasProgrammingRights(mockContext);
    }

    @Test
    public void openrationsOnMultipleReferencesOnDifferentWikisChecksRightsOnEachWiki() throws Exception
    {
        // References from 3 different wikis
        WikiReference wikiReference = new WikiReference("wiki");
        SpaceReference spaceReference = new SpaceReference("space", wikiReference);
        WikiReference wikiReference2 = new WikiReference("wiki2");
        DocumentReference documentReference = new DocumentReference("wiki2", "space", "name");
        DocumentReference documentReference2 = new DocumentReference("wiki3", "space", "name2");

        // Call
        service.index(Arrays.asList(wikiReference, spaceReference, wikiReference2, documentReference,
            documentReference2));

        // Assert and verify

        // setDatabase 3 times for each wiki to check rights on the target wiki and 3 times more to set back the current
        // database each time
        verify(mockContext, times(6)).setDatabase(anyString());

        // Actual rights check, once for each wiki.
        verify(mockRightsService, times(3)).hasWikiAdminRights(mockContext);
        verify(mockRightsService, times(3)).hasProgrammingRights(mockContext);
    }
}
