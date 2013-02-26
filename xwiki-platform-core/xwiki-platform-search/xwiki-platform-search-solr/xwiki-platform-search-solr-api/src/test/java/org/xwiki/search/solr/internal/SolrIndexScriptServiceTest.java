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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.search.solr.internal.api.SolrIndexException;
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
    public final MockitoComponentMockingRule<SolrIndexScriptService> mocker = new MockitoComponentMockingRule(
        SolrIndexScriptService.class);

    private XWikiContext mockContext;

    private XWiki mockXWiki;

    private XWikiRightService mockRightsService;

    private DocumentReference userReference;

    @Before
    public void setUp() throws Exception
    {
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
        when(mockRightsService.hasWikiAdminRights(mockContext)).thenReturn(true);
        when(mockRightsService.hasProgrammingRights(mockContext)).thenReturn(true);
    }

    @Test
    public void checkAccessToWikiIndexHasWikiAdminAndProgramming() throws Exception
    {
        SolrIndexScriptService service = mocker.getComponentUnderTest();

        EntityReference entityReference = new WikiReference("someWiki");

        // Call
        service.checkAccessToWikiIndex(entityReference);

        // Assert and verify
        Mockito.verify(mockContext, Mockito.times(2)).setDatabase(Mockito.anyString());
    }

    @Test
    public void checkAccessToWikiIndexOnDocument() throws Exception
    {
        SolrIndexScriptService service = mocker.getComponentUnderTest();

        EntityReference entityReference = new DocumentReference("someWiki", "space", "document");

        // Call
        service.checkAccessToWikiIndex(entityReference);

        // Assert and verify
        Mockito.verify(mockContext, Mockito.times(2)).setDatabase(Mockito.anyString());
    }

    @Test
    public void checkAccessToWikiIndexHasWikiAdminButNoProgramming() throws Exception
    {
        SolrIndexScriptService service = mocker.getComponentUnderTest();

        EntityReference entityReference = new WikiReference("someWiki");

        // Mock
        when(mockRightsService.hasProgrammingRights(mockContext)).thenReturn(false);

        // Call
        try {
            service.checkAccessToWikiIndex(entityReference);
            Assert.fail("Not having PR should fail the operation.");
        } catch (SolrIndexException e) {
            Assert.assertEquals(String.format("The user '%s' is not allowed to alter the index for the entity '%s'",
                userReference, entityReference), e.getMessage());
        }

        // Assert and verify
        Mockito.verify(mockContext, Mockito.times(2)).setDatabase(Mockito.anyString());
    }

    @Test
    public void checkAccessToWikiIndexNoWikiAdminButHasProgramming() throws Exception
    {
        SolrIndexScriptService service = mocker.getComponentUnderTest();

        EntityReference entityReference = new WikiReference("someWiki");

        // Mock
        when(mockRightsService.hasWikiAdminRights(mockContext)).thenReturn(false);

        // Call
        try {
            service.checkAccessToWikiIndex(entityReference);
            Assert.fail("Not having admin rights should fail the operation.");
        } catch (SolrIndexException e) {
            Assert.assertEquals(String.format("The user '%s' is not allowed to alter the index for the entity '%s'",
                userReference, entityReference), e.getMessage());
        }

        // Assert and verify
        Mockito.verify(mockContext, Mockito.times(2)).setDatabase(Mockito.anyString());
    }

    @Test
    public void checkAccessToWikiIndexMultipleReferencesOneWiki() throws Exception
    {
        // Use a spy to be able to verify invocations of methods.
        SolrIndexScriptService spyService = Mockito.spy(mocker.getComponentUnderTest());

        WikiReference wikiReference = new WikiReference("wiki");
        SpaceReference spaceReference = new SpaceReference("space", wikiReference);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "name");
        DocumentReference documentReference2 = new DocumentReference("wiki", "space", "name2");

        // Call
        spyService.checkAccessToWikiIndex(Arrays.asList(wikiReference, spaceReference, documentReference,
            documentReference2));

        // Assert and verify
        Mockito.verify(spyService, Mockito.times(1)).checkAccessToWikiIndex(Mockito.any(EntityReference.class));
    }

    @Test
    public void checkAccessToWikiIndexMultipleReferencesMultipleWikis() throws Exception
    {
        // Use a spy to be able to verify invocations of methods.
        SolrIndexScriptService spyService = Mockito.spy(mocker.getComponentUnderTest());

        WikiReference wikiReference = new WikiReference("wiki");
        SpaceReference spaceReference = new SpaceReference("space", wikiReference);
        WikiReference wikiReference2 = new WikiReference("wiki2");
        DocumentReference documentReference = new DocumentReference("wiki2", "space", "name");
        DocumentReference documentReference2 = new DocumentReference("wiki3", "space", "name2");

        // Call
        spyService.checkAccessToWikiIndex(Arrays.asList(wikiReference, spaceReference, wikiReference2,
            documentReference, documentReference2));

        // Assert and verify
        Mockito.verify(spyService, Mockito.times(3)).checkAccessToWikiIndex(Mockito.any(EntityReference.class));
    }
}
