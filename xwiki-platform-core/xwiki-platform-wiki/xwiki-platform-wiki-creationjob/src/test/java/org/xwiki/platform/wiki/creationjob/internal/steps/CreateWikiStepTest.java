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

import org.junit.jupiter.api.Test;
import org.xwiki.platform.wiki.creationjob.WikiCreationException;
import org.xwiki.platform.wiki.creationjob.WikiCreationRequest;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.manager.WikiManager;
import org.xwiki.wiki.manager.WikiManagerException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * @version $Id$
 */
@ComponentTest
class CreateWikiStepTest
{
    @InjectMockComponents
    private CreateWikiStep createWikiStep;

    @MockComponent
    private WikiManager wikiManager;

    @Test
    void execute() throws Exception
    {
        WikiCreationRequest request = new WikiCreationRequest();
        request.setWikiId("wikiId");
        request.setAlias("wikiAlias");
        request.setOwnerId("owner");
        request.setFailOnExist(true);

        // Test
        this.createWikiStep.execute(request);

        // Verify
        verify(this.wikiManager).create("wikiId", "wikiAlias", "owner", true);

        // Test 2
        request.setFailOnExist(false);
        this.createWikiStep.execute(request);

        // Verify
        verify(this.wikiManager).create("wikiId", "wikiAlias", "owner", false);
    }

    @Test
    void executeWhenException() throws Exception
    {
        WikiCreationRequest request = new WikiCreationRequest();
        request.setWikiId("wikiId");
        request.setAlias("wikiAlias");
        request.setOwnerId("owner");
        request.setFailOnExist(true);

        Exception exception = new WikiManagerException("Exception in WikiManager.");
        doThrow(exception).when(this.wikiManager).create(any(), any(), any(), anyBoolean());

        // Test and verify
        WikiCreationException caughtException = assertThrows(WikiCreationException.class,
            () -> this.createWikiStep.execute(request));
        assertEquals("Failed to create the wiki [wikiId].", caughtException.getMessage());
        assertEquals(exception, caughtException.getCause());
    }

    @Test
    void getOrder()
    {
        assertEquals(1000, this.createWikiStep.getOrder());
    }
}
