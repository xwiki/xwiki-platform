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

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
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
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CurrentMacroEntityReferenceResolver}.
 * 
 * @version $Id$
 * @since 4.3M1
 */
@ComponentTest
class CurrentMacroEntityReferenceResolverTest
{
    @InjectMockComponents
    private CurrentMacroEntityReferenceResolver resolver;

    @MockComponent
    @Named("current")
    private EntityReferenceResolver<String> currentEntityReferenceResolver;

    @Test
    void resolveWhenNoBlockPassed()
    {
        Throwable exception = assertThrows(IllegalArgumentException.class,
            () -> this.resolver.resolve("something", EntityType.DOCUMENT));
        assertEquals("There must be at least one parameter, with the first parameter of type "
            + "[org.xwiki.rendering.block.Block]", exception.getMessage());
    }

    @Test
    void resolveWhenWrongParameterPassed()
    {
        Throwable exception = assertThrows(IllegalArgumentException.class,
            () -> this.resolver.resolve("something", EntityType.ATTACHMENT, "wrong param type must be Block"));
        assertEquals("There must be at least one parameter, with the first parameter of type "
            + "[org.xwiki.rendering.block.Block]", exception.getMessage());
    }

    @Test
    void resolveWhenNoMetaDataBlock()
    {
        EntityReference expectedReference = new DocumentReference("wiki", "Space", "Page");
        when(this.currentEntityReferenceResolver.resolve("Space.Page", EntityType.DOCUMENT))
            .thenReturn(expectedReference);

        Block block = new WordBlock("whatever");
        assertEquals(expectedReference, this.resolver.resolve("Space.Page", EntityType.DOCUMENT, block));
    }

    @Test
    void resolveWhenMetaDataBlock()
    {
        DocumentReference baseReference = new DocumentReference("basewiki", "basespace", "basepage");
        EntityReference expectedReference = new AttachmentReference("file", baseReference);
        when(this.currentEntityReferenceResolver.resolve("basewiki:basespace.basepage", EntityType.DOCUMENT))
            .thenReturn(baseReference);
        when(this.currentEntityReferenceResolver.resolve("file", EntityType.ATTACHMENT, baseReference))
            .thenReturn(expectedReference);

        Block wordBlock = new WordBlock("whatever");
        MetaData metaData =
            new MetaData(Collections.singletonMap(MetaData.BASE, "basewiki:basespace.basepage"));
        new XDOM(List.of(new MetaDataBlock(List.of(wordBlock), metaData)));

        assertEquals(expectedReference, this.resolver.resolve("file", EntityType.ATTACHMENT, wordBlock));
    }

    @Test
    void resolveWhenNoMetaDataBlockAndEntityReferenceParameter()
    {
        DocumentReference baseReference = new DocumentReference("basewiki", "basespace", "basepage");
        EntityReference expectedReference = new DocumentReference("Wiki", "Space", "Page");
        when(this.currentEntityReferenceResolver.resolve("Space.Page", EntityType.DOCUMENT, baseReference))
            .thenReturn(expectedReference);

        Block block = new WordBlock("whatever");
        assertEquals(expectedReference, this.resolver.resolve("Space.Page", EntityType.DOCUMENT, block, baseReference));
    }
}
