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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.internal.DefaultWikiComponentManager;
import org.xwiki.component.wiki.internal.WikiComponentManagerContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultWikiComponentManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultWikiComponentManagerTest
{
    private static final DocumentReference DOC_REFERENCE = new DocumentReference("xwiki", "XWiki", "MyComponent");

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Admin");

    @InjectMockComponents
    private DefaultWikiComponentManager manager;

    @MockComponent
    private WikiComponentManagerContext wcmc;

    @MockComponent
    @Named("wiki")
    private ComponentManager wikiComponentManager;

    @Test
    void registerAndUnregisterWikiComponent() throws Exception
    {
        when(this.wcmc.getCurrentEntityReference()).thenReturn(DOC_REFERENCE);
        when(this.wcmc.getCurrentUserReference()).thenReturn(AUTHOR_REFERENCE);

        WikiComponent component =
            new TestImplementation(DOC_REFERENCE, AUTHOR_REFERENCE, WikiComponentScope.WIKI, 42, 43);

        // Register the wiki component
        this.manager.registerWikiComponent(component);

        // Test 1: we verify that the component has been registered against the CM
        ArgumentCaptor<ComponentDescriptor> componentCaoptor = ArgumentCaptor.forClass(ComponentDescriptor.class);
        verify(this.wikiComponentManager, times(1)).registerComponent(componentCaoptor.capture(), eq(component));
        assertEquals(TestRole.class, componentCaoptor.getValue().getRoleType());
        assertEquals("roleHint", componentCaoptor.getValue().getRoleHint());
        assertEquals(42, componentCaoptor.getValue().getRoleTypePriority());
        assertEquals(43, componentCaoptor.getValue().getRoleHintPriority());
        assertEquals(TestImplementation.class, componentCaoptor.getValue().getImplementation());

        // Try to register the wiki component again
        this.manager.registerWikiComponent(component);

        // Test 2: we verify that the component has been registered again against the CM
        verify(this.wikiComponentManager, times(2)).registerComponent(any(ComponentDescriptor.class), eq(component));

        // Unregister the wiki component
        this.manager.unregisterWikiComponents(DOC_REFERENCE);

        // Test 3: we verify that the component has been unregistered from the CM
        // Note that indirectly this tests that the wiki component has been added to the wiki component cache during
        // the call to registerWikiComponent()
        verify(this.wikiComponentManager, times(1)).unregisterComponent(TestRole.class, "roleHint");

        // Try to unregister the wiki component again
        this.manager.unregisterWikiComponents(DOC_REFERENCE);

        // Test 4: We verify that nothing happens on the CM since the wiki component is not in the wiki cache.
        // Note: the times(1) comes from the previous call in test 2 above.
        verify(this.wikiComponentManager, times(1)).unregisterComponent(TestRole.class, "roleHint");
    }
}
