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
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.jmock.annotation.MockingRequirement;

/**
 * Unit tests for {@link UserPreferencesConfigurationSource}.
 *
 * @version $Id$
 * @since 4.0RC1
 */
@MockingRequirement(UserPreferencesConfigurationSource.class)
public class UserPreferencesConfigurationSourceTest extends AbstractMockingComponentTestCase
{
    private ConfigurationSource source;

    @Before
    public void configure() throws Exception
    {
        this.source = getComponentManager().getInstance(ConfigurationSource.class, "user");

        final DocumentReference userPreferencesReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        final DocumentReference currentUserReference = new DocumentReference("xwiki", "XWiki", "User");
        final DocumentAccessBridge dab = getComponentManager().getInstance(DocumentAccessBridge.class);

        getMockery().checking(new Expectations() {{
            allowing(dab).getCurrentUserReference();
                will(returnValue(currentUserReference));
            oneOf(dab).getProperty(currentUserReference, userPreferencesReference, "key");
                will(returnValue("value"));
        }});
    }

    @Test
    public void getStringPropertyWhenUserInTheSameWiki() throws Exception
    {
        final ModelContext modelContext = getComponentManager().getInstance(ModelContext.class);
        getMockery().checking(new Expectations() {{
            allowing(modelContext).getCurrentEntityReference();
                will(returnValue(new WikiReference("xwiki")));
        }});
        String result = this.source.getProperty("key", String.class);

        Assert.assertEquals("value", result);
    }

    @Test
    public void getStringPropertyWhenUserInADifferentWiki() throws Exception
    {
        final ModelContext modelContext = getComponentManager().getInstance(ModelContext.class);
        getMockery().checking(new Expectations() {{
            allowing(modelContext).getCurrentEntityReference();
                will(returnValue(new WikiReference("subwiki")));
        }});
        String result = this.source.getProperty("key", String.class);

        Assert.assertEquals("value", result);
    }
}
