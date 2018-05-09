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
package org.xwiki.index.tree.internal.nestedpages;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FarmTreeNode}.
 * 
 * @version $Id$
 */
@ComponentTest
public class FarmTreeNodeTest
{
    @InjectMockComponents
    private FarmTreeNode farmTreeNode;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    @Named("current")
    private EntityReferenceResolver<String> currentEntityReferenceResolver;

    @BeforeEach
    public void before() throws Exception
    {
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(Arrays.asList("one", "two", "three"));
        when(this.currentEntityReferenceResolver.resolve("two", EntityType.WIKI)).thenReturn(new WikiReference("two"));
    }

    @Test
    public void getChildCount()
    {
        assertEquals(3, this.farmTreeNode.getChildCount("anyId"));

        this.farmTreeNode.getProperties().put("exclusions",
            new HashSet<>(Arrays.asList("wiki:two", "document:three:Path.To.Page", "foo:bar")));
        when(this.currentEntityReferenceResolver.resolve("three:Path.To.Page", EntityType.DOCUMENT))
            .thenReturn(new DocumentReference("three", Arrays.asList("Path", "To"), "Page"));

        assertEquals(2, this.farmTreeNode.getChildCount("anyId"));
    }

    @Test
    public void getChildren()
    {
        assertEquals(Arrays.asList("wiki:one", "wiki:two", "wiki:three"), this.farmTreeNode.getChildren("foo", 0, 5));
        assertEquals(Arrays.asList("wiki:two"), this.farmTreeNode.getChildren("foo", 1, 1));

        this.farmTreeNode.getProperties().put("exclusions", Collections.singleton("wiki:two"));

        assertEquals(Arrays.asList("wiki:three"), this.farmTreeNode.getChildren("foo", 1, 1));
    }

    @Test
    public void getParent()
    {
        assertNull(this.farmTreeNode.getParent("anyId"));
    }
}
