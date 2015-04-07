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
package org.xwiki.platform.wiki.creationjob.internal.steps;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.platform.wiki.creationjob.WikiCreationException;
import org.xwiki.platform.wiki.creationjob.WikiCreationRequest;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.manager.WikiManager;
import org.xwiki.wiki.manager.WikiManagerException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * @version $Id$
 */
public class CreateWikiStepTest
{
    @Rule
    public MockitoComponentMockingRule<CreateWikiStep> mocker = new MockitoComponentMockingRule<>(CreateWikiStep.class);

    private WikiManager wikiManager;

    @Before
    public void setUp() throws Exception
    {
        wikiManager = mocker.getInstance(WikiManager.class);
    }

    @Test
    public void execute() throws Exception
    {
        WikiCreationRequest request = new WikiCreationRequest();
        request.setWikiId("wikiId");
        request.setAlias("wikiAlias");
        request.setFailOnExist(true);

        // Test
        mocker.getComponentUnderTest().execute(request);

        // Verify
        verify(wikiManager).create("wikiId", "wikiAlias", true);

        // Test 2
        request.setFailOnExist(false);
        mocker.getComponentUnderTest().execute(request);

        // Verify
        verify(wikiManager).create("wikiId", "wikiAlias", false);
    }

    @Test
    public void executeWhenException() throws Exception
    {
        WikiCreationRequest request = new WikiCreationRequest();
        request.setWikiId("wikiId");
        request.setAlias("wikiAlias");
        request.setFailOnExist(true);

        Exception exception = new WikiManagerException("Execption in WikiManager.");
        doThrow(exception).when(wikiManager).create(anyString(), anyString(), anyBoolean());

        // Test
        WikiCreationException caughtException = null;
        try {
            mocker.getComponentUnderTest().execute(request);
        } catch (WikiCreationException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals("Failed to create the wiki [wikiId].", caughtException.getMessage());
        assertEquals(exception, caughtException.getCause());

    }

    @Test
    public void getOrder() throws Exception
    {
        assertEquals(1000, mocker.getComponentUnderTest().getOrder());
    }
}
