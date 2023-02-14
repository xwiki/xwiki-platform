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
package org.xwiki.user.script;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.group.GroupException;
import org.xwiki.user.group.GroupManager;

import ch.qos.logback.classic.Level;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.test.LogLevel.WARN;

/**
 * Test of {@link GroupScriptService}.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@ComponentTest
class GroupScriptServiceTest
{
    private static final DocumentReference CANDIDATE_GROUP = new DocumentReference("xwiki", "XWiki", "Added");

    private static final DocumentReference TARGET_GROUP = new DocumentReference("xwiki", "XWiki", "Target");

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(WARN);

    @InjectMockComponents
    private GroupScriptService groupScriptService;

    @MockComponent
    private GroupManager groupManager;

    @Test
    void canAddGroupAsMember() throws Exception
    {
        when(this.groupManager.getMembers(CANDIDATE_GROUP, true)).thenReturn(emptyList());
        when(this.groupManager.getMembers(TARGET_GROUP, true)).thenReturn(emptyList());

        boolean actual = this.groupScriptService.canAddGroupAsMember(CANDIDATE_GROUP, TARGET_GROUP);

        assertTrue(actual);
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void canAddGroupAsMemberTargetNull() throws Exception
    {
        boolean actual = this.groupScriptService.canAddGroupAsMember(CANDIDATE_GROUP, null);

        assertFalse(actual);
        verify(this.groupManager, never()).getGroups(any(), any(), anyBoolean());
        verify(this.groupManager, never()).getMembers(any(), anyBoolean());
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void canAddGroupAsMemberCandidateNull() throws Exception
    {
        boolean actual = this.groupScriptService.canAddGroupAsMember(null, TARGET_GROUP);

        assertFalse(actual);
        verify(this.groupManager, never()).getGroups(any(), any(), anyBoolean());
        verify(this.groupManager, never()).getMembers(any(), anyBoolean());
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void canAddGroupAsMemberTargetIsCandidate() throws Exception
    {
        boolean actual = this.groupScriptService.canAddGroupAsMember(TARGET_GROUP, TARGET_GROUP);

        assertFalse(actual);
        // if the target is equal to the member no need to do more advanced verifications
        verify(this.groupManager, never()).getGroups(any(), any(), anyBoolean());
        verify(this.groupManager, never()).getMembers(any(), anyBoolean());
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void canAddGroupAsMemberCandidateIsAlreadyAMemberOfTarget() throws Exception
    {
        when(this.groupManager.getMembers(TARGET_GROUP, false)).thenReturn(singletonList(CANDIDATE_GROUP));

        boolean actual = this.groupScriptService.canAddGroupAsMember(CANDIDATE_GROUP, TARGET_GROUP);

        assertFalse(actual);
        verify(this.groupManager, never()).getMembers(eq(CANDIDATE_GROUP), anyBoolean());
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void canAddGroupAsMemberLinearHierarchy() throws Exception
    {
        /*
        - B is a member of A
        - C is a member of B
        - C can be added to the members of A
        - B cannot be added to the members of C
        - A cannot be added to the members of C
         */

        DocumentReference groupA = new DocumentReference("xwiki", "XWiki", "A");
        DocumentReference groupB = new DocumentReference("xwiki", "XWiki", "B");
        DocumentReference groupC = new DocumentReference("xwiki", "XWiki", "C");

        when(this.groupManager.getMembers(groupA, false)).thenReturn(singletonList(groupB));
        when(this.groupManager.getMembers(groupA, true)).thenReturn(asList(groupB, groupC));

        when(this.groupManager.getMembers(groupB, false)).thenReturn(singletonList(groupC));
        when(this.groupManager.getMembers(groupB, true)).thenReturn(singletonList(groupC));

        when(this.groupManager.getMembers(groupC, false)).thenReturn(emptyList());
        when(this.groupManager.getMembers(groupC, true)).thenReturn(emptyList());

        assertTrue(this.groupScriptService.canAddGroupAsMember(groupC, groupA));
        assertFalse(this.groupScriptService.canAddGroupAsMember(groupA, groupC));
        assertFalse(this.groupScriptService.canAddGroupAsMember(groupB, groupC));
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void canAddGroupAsMemberNonLinearHierarchy() throws Exception
    {
        /*
        - B is a member of A
        - C is a member of A
        - C is a member of B
        - C cannot be added to the members of A
        - C cannot be added to the members of B
        - B cannot be added to the members of C
        - A cannot be added to the members of C
         */

        DocumentReference groupA = new DocumentReference("xwiki", "XWiki", "A");
        DocumentReference groupB = new DocumentReference("xwiki", "XWiki", "B");
        DocumentReference groupC = new DocumentReference("xwiki", "XWiki", "C");

        when(this.groupManager.getMembers(groupA, false)).thenReturn(asList(groupB, groupC));
        when(this.groupManager.getMembers(groupA, true)).thenReturn(asList(groupB, groupC));

        when(this.groupManager.getMembers(groupB, false)).thenReturn(singletonList(groupC));
        when(this.groupManager.getMembers(groupB, true)).thenReturn(singletonList(groupC));

        when(this.groupManager.getMembers(groupC, false)).thenReturn(emptyList());
        when(this.groupManager.getMembers(groupC, true)).thenReturn(emptyList());

        assertFalse(this.groupScriptService.canAddGroupAsMember(groupC, groupA));
        assertFalse(this.groupScriptService.canAddGroupAsMember(groupC, groupB));
        assertFalse(this.groupScriptService.canAddGroupAsMember(groupB, groupC));
        assertFalse(this.groupScriptService.canAddGroupAsMember(groupA, groupC));
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void canAddGroupAsMemberTargetMembersError() throws Exception
    {
        when(this.groupManager.getMembers(TARGET_GROUP, false)).thenThrow(new GroupException(""));

        boolean actual = this.groupScriptService.canAddGroupAsMember(CANDIDATE_GROUP, TARGET_GROUP);

        assertFalse(actual);
        verify(this.groupManager, never()).getMembers(CANDIDATE_GROUP, true);
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Failed to access the members of the target group [xwiki:XWiki.Target]",
            this.logCapture.getMessage(0));
    }

    @Test
    void canAddGroupAsMemberCandidateMembersError() throws Exception
    {
        when(this.groupManager.getMembers(TARGET_GROUP, false)).thenReturn(emptyList());
        when(this.groupManager.getMembers(CANDIDATE_GROUP, true)).thenThrow(new GroupException(""));

        boolean actual = this.groupScriptService.canAddGroupAsMember(CANDIDATE_GROUP, TARGET_GROUP);

        assertFalse(actual);
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Failed to access the members of the candidate group [xwiki:XWiki.Added]",
            this.logCapture.getMessage(0));
    }
}
