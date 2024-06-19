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
package org.xwiki.index.tree.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.properties.converter.Converter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Unit tests for {@link FarmTreeNode}.
 * 
 * @version $Id$
 */
@ComponentTest
class FarmTreeNodeTest
{
    @InjectMockComponents
    private FarmTreeNode farmTreeNode;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    @Named("entityTreeNodeId")
    private Converter<EntityReference> entityTreeNodeIdConverter;

    @BeforeEach
    void before() throws Exception
    {
        when(this.wikiDescriptorManager.getAll()).thenReturn(Arrays.asList(new WikiDescriptor("one", null),
            new WikiDescriptor("two", null), new WikiDescriptor("three", null)));
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "wiki:two"))
            .thenReturn(new WikiReference("two"));
    }

    @Test
    void getChildCount()
    {
        assertEquals(3, this.farmTreeNode.getChildCount("anyId"));

        this.farmTreeNode.getProperties().put("exclusions",
            new HashSet<>(Arrays.asList("wiki:two", "document:three:Path.To.one", "foo:bar", "space:wiki:three")));
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:three:Path.To.one"))
            .thenReturn(new DocumentReference("three", Arrays.asList("Path", "To"), "one"));
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "space:wiki:three"))
            .thenReturn(new SpaceReference("wiki", "three"));

        assertEquals(2, this.farmTreeNode.getChildCount("anyId"));
    }

    @Test
    void getChildren()
    {
        assertEquals(Arrays.asList("wiki:one", "wiki:two", "wiki:three"), this.farmTreeNode.getChildren("foo", 0, 5));
        assertEquals(Arrays.asList("wiki:two"), this.farmTreeNode.getChildren("foo", 1, 1));

        this.farmTreeNode.getProperties().put("exclusions", Collections.singleton("wiki:two"));

        assertEquals(Arrays.asList("wiki:three"), this.farmTreeNode.getChildren("foo", 1, 1));
    }

    @Test
    void getParent()
    {
        assertNull(this.farmTreeNode.getParent("anyId"));
    }
}
