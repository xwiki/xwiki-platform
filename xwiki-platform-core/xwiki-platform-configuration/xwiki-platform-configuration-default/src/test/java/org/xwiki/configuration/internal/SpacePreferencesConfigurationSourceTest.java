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

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Unit tests for {@link org.xwiki.configuration.internal.SpacePreferencesConfigurationSource}.
 *
 * @version $Id$
 * @since 2.4M2
 */
public class SpacePreferencesConfigurationSourceTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private SpacePreferencesConfigurationSource source;

    @Test
    public void getPropertyForStringWhenExists() throws Exception
    {
        final DocumentReference webPreferencesReference = new DocumentReference("wiki", "space", "WebPreferences");
        final DocumentReference xwikiPreferencesReference = new DocumentReference("wiki", "XWiki", "XWikiPreferences");
        final DocumentReference currentDocument = new DocumentReference("wiki", "space", "page");

        final DocumentAccessBridge dab = getComponentManager().getInstance(DocumentAccessBridge.class);
        final ModelContext modelContext = getComponentManager().getInstance(ModelContext.class);
        getMockery().checking(new Expectations() {{
            allowing(dab).getCurrentDocumentReference();
                will(returnValue(currentDocument));
            allowing(modelContext).getCurrentEntityReference();
                will(returnValue(new WikiReference("wiki")));
            oneOf(dab).getProperty(webPreferencesReference, xwikiPreferencesReference, "key");
                will(returnValue("value"));
        }});

        String result = this.source.getProperty("key", String.class);

        Assert.assertEquals("value", result);
    }
}
