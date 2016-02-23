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
package org.xwiki.component.wiki;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.internal.DefaultWikiComponentManager;
import org.xwiki.component.wiki.internal.WikiComponentManagerContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultWikiComponentManager}.
 *
 * @version $Id$
 */
public class DefaultWikiComponentManagerTest
{
    private static final DocumentReference DOC_REFERENCE = new DocumentReference("xwiki", "XWiki", "MyComponent");

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Admin");

    @Rule
    public MockitoComponentMockingRule<DefaultWikiComponentManager> mocker = new MockitoComponentMockingRule<>(
        DefaultWikiComponentManager.class);

    @Test
    public void registerAndUnregisterWikiComponent() throws Exception
    {
        WikiComponentManagerContext wcmc = this.mocker.getInstance(WikiComponentManagerContext.class);
        when(wcmc.getCurrentEntityReference()).thenReturn(DOC_REFERENCE);
        when(wcmc.getCurrentUserReference()).thenReturn(AUTHOR_REFERENCE);

        WikiComponent component = new TestImplementation(DOC_REFERENCE, AUTHOR_REFERENCE, WikiComponentScope.WIKI);

        ComponentManager wikiComponentManager = this.mocker.registerMockComponent(ComponentManager.class, "wiki");

        // Register the wiki component
        this.mocker.getComponentUnderTest().registerWikiComponent(component);

        // Test 1: we verify that the component has been registered against the CM
        verify(wikiComponentManager, times(1)).registerComponent(any(ComponentDescriptor.class), eq(component));

        // Try to register the wiki component again
        this.mocker.getComponentUnderTest().registerWikiComponent(component);

        // Test 2: we verify that the component has been registered again against the CM
        verify(wikiComponentManager, times(2)).registerComponent(any(ComponentDescriptor.class), eq(component));

        // Unregister the wiki component
        this.mocker.getComponentUnderTest().unregisterWikiComponents(DOC_REFERENCE);

        // Test 3: we verify that the component has been unregistered from the CM
        // Note that indirectly this tests that the wiki component has been added to the wiki component cache during
        // the call to registerWikiComponent()
        verify(wikiComponentManager, times(1)).unregisterComponent(TestRole.class, "roleHint");

        // Try to unregister the wiki component again
        this.mocker.getComponentUnderTest().unregisterWikiComponents(DOC_REFERENCE);

        // Test 4: We verify that nothing happens on the CM since the wiki component is not in the wiki cache.
        // Note: the times(1) comes from the previous call in test 2 above.
        verify(wikiComponentManager, times(1)).unregisterComponent(TestRole.class, "roleHint");
    }
}
