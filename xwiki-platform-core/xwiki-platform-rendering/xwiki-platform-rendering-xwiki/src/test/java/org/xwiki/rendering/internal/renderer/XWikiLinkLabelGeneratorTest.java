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
package org.xwiki.rendering.internal.renderer;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link XWikiLinkLabelGenerator}.
 *
 * @version $Id$
 * @since 2.0M1
 */
public class XWikiLinkLabelGeneratorTest
{
    @Rule
    public MockitoComponentMockingRule<XWikiLinkLabelGenerator> mocker =
        new MockitoComponentMockingRule<>(XWikiLinkLabelGenerator.class);

    @Before
    public void setUp() throws Exception
    {
        RenderingConfiguration configuration = this.mocker.getInstance(RenderingConfiguration.class);
        when(configuration.getLinkLabelFormat()).thenReturn(
            "%l%la%n%na%N%NA [%w:%s.%p] %ls %np %P %NP (%t) [%w:%s.%p] %ls %np %P %NP (%t)");

        EntityReferenceProvider entityReferenceProvider = this.mocker.getInstance(EntityReferenceProvider.class);
        when(entityReferenceProvider.getDefaultReference(EntityType.DOCUMENT)).thenReturn(
            new EntityReference("WebHome", EntityType.DOCUMENT));
    }

    @Test
    public void generateWhenTerminalPage() throws Exception
    {
        ResourceReference resourceReference = new DocumentResourceReference("HelloWorld");
        DocumentReference documentReference =
            new DocumentReference("wiki", Arrays.asList("space1", "space2"), "HelloWorld");

        EntityReferenceResolver<ResourceReference> resourceReferenceResolver = this.mocker.getInstance(
            new DefaultParameterizedType(null, EntityReferenceResolver.class, ResourceReference.class));
        when(resourceReferenceResolver.resolve(resourceReference, EntityType.DOCUMENT)).thenReturn(documentReference);

        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentModelBridge dmb = mock(DocumentModelBridge.class);
        when(dab.getTranslatedDocumentInstance(documentReference)).thenReturn(dmb);
        when(dmb.getTitle()).thenReturn("My title");

        EntityReferenceSerializer<String> localSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        when(localSerializer.serialize(new SpaceReference("wiki", "space1", "space2"))).thenReturn("space1.space2");

        assertEquals("%l%la%n%na%N%NA "
            + "[wiki:space1.space2.HelloWorld] space2 HelloWorld Hello World Hello World (My title) "
            + "[wiki:space1.space2.HelloWorld] space2 HelloWorld Hello World Hello World (My title)",
            this.mocker.getComponentUnderTest().generate(resourceReference));
    }

    @Test
    public void generateWhenNestedPage() throws Exception
    {
        ResourceReference resourceReference = new DocumentResourceReference("WebHome");
        DocumentReference documentReference =
            new DocumentReference("wiki", Arrays.asList("space1", "NestedPage"), "WebHome");

        EntityReferenceResolver<ResourceReference> resourceReferenceResolver = this.mocker.getInstance(
            new DefaultParameterizedType(null, EntityReferenceResolver.class, ResourceReference.class));
        when(resourceReferenceResolver.resolve(resourceReference, EntityType.DOCUMENT)).thenReturn(documentReference);

        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentModelBridge dmb = mock(DocumentModelBridge.class);
        when(dab.getTranslatedDocumentInstance(documentReference)).thenReturn(dmb);
        when(dmb.getTitle()).thenReturn("My title");

        EntityReferenceSerializer<String> localSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        when(localSerializer.serialize(new SpaceReference("wiki", "space1", "NestedPage"))).thenReturn(
            "space1.NestedPage");

        assertEquals("%l%la%n%na%N%NA "
            + "[wiki:space1.NestedPage.WebHome] NestedPage NestedPage Web Home Nested Page (My title) "
            + "[wiki:space1.NestedPage.WebHome] NestedPage NestedPage Web Home Nested Page (My title)",
            this.mocker.getComponentUnderTest().generate(resourceReference));
    }

    @Test
    public void generateWhenDocumentFailsToLoad() throws Exception
    {
        ResourceReference resourceReference = new DocumentResourceReference("HelloWorld");
        DocumentReference documentReference = new DocumentReference("xwiki", "Main", "HelloWorld");

        EntityReferenceResolver<ResourceReference> resourceReferenceResolver = this.mocker.getInstance(
            new DefaultParameterizedType(null, EntityReferenceResolver.class, ResourceReference.class));
        when(resourceReferenceResolver.resolve(resourceReference, EntityType.DOCUMENT)).thenReturn(documentReference);

        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        when(dab.getTranslatedDocumentInstance(documentReference)).thenThrow(new Exception("error"));

        EntityReferenceSerializer<String> localSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        when(localSerializer.serialize(new SpaceReference("xwiki", "Main"))).thenReturn("Main");

        assertEquals("%l%la%n%na%N%NA "
            + "[xwiki:Main.HelloWorld] Main HelloWorld Hello World Hello World (HelloWorld) "
            + "[xwiki:Main.HelloWorld] Main HelloWorld Hello World Hello World (HelloWorld)",
            this.mocker.getComponentUnderTest().generate(resourceReference));
    }

    @Test
    public void generateWhenDocumentTitleIsNull() throws Exception
    {
        ResourceReference resourceReference = new DocumentResourceReference("HelloWorld");
        DocumentReference documentReference = new DocumentReference("xwiki", "Main", "HelloWorld");

        EntityReferenceResolver<ResourceReference> resourceReferenceResolver = this.mocker.getInstance(
            new DefaultParameterizedType(null, EntityReferenceResolver.class, ResourceReference.class));
        when(resourceReferenceResolver.resolve(resourceReference, EntityType.DOCUMENT)).thenReturn(documentReference);

        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentModelBridge dmb = mock(DocumentModelBridge.class);
        when(dab.getTranslatedDocumentInstance(documentReference)).thenReturn(dmb);
        when(dmb.getTitle()).thenReturn(null);

        EntityReferenceSerializer<String> localSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        when(localSerializer.serialize(new SpaceReference("xwiki", "Main"))).thenReturn("Main");

        assertEquals("%l%la%n%na%N%NA "
            + "[xwiki:Main.HelloWorld] Main HelloWorld Hello World Hello World (HelloWorld) "
            + "[xwiki:Main.HelloWorld] Main HelloWorld Hello World Hello World (HelloWorld)",
            this.mocker.getComponentUnderTest().generate(resourceReference));
    }

    @Test
    public void generateWhithRegexpSyntax() throws Exception
    {
        ResourceReference resourceReference = new DocumentResourceReference("HelloWorld");
        DocumentReference documentReference = new DocumentReference("$0", "\\", "$0");

        EntityReferenceResolver<ResourceReference> resourceReferenceResolver = this.mocker.getInstance(
            new DefaultParameterizedType(null, EntityReferenceResolver.class, ResourceReference.class));
        when(resourceReferenceResolver.resolve(resourceReference, EntityType.DOCUMENT)).thenReturn(documentReference);

        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentModelBridge dmb = mock(DocumentModelBridge.class);
        when(dab.getTranslatedDocumentInstance(documentReference)).thenReturn(dmb);
        when(dmb.getTitle()).thenReturn("$0");

        EntityReferenceSerializer<String> localSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        when(localSerializer.serialize(new SpaceReference("$0", "\\"))).thenReturn("\\");

        assertEquals("%l%la%n%na%N%NA [$0:\\.$0] \\ $0 $0 $0 ($0) [$0:\\.$0] \\ $0 $0 $0 ($0)",
            this.mocker.getComponentUnderTest().generate(resourceReference));
    }

    @Test
    public void generateWhithPageNameWithPercent() throws Exception
    {
        ResourceReference resourceReference = new DocumentResourceReference("HelloWorld");
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page%t");

        EntityReferenceResolver<ResourceReference> resourceReferenceResolver = this.mocker.getInstance(
            new DefaultParameterizedType(null, EntityReferenceResolver.class, ResourceReference.class));
        when(resourceReferenceResolver.resolve(resourceReference, EntityType.DOCUMENT)).thenReturn(documentReference);

        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentModelBridge dmb = mock(DocumentModelBridge.class);
        when(dab.getTranslatedDocumentInstance(documentReference)).thenReturn(dmb);
        when(dmb.getTitle()).thenReturn("my title");

        EntityReferenceSerializer<String> localSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        when(localSerializer.serialize(new SpaceReference("wiki", "space"))).thenReturn("space");

        assertEquals("%l%la%n%na%N%NA "
            + "[wiki:space.page%t] space page%t page%t page%t (my title) "
            + "[wiki:space.page%t] space page%t page%t page%t (my title)",
            this.mocker.getComponentUnderTest().generate(resourceReference));
    }
}
