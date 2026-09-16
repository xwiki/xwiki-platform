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
package org.xwiki.notifications.filters.internal;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LocationOperatorNodeGenerator}.
 *
 * @version $Id$
 */
@ComponentTest
class LocationOperatorNodeGeneratorTest
{
    @InjectMockComponents
    private LocationOperatorNodeGenerator generator;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    private final DocumentReference documentReference = new DocumentReference("wikiA", "SpaceB", "PageC");

    @Test
    void generateNodeForDocument()
    {
        when(this.localSerializer.serialize(this.documentReference)).thenReturn("SpaceB.PageC");

        assertEquals("(WIKI = \"wikiA\" AND PAGE = \"SpaceB.PageC\")",
            this.generator.generateNode(this.documentReference).toString());
    }

    @Test
    void generateNodeForSpace()
    {
        SpaceReference location = this.documentReference.getLastSpaceReference();
        when(this.localSerializer.serialize(location)).thenReturn("SpaceB");

        assertEquals("(WIKI = \"wikiA\" AND SPACE STARTS WITH \"SpaceB\")",
            this.generator.generateNode(location).toString());
    }

    @Test
    void generateNodeForWiki()
    {
        assertEquals("WIKI = \"wikiA\"",
            this.generator.generateNode(this.documentReference.getWikiReference()).toString());
    }

    @Test
    void generateNodeForUnsupportedLocationType()
    {
        AttachmentReference location = new AttachmentReference("file.txt", this.documentReference);

        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> this.generator.generateNode(location));
        assertEquals("Unsupported location type [ATTACHMENT], expected a document, a space or a wiki reference.",
            exception.getMessage());
    }
}
