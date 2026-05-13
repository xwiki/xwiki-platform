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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.platform.wiki.creationjob.WikiCreationException;
import org.xwiki.platform.wiki.creationjob.WikiCreationRequest;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.user.WikiUserManager;
import org.xwiki.wiki.user.WikiUserManagerException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * @version $Id$
 */
@ComponentTest
class AddUsersStepTest
{
    @InjectMockComponents
    private AddUsersStep addUsersStep;

    @MockComponent
    private WikiUserManager wikiUserManager;

    @Test
    void execute() throws Exception
    {
        WikiCreationRequest request = new WikiCreationRequest();
        request.setWikiId("wikiId");
        List<String> members = new ArrayList<>();
        request.setMembers(members);

        // Test
        this.addUsersStep.execute(request);

        // Verify
        verify(this.wikiUserManager).addMembers(members, "wikiId");
    }

    @Test
    void executeWhenException() throws Exception
    {
        WikiCreationRequest request = new WikiCreationRequest();
        request.setWikiId("wikiId");
        List<String> members = new ArrayList<>();
        request.setMembers(members);

        Exception exception = new WikiUserManagerException("Execption in WikiUserManager.");
        doThrow(exception).when(this.wikiUserManager).addMembers(anyCollection(), any());

        // Test and verify
        WikiCreationException caughtException = assertThrows(WikiCreationException.class,
            () -> this.addUsersStep.execute(request));
        assertEquals("Failed to add members to the wiki [wikiId].", caughtException.getMessage());
        assertEquals(exception, caughtException.getCause());
    }

    @Test
    void getOrder()
    {
        assertEquals(4000, this.addUsersStep.getOrder());
    }
}
