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
package org.xwiki.filter.instance.internal.input;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.filter.instance.input.InstanceInputEventGenerator;
import org.xwiki.filter.instance.input.InstanceInputProperties;
import org.xwiki.filter.instance.internal.InstanceFilter;
import org.xwiki.filter.instance.internal.InstanceModel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSet;
import org.xwiki.model.reference.EntityReferenceTree;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InstanceInputFilterStream}.
 *
 * @version $Id$
 */
@ComponentTest
class InstanceInputFilterStreamTest
{
    @InjectMockComponents
    private InstanceInputFilterStream filterStream;

    @MockComponent
    private InstanceModel instanceModel;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Mock
    private ComponentManager componentManager;

    @Mock
    private InstanceFilter proxyFilter;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.componentManagerProvider.get()).thenReturn(this.componentManager);
        when(this.componentManager.getInstanceList(InstanceInputEventGenerator.class))
            .thenReturn(List.of());
    }

    /**
     * Tests that when exporting a single document in a deeply nested space, sibling spaces are not traversed.
     * This is a regression test for a performance issue where exporting a single document would query all
     * spaces in the wiki, causing massive overhead on large wikis.
     */
    @Test
    void exportSingleDocumentDoesNotTraverseSiblingSpaces() throws Exception
    {
        WikiReference wiki = new WikiReference("wiki");
        
        // The document we want to export: wiki:A.B.C.document
        DocumentReference targetDocument = new DocumentReference("wiki", List.of("A", "B", "C"), "document");

        // Create spaces: A, A.B, A.B.C (the path to our document) and A.X, A.B.Y (sibling spaces)
        SpaceReference spaceA = new SpaceReference("wiki", "A");
        SpaceReference spaceAB = new SpaceReference("B", spaceA);
        SpaceReference spaceABC = new SpaceReference("C", spaceAB);
        SpaceReference spaceAX = new SpaceReference("X", spaceA);
        SpaceReference spaceABY = new SpaceReference("Y", spaceAB);

        // Build a tree that contains all spaces (simulating what the real InstanceModel returns)
        EntityReferenceTree tree = new EntityReferenceTree(List.of(spaceA, spaceAB, spaceABC, spaceAX, spaceABY));

        // Configure the instance model
        when(this.instanceModel.getWikiReferences()).thenReturn(List.of(wiki));
        when(this.instanceModel.getSpaceReferences(wiki)).thenReturn(tree.getChildren().iterator().next());
        
        // Only A.B.C contains our target document
        when(this.instanceModel.getDocumentReferences(spaceA)).thenReturn(Collections.emptyList());
        when(this.instanceModel.getDocumentReferences(spaceAB)).thenReturn(Collections.emptyList());
        when(this.instanceModel.getDocumentReferences(spaceABC)).thenReturn(List.of(targetDocument));
        when(this.instanceModel.getDocumentReferences(spaceAX)).thenReturn(Collections.emptyList());
        when(this.instanceModel.getDocumentReferences(spaceABY)).thenReturn(Collections.emptyList());

        // Set up the filter to only include our target document
        EntityReferenceSet entities = new EntityReferenceSet();
        entities.includes(targetDocument);

        InstanceInputProperties properties = new InstanceInputProperties();
        properties.setEntities(entities);
        properties.setVerbose(false);

        this.filterStream.setProperties(properties);

        // Execute the export
        this.filterStream.read(this.proxyFilter, this.proxyFilter);

        // Verify that only the path to our document was traversed (A, A.B, A.B.C)
        verify(this.proxyFilter).beginWikiSpace(eq("A"), any());
        verify(this.proxyFilter).beginWikiSpace(eq("B"), any());
        verify(this.proxyFilter).beginWikiSpace(eq("C"), any());
        verify(this.proxyFilter).beginWikiDocument(eq("document"), any());

        // Verify that sibling spaces were NOT traversed
        verify(this.proxyFilter, never()).beginWikiSpace(eq("X"), any());
        verify(this.proxyFilter, never()).beginWikiSpace(eq("Y"), any());

        // Verify that getDocumentReferences was NOT called for sibling spaces
        verify(this.instanceModel, never()).getDocumentReferences(spaceAX);
        verify(this.instanceModel, never()).getDocumentReferences(spaceABY);
    }
}
