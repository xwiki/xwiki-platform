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
package org.xwiki.configuration.internal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link org.xwiki.configuration.internal.WikiPreferencesConfigurationSource}.
 * 
 * @version $Id: 31e2e0d488d6f5dbc1fcec1211d30dc30000b5eb 
 */
public class WikiPreferencesConfigurationSourceTest
{
    @Rule
    public final MockitoComponentMockingRule<ConfigurationSource> componentManager =
        new MockitoComponentMockingRule<ConfigurationSource>(WikiPreferencesConfigurationSource.class);

    @Test
    public void getPropertyForStringWhenExists() throws Exception
    {
        final DocumentReference xwikiPreferencesReference = new DocumentReference("wiki", "XWiki", "XWikiPreferences");

        final DocumentAccessBridge dab = this.componentManager.getInstance(DocumentAccessBridge.class);
        final ModelContext modelContext = this.componentManager.getInstance(ModelContext.class);

        when(dab.getCurrentDocumentReference()).thenReturn(xwikiPreferencesReference);
        when(modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("wiki"));

        when(dab.getProperty(xwikiPreferencesReference, xwikiPreferencesReference, "key")).thenReturn("value");

        String result = this.componentManager.getComponentUnderTest().getProperty("key", String.class);

        verify(dab).getProperty(xwikiPreferencesReference, xwikiPreferencesReference, "key");
        Assert.assertEquals("value", result);
    }
}
