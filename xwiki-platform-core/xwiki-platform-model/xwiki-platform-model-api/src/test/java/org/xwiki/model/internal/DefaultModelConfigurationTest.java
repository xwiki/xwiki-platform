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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.internal.MemoryConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link DefaultModelConfiguration}.
 * 
 * @version $Id$
 * @since 2.2M1
 */
@ComponentTest
@ComponentList({
    RelativeStringEntityReferenceResolver.class,
    DefaultStringEntityReferenceSerializer.class
})
public class DefaultModelConfigurationTest
{
    @InjectMockComponents
    private DefaultModelConfiguration configuration;

    private MemoryConfigurationSource configurationSource;
    
    @BeforeEach
    public void before(MockitoComponentManager componentManager) throws Exception
    {
        this.configurationSource = componentManager.registerMemoryConfigurationSource();
    }

    @Test
    public void getDefaultReferenceNameWhenDefinedInConfiguration()
    {
        this.configurationSource.setProperty("model.reference.default.wiki", "defaultWiki");
        this.configurationSource.setProperty("model.reference.default.document", "defaultDocument");
        this.configurationSource.setProperty("model.reference.default.space", "defaultSpace");
        this.configurationSource.setProperty("model.reference.default.attachment", "defaultFilename");
        this.configurationSource.setProperty("model.reference.default.object", "defaultObject");
        this.configurationSource.setProperty("model.reference.default.object_property", "defaultProperty");

        assertEquals("defaultWiki", this.configuration.getDefaultReferenceValue(EntityType.WIKI));
        assertEquals("defaultDocument", this.configuration.getDefaultReferenceValue(EntityType.DOCUMENT));
        assertEquals("defaultSpace", this.configuration.getDefaultReferenceValue(EntityType.SPACE));
        assertEquals("defaultFilename", this.configuration.getDefaultReferenceValue(EntityType.ATTACHMENT));
        assertEquals("defaultObject", this.configuration.getDefaultReferenceValue(EntityType.OBJECT));
        assertEquals("defaultProperty", this.configuration.getDefaultReferenceValue(EntityType.OBJECT_PROPERTY));
    }

    @Test
    public void testGetDefaultReferenceNameWhenNotDefinedInConfiguration() throws ComponentLookupException
    {
        assertEquals("xwiki", this.configuration.getDefaultReferenceValue(EntityType.WIKI));
        assertEquals("WebHome", this.configuration.getDefaultReferenceValue(EntityType.DOCUMENT));
        assertEquals("Main", this.configuration.getDefaultReferenceValue(EntityType.SPACE));
        assertEquals("filename", this.configuration.getDefaultReferenceValue(EntityType.ATTACHMENT));
        assertEquals("object", this.configuration.getDefaultReferenceValue(EntityType.OBJECT));
        assertEquals("property", this.configuration.getDefaultReferenceValue(EntityType.OBJECT_PROPERTY));
    }
}
