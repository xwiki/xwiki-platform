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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.group.GroupManager;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link GroupScriptService}.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@ComponentTest
class GroupScriptServiceTest
{
    private static final DocumentReference ADDED_GROUP = new DocumentReference("xwiki", "XWiki", "Added");

    private static final DocumentReference TARGET_GROUP = new DocumentReference("xwiki", "XWiki", "Target");

    @InjectMockComponents
    private GroupScriptService groupScriptService;

    @MockComponent
    private GroupManager groupManager;

    @Test
    void canAddAsMemberTargetNull() throws Exception
    {
        final boolean actual = this.groupScriptService.canAddAsMember(null, ADDED_GROUP);
        assertFalse(actual);
        verify(this.groupManager, never()).getGroups(any(), any(), anyBoolean());
        verify(this.groupManager, never()).getMembers(any(), anyBoolean());
    }

    @Test
    void canAddAsMemberMemberNull() throws Exception
    {
        final boolean actual = this.groupScriptService.canAddAsMember(TARGET_GROUP, null);
        assertFalse(actual);
        verify(this.groupManager, never()).getGroups(any(), any(), anyBoolean());
        verify(this.groupManager, never()).getMembers(any(), anyBoolean());
    }

    @Test
    void canAddAsMemberTargetIsMember() throws Exception
    {
        final boolean actual = this.groupScriptService.canAddAsMember(TARGET_GROUP, TARGET_GROUP);
        assertFalse(actual);
        // if the target is equal to the member no need to do more advanced verifications
        verify(this.groupManager, never()).getGroups(any(), any(), anyBoolean());
        verify(this.groupManager, never()).getMembers(any(), anyBoolean());
    }

    @Test
    void canAddAsMemberIsAlreadyAMemberOf() throws Exception
    {
        when(this.groupManager.getGroups(TARGET_GROUP, null, true)).thenReturn(singletonList(ADDED_GROUP));

        final boolean actual = this.groupScriptService.canAddAsMember(TARGET_GROUP, ADDED_GROUP);

        assertFalse(actual);
        verify(this.groupManager, never()).getMembers(any(), anyBoolean());
    }

    @Test
    void canAddAsMemberTargetIsAlreadyAMember() throws Exception
    {
        when(this.groupManager.getGroups(TARGET_GROUP, null, true)).thenReturn(emptyList());
        when(this.groupManager.getMembers(TARGET_GROUP, true)).thenReturn(singletonList(ADDED_GROUP));

        final boolean actual = this.groupScriptService.canAddAsMember(TARGET_GROUP, ADDED_GROUP);
        assertFalse(actual);
    }

    @Test
    void canAddAsMember() throws Exception
    {
        when(this.groupManager.getGroups(TARGET_GROUP, null, true)).thenReturn(emptyList());
        when(this.groupManager.getMembers(TARGET_GROUP, true)).thenReturn(emptyList());

        final boolean actual = this.groupScriptService.canAddAsMember(TARGET_GROUP, ADDED_GROUP);
        assertTrue(actual);
    }
}
