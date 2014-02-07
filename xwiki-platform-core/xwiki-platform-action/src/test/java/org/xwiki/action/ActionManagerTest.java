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
package org.xwiki.action;

import java.util.Arrays;
import java.util.List;

import javax.inject.Provider;

import org.junit.*;
import org.xwiki.action.internal.DefaultActionManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.Resource;
import org.xwiki.resource.ResourceAction;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.action.ActionManager}.
 *
 * @version $Id$
 * @since 6.0M1
 */
public class ActionManagerTest
{
    @Rule
    public MockitoComponentMockingRule<ActionManager> componentManager =
        new MockitoComponentMockingRule<ActionManager>(DefaultActionManager.class);

    @Test
    public void executeWithOrder() throws Exception
    {
        // First Action component will lower priority
        Action viewAction = mock(Action.class, "action1");
        when(viewAction.getSupportedResourceActions()).thenReturn(Arrays.asList(ResourceAction.VIEW));

        // Second Action component will higher priority so that it's executed first
        Action beforeViewAction = mock(Action.class, "action2");
        when(beforeViewAction.getSupportedResourceActions()).thenReturn(Arrays.asList(ResourceAction.VIEW));
        // We return 1 to mean that the second action has a higher priority than the first action
        when(beforeViewAction.compareTo(viewAction)).thenReturn(1);

        Provider<List<Action>> actionComponents = this.componentManager.getInstance(
            new DefaultParameterizedType(null, Provider.class,
                new DefaultParameterizedType(null, List.class, Action.class)));
        when(actionComponents.get()).thenReturn(Arrays.asList(viewAction, beforeViewAction));

        Resource resource = mock(Resource.class);
        when(resource.getAction()).thenReturn(ResourceAction.VIEW);

        this.componentManager.getComponentUnderTest().execute(resource);

        // Verify that the second Action is called (since it has a higher priority).
        verify(beforeViewAction).execute(same(resource), any(ActionChain.class));
    }
}
