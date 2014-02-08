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
package org.xwiki.action.internal;

import java.util.Arrays;

import org.junit.*;
import org.xwiki.action.Action;
import org.xwiki.action.ActionChain;
import org.xwiki.action.ActionManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.resource.ActionId;
import org.xwiki.resource.Resource;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.action.internal.DefaultActionManager}.
 *
 * @version $Id$
 * @since 6.0M1
 */
public class DefaultActionManagerTest
{
    @Rule
    public MockitoComponentMockingRule<ActionManager> componentManager =
        new MockitoComponentMockingRule<ActionManager>(DefaultActionManager.class);

    @Test
    public void executeWithOrder() throws Exception
    {
        // First Action component will lower priority
        Action viewAction = mock(Action.class, "action1");
        when(viewAction.getSupportedActionIds()).thenReturn(Arrays.asList(ActionId.VIEW));

        // Second Action component will higher priority so that it's executed first
        Action beforeViewAction = mock(Action.class, "action2");
        when(beforeViewAction.getSupportedActionIds()).thenReturn(Arrays.asList(ActionId.VIEW));
        // We return 1 to mean that the second action has a higher priority than the first action
        when(beforeViewAction.compareTo(viewAction)).thenReturn(1);

        ComponentManager contextComponentManager = this.componentManager.getInstance(ComponentManager.class, "context");
        when(contextComponentManager.<Action>getInstanceList(Action.class)).thenReturn(
            Arrays.asList(viewAction, beforeViewAction));

        Resource resource = mock(Resource.class);
        when(resource.getActionId()).thenReturn(ActionId.VIEW);

        this.componentManager.getComponentUnderTest().execute(resource);

        // Verify that the second Action is called (since it has a higher priority).
        verify(beforeViewAction).execute(same(resource), any(ActionChain.class));
    }
}
