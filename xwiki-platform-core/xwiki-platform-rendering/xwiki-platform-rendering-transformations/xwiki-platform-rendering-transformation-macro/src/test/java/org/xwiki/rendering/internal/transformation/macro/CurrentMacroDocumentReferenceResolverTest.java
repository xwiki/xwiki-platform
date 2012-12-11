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

import java.util.Arrays;
import java.util.Collections;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.jmock.annotation.MockingRequirement;

/**
 * Unit tests for {@link CurrentMacroDocumentReferenceResolver}.
 *
 * @version $Id$
 * @since 4.3M1
 */
@MockingRequirement(CurrentMacroDocumentReferenceResolver.class)
public class CurrentMacroDocumentReferenceResolverTest
    extends AbstractMockingComponentTestCase<CurrentMacroDocumentReferenceResolver>
{
    @Test
    public void resolveWhenNoBlockPassed() throws Exception
    {
        try {
            getMockedComponent().resolve("something");
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
            getMockedComponent().resolve("something", "wrong param type must be Block");
            Assert.fail("Should have thrown an IllegalArgumentException here");
        } catch (IllegalArgumentException expected) {
            Assert.assertEquals("You must pass one parameter of type [org.xwiki.rendering.block.Block]",
                expected.getMessage());
        }
    }

    @Test
    public void resolveWhenNoMetaDataBlock() throws Exception
    {
        final DocumentReference expectedReference = new DocumentReference("wiki", "space", "page");
        final DocumentReferenceResolver currentDocumentReferenceResolver = getComponentManager().getInstance(
            DocumentReferenceResolver.TYPE_STRING, "current");
        getMockery().checking(new Expectations() {{
            oneOf(currentDocumentReferenceResolver).resolve("space.page");
            will(returnValue(expectedReference));
        }});

        final Block block = new WordBlock("whatever");
        DocumentReference reference = getMockedComponent().resolve("space.page", block);
        Assert.assertEquals(expectedReference, reference);
    }

    @Test
    public void resolveWhenMetaDataBlock() throws Exception
    {
        final DocumentReference expectedReference = new DocumentReference("basewiki", "basespace", "page");
        final DocumentReference baseReference = new DocumentReference("basewiki", "basespace", "basepage");
        final DocumentReferenceResolver currentDocumentReferenceResolver = getComponentManager().getInstance(
                DocumentReferenceResolver.TYPE_STRING, "current");
        getMockery().checking(new Expectations() {{
            oneOf(currentDocumentReferenceResolver).resolve("basewiki:basespace.basepage");
            will(returnValue(baseReference));
            oneOf(currentDocumentReferenceResolver).resolve("page", baseReference);
            will(returnValue(expectedReference));
        }});

        Block wordBlock = new WordBlock("whatever");
        MetaData metaData = new MetaData(
            Collections.<String, Object> singletonMap(MetaData.BASE, "basewiki:basespace.basepage"));
        new XDOM(Arrays.<Block>asList(new MetaDataBlock(Arrays.<Block>asList(wordBlock), metaData)));

        DocumentReference reference = getMockedComponent().resolve("page", wordBlock);
        Assert.assertEquals(expectedReference, reference);
    }
}
