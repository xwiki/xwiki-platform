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
package org.xwiki.localization.internal;

import java.util.Collection;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.localization.TranslationBundle;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultTranslationBundleContext}.
 *
 * @version $Id$
 * @since 8.0RC1
 */
@ComponentTest
class DefaultTranslationBundleContextTest
{
    @InjectMockComponents
    private DefaultTranslationBundleContext translationBundleContext;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private Execution mockExecution;

    @MockComponent
    private ModelContext mockModelContext;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> mockContextComponentManagerProvider;

    private ExecutionContext mockExecutionContext;

    @BeforeEach
    void before()
    {
        this.mockExecutionContext = new ExecutionContext();

        when(this.mockExecution.getContext()).thenReturn(this.mockExecutionContext);
        when(this.mockModelContext.getCurrentEntityReference()).thenReturn(new WikiReference("currentWiki"));
    }

    @Test
    void getBundlesNewContext() throws Exception
    {
        // Map the context component manager to the test component manager for easier test setup.
        when(this.mockContextComponentManagerProvider.get()).thenReturn(this.componentManager);

        TranslationBundle mockTranslationBundle = this.componentManager.registerMockComponent(TranslationBundle.class);

        Collection<TranslationBundle> bundles = this.translationBundleContext.getBundles();

        // Verify that an internal bundles cache is created and stored in the ExecutionContext.
        assertNotNull(this.mockExecutionContext.getProperty(DefaultTranslationBundleContext.CKEY_BUNDLES));

        assertEquals(1, bundles.size());
        assertEquals(mockTranslationBundle, bundles.iterator().next());
    }

    @Test
    void getBundlesSwitchContext() throws Exception
    {
        // Mock the first wiki bundles.
        ComponentManager mockWiki1ComponentManager = mock(ComponentManager.class);
        when(this.mockContextComponentManagerProvider.get()).thenReturn(mockWiki1ComponentManager);

        TranslationBundle mockWiki1TranslationBundle = mock(TranslationBundle.class);
        List<TranslationBundle> wiki1Bundles = List.of(mockWiki1TranslationBundle);
        when(mockWiki1ComponentManager.<TranslationBundle>getInstanceList(TranslationBundle.class))
            .thenReturn(wiki1Bundles);

        Collection<TranslationBundle> firstBundles = this.translationBundleContext.getBundles();

        // Check the output
        assertEquals(1, firstBundles.size());
        assertEquals(mockWiki1TranslationBundle, firstBundles.iterator().next());

        // Switch context wiki to the second wiki.
        when(this.mockModelContext.getCurrentEntityReference()).thenReturn(new WikiReference("otherWiki"));

        // Mock the second wiki bundles.
        ComponentManager mockWiki2ComponentManager = mock(ComponentManager.class);
        when(this.mockContextComponentManagerProvider.get()).thenReturn(mockWiki2ComponentManager);

        TranslationBundle mockWiki2TranslationBundle = mock(TranslationBundle.class);
        List<TranslationBundle> wiki2Bundles = List.of(mockWiki2TranslationBundle);
        when(mockWiki2ComponentManager.<TranslationBundle>getInstanceList(TranslationBundle.class)).thenReturn(
            wiki2Bundles);

        Collection<TranslationBundle> secondBundles = this.translationBundleContext.getBundles();

        // Check the output.
        assertEquals(1, secondBundles.size());
        assertEquals(mockWiki2TranslationBundle, secondBundles.iterator().next());

        // Check that we get different results on different wikis.
        // Note: We are comparing values instead of the actual sets because we did not mock the compareTo method on the
        // bundles to simplify the test.
        assertNotEquals(firstBundles.iterator().next(), secondBundles.iterator().next());

        // Switch back to the first wiki and get the same (cached) first bundles.
        when(this.mockModelContext.getCurrentEntityReference()).thenReturn(new WikiReference("currentWiki"));

        Collection<TranslationBundle> thirdBundles = this.translationBundleContext.getBundles();

        assertEquals(firstBundles.iterator().next(), thirdBundles.iterator().next());
    }

    @Test
    void addBundlesToCurrentContext()
    {
        // Map the context component manager to the test component manager for easier test setup.
        when(this.mockContextComponentManagerProvider.get()).thenReturn(this.componentManager);

        Collection<TranslationBundle> bundles = this.translationBundleContext.getBundles();

        // No bundles.
        assertEquals(0, bundles.size());

        // Add a bundle
        TranslationBundle mockBundleToAdd = mock(TranslationBundle.class);
        this.translationBundleContext.addBundle(mockBundleToAdd);

        bundles = this.translationBundleContext.getBundles();

        // Check that it was added.
        assertEquals(1, bundles.size());
        assertTrue(bundles.contains(mockBundleToAdd));
    }
}
