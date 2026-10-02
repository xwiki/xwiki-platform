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

import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.PageReferenceResolver;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.PageResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiDocumentWantedLinkTitleGenerator}.
 *
 * @version $Id$
 */
@ComponentTest
class XWikiDocumentWantedLinkTitleGeneratorTest
{
    private static final String KEY = "rendering.xwiki.wantedLink.page.label";

    @InjectMockComponents
    private XWikiDocumentWantedLinkTitleGenerator generator;

    @MockComponent
    private ContextualLocalizationManager contextLocalization;

    @MockComponent
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @MockComponent
    @Named("current")
    private PageReferenceResolver<String> currentPageReferenceResolver;

    @BeforeEach
    void setUp()
    {
        when(this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new EntityReference("WebHome", EntityType.DOCUMENT));
        when(this.contextLocalization.getTranslationPlain(any(), any()))
            .thenAnswer(invocation -> "Create page: " + invocation.getArgument(1));
    }

    @Test
    void generateWantedLinkTitleOnUntypedReferenceUsesTheDocumentName()
    {
        ResourceReference reference = new DocumentResourceReference("Space.UnknownPage");
        reference.setTyped(false);
        when(this.currentDocumentReferenceResolver.resolve("Space.UnknownPage"))
            .thenReturn(new DocumentReference("wiki", "Space", "UnknownPage"));

        assertEquals("Create page: UnknownPage", this.generator.generateWantedLinkTitle(reference));
    }

    @Test
    void generateWantedLinkTitleOnTypedReferenceUsesTheDocumentName()
    {
        ResourceReference reference = new DocumentResourceReference("Space.UnknownPage");
        reference.setTyped(true);
        when(this.currentDocumentReferenceResolver.resolve("Space.UnknownPage"))
            .thenReturn(new DocumentReference("wiki", "Space", "UnknownPage"));

        assertEquals("Create page: UnknownPage", this.generator.generateWantedLinkTitle(reference));
    }

    @Test
    void generateWantedLinkTitleOnNestedPageUsesTheSpaceNameInsteadOfTheDefaultDocumentName()
    {
        ResourceReference reference = new DocumentResourceReference("Fa.Fi.Foo.WebHome");
        reference.setTyped(true);
        when(this.currentDocumentReferenceResolver.resolve("Fa.Fi.Foo.WebHome"))
            .thenReturn(new DocumentReference("wiki", List.of("Fa", "Fi", "Foo"), "WebHome"));

        assertEquals("Create page: Foo", this.generator.generateWantedLinkTitle(reference));
    }

    @Test
    void generateWantedLinkTitleOnPageReferenceUsesThePageName()
    {
        ResourceReference reference = new PageResourceReference("Fa/Fi/Foo");
        when(this.currentPageReferenceResolver.resolve("Fa/Fi/Foo"))
            .thenReturn(new PageReference("wiki", "Fa", "Fi", "Foo"));

        assertEquals("Create page: Foo", this.generator.generateWantedLinkTitle(reference));
    }
}
