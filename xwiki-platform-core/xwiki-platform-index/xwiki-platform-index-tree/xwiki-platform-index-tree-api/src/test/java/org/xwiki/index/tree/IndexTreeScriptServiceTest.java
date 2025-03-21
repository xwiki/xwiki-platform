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
package org.xwiki.index.tree;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link IndexTreeScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class IndexTreeScriptServiceTest
{
    @InjectMockComponents
    private IndexTreeScriptService scriptService;

    @MockComponent
    private EntityReferenceProvider entityReferenceProvider;

    @Test
    void computeDistanceBetweenNodes()
    {
        DocumentReference terminalDocumentReference = new DocumentReference("xwiki", List.of("Space", "Sub"), "Test");
        DocumentReference nonTerminalDocumentReference = new DocumentReference("xwiki", List.of("Space", "Sub"),
            "WebHome");
        SpaceReference rootReference = new SpaceReference("xwiki", List.of("Space"));
        DocumentReference rootDocumentReference = new DocumentReference("xwiki", List.of("Space"), "WebHome");
        when(this.entityReferenceProvider.getDefaultReference(EntityType.DOCUMENT)).thenReturn(
            new DocumentReference("xwiki", "Main", "WebHome"));

        assertEquals(2, scriptService.computeDistanceBetweenNodes(rootReference, terminalDocumentReference));
        assertEquals(2, scriptService.computeDistanceBetweenNodes(rootDocumentReference, terminalDocumentReference));
        assertEquals(1, scriptService.computeDistanceBetweenNodes(rootReference, nonTerminalDocumentReference));
        assertEquals(1, scriptService.computeDistanceBetweenNodes(rootDocumentReference, nonTerminalDocumentReference));

        WikiReference wikiReference = new WikiReference("xwiki");
        assertEquals(3, scriptService.computeDistanceBetweenNodes(wikiReference, terminalDocumentReference));
        assertEquals(2, scriptService.computeDistanceBetweenNodes(wikiReference, nonTerminalDocumentReference));

        assertEquals(-1, scriptService.computeDistanceBetweenNodes(new DocumentReference("foor", "XWiki", "Thing"),
            terminalDocumentReference));
    }
}