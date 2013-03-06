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
package org.xwiki.rendering.internal.transformation.macro;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link CurrentMacroEntityReferenceResolver}.
 * 
 * @version $Id$
 * @since 4.3M1
 */
public class CurrentMacroEntityReferenceResolverTest
{
    @Rule
    public MockitoComponentMockingRule<EntityReferenceResolver<String>> mocker =
        new MockitoComponentMockingRule<EntityReferenceResolver<String>>(CurrentMacroEntityReferenceResolver.class);

    @Test
    public void resolveWhenNoBlockPassed() throws Exception
    {
        try {
            mocker.getComponentUnderTest().resolve("something", EntityType.DOCUMENT);
            Assert.fail("Should have thrown an IllegalArgumentException here");
        } catch (IllegalArgumentException expected) {
            Assert.assertEquals("You must pass one parameter of type [org.xwiki.rendering.block.Block]",
                expected.getMessage());
        }
    }

    @Test
    public void resolveWhenWrongParameterPassed() throws Exception
    {
        try {
            mocker.getComponentUnderTest()
                .resolve("something", EntityType.ATTACHMENT, "wrong param type must be Block");
            Assert.fail("Should have thrown an IllegalArgumentException here");
        } catch (IllegalArgumentException expected) {
            Assert.assertEquals("You must pass one parameter of type [org.xwiki.rendering.block.Block]",
                expected.getMessage());
        }
    }

    @Test
    public void resolveWhenNoMetaDataBlock() throws Exception
    {
        EntityReference expectedReference = new DocumentReference("wiki", "Space", "Page");
        EntityReferenceResolver<String> currentEntityReferenceResolver =
            mocker.getInstance(EntityReferenceResolver.TYPE_STRING, "current");
        when(currentEntityReferenceResolver.resolve("Space.Page", EntityType.DOCUMENT)).thenReturn(expectedReference);

        Block block = new WordBlock("whatever");
        Assert.assertEquals(expectedReference,
            mocker.getComponentUnderTest().resolve("Space.Page", EntityType.DOCUMENT, block));
    }

    @Test
    public void resolveWhenMetaDataBlock() throws Exception
    {
        DocumentReference baseReference = new DocumentReference("basewiki", "basespace", "basepage");
        EntityReference expectedReference = new AttachmentReference("file", baseReference);
        EntityReferenceResolver<String> currentEntityReferenceResolver =
            mocker.getInstance(EntityReferenceResolver.TYPE_STRING, "current");
        when(currentEntityReferenceResolver.resolve("basewiki:basespace.basepage", EntityType.DOCUMENT)).thenReturn(
            baseReference);
        when(currentEntityReferenceResolver.resolve("file", EntityType.ATTACHMENT, baseReference)).thenReturn(
            expectedReference);

        Block wordBlock = new WordBlock("whatever");
        MetaData metaData =
            new MetaData(Collections.<String, Object> singletonMap(MetaData.BASE, "basewiki:basespace.basepage"));
        new XDOM(Arrays.<Block> asList(new MetaDataBlock(Arrays.<Block> asList(wordBlock), metaData)));

        Assert.assertEquals(expectedReference,
            mocker.getComponentUnderTest().resolve("file", EntityType.ATTACHMENT, wordBlock));
    }
}
