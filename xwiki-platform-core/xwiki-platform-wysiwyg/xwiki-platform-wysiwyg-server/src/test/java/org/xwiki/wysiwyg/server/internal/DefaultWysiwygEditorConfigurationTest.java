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
package org.xwiki.wysiwyg.server.internal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wysiwyg.server.WysiwygEditorConfiguration;

import com.xpn.xwiki.XWikiContext;

/**
 * Unit tests for {@link DefaultWysiwygEditorConfiguration}.
 * 
 * @version $Id$
 */
public class DefaultWysiwygEditorConfigurationTest
{
    @Rule
    public MockitoComponentMockingRule<WysiwygEditorConfiguration> mocker =
        new MockitoComponentMockingRule<WysiwygEditorConfiguration>(DefaultWysiwygEditorConfiguration.class);

    @Before
    public void configure() throws Exception
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getMainXWiki()).thenReturn("chess");

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getProperty("xwikicontext")).thenReturn(xcontext);

        Execution execution = mocker.getInstance(Execution.class);
        when(execution.getContext()).thenReturn(executionContext);
    }

    @Test
    public void getPropertyWithFallBack() throws Exception
    {
        ModelContext modelContext = mocker.getInstance(ModelContext.class);
        when(modelContext.getCurrentEntityReference()).thenReturn(new EntityReference("tennis", EntityType.WIKI));

        DocumentAccessBridge dab = mocker.getInstance(DocumentAccessBridge.class);
        when(
            dab.getProperty(new DocumentReference("tennis", "XWiki", "WysiwygEditorConfig"), new DocumentReference(
                "tennis", "XWiki", "WysiwygEditorConfigClass"), "plugins")).thenReturn(null);
        when(
            dab.getProperty(new DocumentReference("chess", "XWiki", "WysiwygEditorConfig"), new DocumentReference(
                "chess", "XWiki", "WysiwygEditorConfigClass"), "plugins")).thenReturn("foo bar");

        assertEquals("foo bar", mocker.getComponentUnderTest().getPlugins());
    }
}
