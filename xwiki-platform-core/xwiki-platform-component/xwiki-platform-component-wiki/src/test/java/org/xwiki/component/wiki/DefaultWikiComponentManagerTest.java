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

import org.junit.jupiter.api.Test;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.internal.DefaultWikiComponentManager;
import org.xwiki.component.wiki.internal.WikiComponentManagerContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link DefaultWikiComponentManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultWikiComponentManagerTest
{
    private static final DocumentReference DOC_A = new DocumentReference("xwiki", "XWiki", "ComponentA");

    private static final DocumentReference DOC_B = new DocumentReference("xwiki", "XWiki", "ComponentB");

    private static final DocumentReference DOC_C = new DocumentReference("xwiki", "XWiki", "ComponentC");

    private static final DocumentReference OTHER_WIKI_DOC = new DocumentReference("other", "XWiki", "Component");

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Admin");

    private static final DocumentReference OTHER_AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Other");

    private static final String ROLE_HINT = "roleHint";

    @InjectMockComponents
    private DefaultWikiComponentManager manager;

    @MockComponent
    private WikiComponentManagerContext wcmc;

    // A real component manager to get the actual behavior when registering several components with the same role.
    // Note that it doesn't distinguish wikis and users, which allows verifying that a wiki component is only restored
    // in the component manager it was registered in.
    private final EmbeddableComponentManager wikiComponentManager = new EmbeddableComponentManager();

    private final EmbeddableComponentManager userComponentManager = new EmbeddableComponentManager();

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(ComponentManager.class, "wiki", this.wikiComponentManager);
        componentManager.registerComponent(ComponentManager.class, "user", this.userComponentManager);
    }

    private TestImplementation component(DocumentReference documentReference, int roleHintPriority)
    {
        return new TestImplementation(documentReference, AUTHOR_REFERENCE, WikiComponentScope.WIKI, 42,
            roleHintPriority);
    }

    private Object getRegisteredComponent(ComponentManager componentManager) throws Exception
    {
        return componentManager.getInstance(TestRole.class, ROLE_HINT);
    }

    @Test
    void registerAndUnregisterWikiComponent() throws Exception
    {
        TestImplementation component = component(DOC_A, 1000);

        this.manager.registerWikiComponent(component);
        assertSame(component, getRegisteredComponent(this.wikiComponentManager));
        assertEquals(42, this.wikiComponentManager.getComponentDescriptor(TestRole.class, ROLE_HINT)
            .getRoleTypePriority());

        // Registering the same component again doesn't change anything.
        this.manager.registerWikiComponent(component);
        assertSame(component, getRegisteredComponent(this.wikiComponentManager));

        this.manager.unregisterWikiComponents(DOC_A);
        assertFalse(this.wikiComponentManager.hasComponent(TestRole.class, ROLE_HINT));

        // Unregistering again doesn't fail.
        this.manager.unregisterWikiComponents(DOC_A);
        assertFalse(this.wikiComponentManager.hasComponent(TestRole.class, ROLE_HINT));
    }

    @Test
    void unregisterRestoresReplacedComponent() throws Exception
    {
        TestImplementation componentA = component(DOC_A, 1000);
        TestImplementation componentB = component(DOC_B, 1000);
        this.manager.registerWikiComponent(componentA);
        this.manager.registerWikiComponent(componentB);
        assertSame(componentB, getRegisteredComponent(this.wikiComponentManager));

        this.manager.unregisterWikiComponents(DOC_B);

        assertSame(componentA, getRegisteredComponent(this.wikiComponentManager));
    }

    @Test
    void unregisterReplacedComponentKeepsTheRegisteredOne() throws Exception
    {
        TestImplementation componentA = component(DOC_A, 1000);
        TestImplementation componentB = component(DOC_B, 1000);
        this.manager.registerWikiComponent(componentA);
        this.manager.registerWikiComponent(componentB);

        this.manager.unregisterWikiComponents(DOC_A);
        assertSame(componentB, getRegisteredComponent(this.wikiComponentManager));

        // The unregistered component isn't restored anymore.
        this.manager.unregisterWikiComponents(DOC_B);
        assertFalse(this.wikiComponentManager.hasComponent(TestRole.class, ROLE_HINT));
    }

    @Test
    void unregisterComponentWithLowerPriority() throws Exception
    {
        TestImplementation componentA = component(DOC_A, 1000);
        // A higher hint priority value means a lower priority, so this component doesn't replace the first one.
        TestImplementation componentB = component(DOC_B, 2000);
        this.manager.registerWikiComponent(componentA);
        this.manager.registerWikiComponent(componentB);
        assertSame(componentA, getRegisteredComponent(this.wikiComponentManager));

        this.manager.unregisterWikiComponents(DOC_B);

        assertSame(componentA, getRegisteredComponent(this.wikiComponentManager));
    }

    @Test
    void unregisterRestoresTheComponentWithTheHighestPriority() throws Exception
    {
        TestImplementation componentA = component(DOC_A, 1000);
        TestImplementation componentB = component(DOC_B, 500);
        TestImplementation componentC = component(DOC_C, 100);
        this.manager.registerWikiComponent(componentB);
        this.manager.registerWikiComponent(componentA);
        assertSame(componentB, getRegisteredComponent(this.wikiComponentManager));
        this.manager.registerWikiComponent(componentC);
        assertSame(componentC, getRegisteredComponent(this.wikiComponentManager));

        this.manager.unregisterWikiComponents(DOC_C);

        assertSame(componentB, getRegisteredComponent(this.wikiComponentManager));
    }

    @Test
    void unregisterDoesNotRestoreComponentFromAnotherWiki() throws Exception
    {
        this.manager.registerWikiComponent(component(OTHER_WIKI_DOC, 1000));
        this.manager.registerWikiComponent(component(DOC_A, 1000));

        this.manager.unregisterWikiComponents(DOC_A);

        assertFalse(this.wikiComponentManager.hasComponent(TestRole.class, ROLE_HINT));
    }

    @Test
    void unregisterDoesNotRestoreComponentOfAnotherUser() throws Exception
    {
        TestImplementation componentA =
            new TestImplementation(DOC_A, AUTHOR_REFERENCE, WikiComponentScope.USER, 42, 1000);
        TestImplementation componentB =
            new TestImplementation(DOC_B, OTHER_AUTHOR_REFERENCE, WikiComponentScope.USER, 42, 1000);
        TestImplementation componentC =
            new TestImplementation(DOC_C, AUTHOR_REFERENCE, WikiComponentScope.USER, 42, 1000);
        this.manager.registerWikiComponent(componentA);
        this.manager.registerWikiComponent(componentB);

        this.manager.unregisterWikiComponents(DOC_B);
        assertFalse(this.userComponentManager.hasComponent(TestRole.class, ROLE_HINT));

        // A component of the same user is restored.
        this.manager.registerWikiComponent(componentA);
        this.manager.registerWikiComponent(componentC);
        this.manager.unregisterWikiComponents(DOC_C);
        assertSame(componentA, getRegisteredComponent(this.userComponentManager));
    }

    @Test
    void unregisterComponentHiddenByParentComponent() throws Exception
    {
        EmbeddableComponentManager parentComponentManager = new EmbeddableComponentManager();
        TestRole parentComponent = mock();
        parentComponentManager.registerComponent(descriptor(parentComponent, 500), parentComponent);
        this.wikiComponentManager.setParent(parentComponentManager);

        TestImplementation component = component(DOC_A, 1000);
        this.manager.registerWikiComponent(component);
        // The component of the parent component manager has a higher priority.
        assertSame(parentComponent, getRegisteredComponent(this.wikiComponentManager));

        this.manager.unregisterWikiComponents(DOC_A);

        // The wiki component doesn't come back once the component of the parent component manager is unregistered.
        parentComponentManager.unregisterComponent(TestRole.class, ROLE_HINT);
        assertFalse(this.wikiComponentManager.hasComponent(TestRole.class, ROLE_HINT));
    }

    @Test
    void unregisterComponentReplacedByJavaComponent() throws Exception
    {
        TestImplementation component = component(DOC_A, 1000);
        this.manager.registerWikiComponent(component);
        TestRole javaComponent = mock();
        this.wikiComponentManager.registerComponent(descriptor(javaComponent, 1000), javaComponent);

        this.manager.unregisterWikiComponents(DOC_A);

        assertSame(javaComponent, getRegisteredComponent(this.wikiComponentManager));
    }

    @SuppressWarnings("unchecked")
    private ComponentDescriptor<TestRole> descriptor(TestRole implementation, int roleHintPriority)
    {
        DefaultComponentDescriptor<TestRole> descriptor = new DefaultComponentDescriptor<>();
        descriptor.setRoleType(TestRole.class);
        descriptor.setRoleHint(ROLE_HINT);
        descriptor.setImplementation((Class<? extends TestRole>) implementation.getClass());
        descriptor.setRoleHintPriority(roleHintPriority);
        return descriptor;
    }
}
