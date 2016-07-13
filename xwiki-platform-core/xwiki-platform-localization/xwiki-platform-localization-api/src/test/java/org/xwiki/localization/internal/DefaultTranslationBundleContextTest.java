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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.localization.TranslationBundle;
import org.xwiki.localization.TranslationBundleContext;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultTranslationBundleContext}.
 *
 * @version $Id$
 * @since 8.0RC1
 */
public class DefaultTranslationBundleContextTest
{
    @Rule
    public final MockitoComponentMockingRule<TranslationBundleContext> mocker =
        new MockitoComponentMockingRule<TranslationBundleContext>(DefaultTranslationBundleContext.class);

    private ExecutionContext mockExecutionContext;

    private ModelContext mockModelContext;

    private Provider<ComponentManager> mockContextComponentManagerProvider;

    @Before
    public void before() throws Exception
    {
        this.mockExecutionContext = new ExecutionContext();

        Execution mockExecution = this.mocker.getInstance(Execution.class);
        when(mockExecution.getContext()).thenReturn(mockExecutionContext);

        this.mockModelContext = this.mocker.getInstance(ModelContext.class);
        when(this.mockModelContext.getCurrentEntityReference()).thenReturn(new WikiReference("currentWiki"));

        this.mockContextComponentManagerProvider =
            this.mocker.registerMockComponent(
                new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
    }

    @Test
    public void getBundlesNewContext() throws Exception
    {
        // Map the context component manager to the test component manager for easier test setup.
        when(this.mockContextComponentManagerProvider.get()).thenReturn(this.mocker);

        TranslationBundle mockTranslationBundle = this.mocker.registerMockComponent(TranslationBundle.class);

        Collection<TranslationBundle> bundles = this.mocker.getComponentUnderTest().getBundles();

        // Verify that an internal bundles cache is created and stored in the ExecutionContext.
        assertNotNull(this.mockExecutionContext.getProperty(DefaultTranslationBundleContext.CKEY_BUNDLES));

        assertEquals(1, bundles.size());
        assertEquals(mockTranslationBundle, bundles.iterator().next());
    }

    @Test
    public void getBundlesSwitchContext() throws Exception
    {
        // Mock the first wiki bundles.
        ComponentManager mockWiki1ComponentManager = mock(ComponentManager.class);
        when(this.mockContextComponentManagerProvider.get()).thenReturn(mockWiki1ComponentManager);

        TranslationBundle mockWiki1TranslationBundle = mock(TranslationBundle.class);
        List<TranslationBundle> wiki1Bundles = Arrays.asList(mockWiki1TranslationBundle);
        when(mockWiki1ComponentManager.<TranslationBundle>getInstanceList(TranslationBundle.class)).thenReturn(
            wiki1Bundles);

        Collection<TranslationBundle> firstBundles = this.mocker.getComponentUnderTest().getBundles();

        // Check the output
        assertEquals(1, firstBundles.size());
        assertEquals(mockWiki1TranslationBundle, firstBundles.iterator().next());

        // Switch context wiki to the second wiki.
        when(this.mockModelContext.getCurrentEntityReference()).thenReturn(new WikiReference("otherWiki"));

        // Mock the second wiki bundles.
        ComponentManager mockWiki2ComponentManager = mock(ComponentManager.class);
        when(this.mockContextComponentManagerProvider.get()).thenReturn(mockWiki2ComponentManager);

        TranslationBundle mockWiki2TranslationBundle = mock(TranslationBundle.class);
        List<TranslationBundle> wiki2Bundles = Arrays.asList(mockWiki2TranslationBundle);
        when(mockWiki2ComponentManager.<TranslationBundle>getInstanceList(TranslationBundle.class)).thenReturn(
            wiki2Bundles);

        Collection<TranslationBundle> secondBundles = this.mocker.getComponentUnderTest().getBundles();

        // Check the output.
        assertEquals(1, secondBundles.size());
        assertEquals(mockWiki2TranslationBundle, secondBundles.iterator().next());

        // Check that we get different results on different wikis.
        // Note: We are comparing values instead of the actual sets because we did not mock the compareTo method on the
        // bundles to simplify the test.
        assertNotEquals(firstBundles.iterator().next(), secondBundles.iterator().next());

        // Switch back to the first wiki and get the same (cached) first bundles.
        when(this.mockModelContext.getCurrentEntityReference()).thenReturn(new WikiReference("currentWiki"));

        Collection<TranslationBundle> thirdBundles = this.mocker.getComponentUnderTest().getBundles();

        assertEquals(firstBundles.iterator().next(), thirdBundles.iterator().next());
    }

    @Test
    public void addBundlesToCurrentContext() throws Exception
    {
        // Map the context component manager to the test component manager for easier test setup.
        when(this.mockContextComponentManagerProvider.get()).thenReturn(this.mocker);

        Collection<TranslationBundle> bundles = this.mocker.getComponentUnderTest().getBundles();

        // No bundles.
        assertEquals(0, bundles.size());

        // Add a bundle
        TranslationBundle mockBundleToAdd = mock(TranslationBundle.class);
        this.mocker.getComponentUnderTest().addBundle(mockBundleToAdd);

        bundles = this.mocker.getComponentUnderTest().getBundles();

        // Check that it was added.
        assertEquals(1, bundles.size());
        assertTrue(bundles.contains(mockBundleToAdd));
    }
}
