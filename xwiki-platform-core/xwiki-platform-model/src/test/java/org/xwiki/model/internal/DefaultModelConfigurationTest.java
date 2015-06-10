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
package org.xwiki.model.internal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.internal.MemoryConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link DefaultModelConfiguration}.
 * 
 * @version $Id$
 * @since 2.2M1
 */
@ComponentList({RelativeStringEntityReferenceResolver.class, DefaultStringEntityReferenceSerializer.class})
public class DefaultModelConfigurationTest
{
    @Rule
    public final MockitoComponentMockingRule<ModelConfiguration> mocker =
        new MockitoComponentMockingRule<ModelConfiguration>(DefaultModelConfiguration.class);

    private MemoryConfigurationSource configuration;

    @Before
    public void before() throws Exception
    {
        this.configuration = this.mocker.registerMemoryConfigurationSource();
    }

    @Test
    public void testGetDefaultReferenceNameWhenDefinedInConfiguration() throws ComponentLookupException
    {
        this.configuration.setProperty("model.reference.default.wiki", "defaultWiki");
        this.configuration.setProperty("model.reference.default.document", "defaultDocument");
        this.configuration.setProperty("model.reference.default.space", "defaultSpace");
        this.configuration.setProperty("model.reference.default.attachment", "defaultFilename");
        this.configuration.setProperty("model.reference.default.object", "defaultObject");
        this.configuration.setProperty("model.reference.default.object_property", "defaultProperty");

        Assert.assertEquals("defaultWiki", this.mocker.getComponentUnderTest().getDefaultReferenceValue(EntityType.WIKI));
        Assert.assertEquals("defaultDocument", this.mocker.getComponentUnderTest().getDefaultReferenceValue(EntityType.DOCUMENT));
        Assert.assertEquals("defaultSpace", this.mocker.getComponentUnderTest().getDefaultReferenceValue(EntityType.SPACE));
        Assert.assertEquals("defaultFilename", this.mocker.getComponentUnderTest().getDefaultReferenceValue(EntityType.ATTACHMENT));
        Assert.assertEquals("defaultObject", this.mocker.getComponentUnderTest().getDefaultReferenceValue(EntityType.OBJECT));
        Assert.assertEquals("defaultProperty", this.mocker.getComponentUnderTest().getDefaultReferenceValue(EntityType.OBJECT_PROPERTY));
    }

    @Test
    public void testGetDefaultReferenceNameWhenNotDefinedInConfiguration() throws ComponentLookupException
    {
        Assert.assertEquals("xwiki", this.mocker.getComponentUnderTest().getDefaultReferenceValue(EntityType.WIKI));
        Assert.assertEquals("WebHome", this.mocker.getComponentUnderTest().getDefaultReferenceValue(EntityType.DOCUMENT));
        Assert.assertEquals("Main", this.mocker.getComponentUnderTest().getDefaultReferenceValue(EntityType.SPACE));
        Assert.assertEquals("filename", this.mocker.getComponentUnderTest().getDefaultReferenceValue(EntityType.ATTACHMENT));
        Assert.assertEquals("object", mocker.getComponentUnderTest().getDefaultReferenceValue(EntityType.OBJECT));
        Assert.assertEquals("property", mocker.getComponentUnderTest().getDefaultReferenceValue(EntityType.OBJECT_PROPERTY));
    }
}
