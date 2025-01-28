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
package org.xwiki.rendering.internal.resolver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.DefaultReferenceAttachmentReferenceResolver;
import org.xwiki.model.internal.reference.DefaultReferenceDocumentReferenceResolver;
import org.xwiki.model.internal.reference.DefaultReferenceEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringAttachmentReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringDocumentReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringPageReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringSpaceReferenceResolver;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.PageResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.listener.reference.SpaceResourceReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DefaultResourceReferenceEntityReferenceResolver}.
 *
 * @version $Id$
 */
// @formatter:off
@ComponentList({
    DefaultResourceReferenceEntityReferenceResolver.class,
    AttachmentResourceReferenceEntityReferenceResolver.class,
    DocumentResourceReferenceEntityReferenceResolver.class,
    DefaultStringAttachmentReferenceResolver.class,
    DefaultStringDocumentReferenceResolver.class,
    SpaceResourceReferenceEntityReferenceResolver.class,
    DefaultReferenceEntityReferenceResolver.class,
    DefaultStringEntityReferenceResolver.class,
    RelativeStringEntityReferenceResolver.class,
    DefaultReferenceAttachmentReferenceResolver.class,
    DefaultReferenceDocumentReferenceResolver.class,
    DefaultStringSpaceReferenceResolver.class,
    ContextComponentManagerProvider.class,
    DefaultSymbolScheme.class,
    PageResourceReferenceEntityReferenceResolver.class,
    DefaultStringPageReferenceResolver.class
})
@ComponentTest
// @formatter:on
class DefaultResourceReferenceEntityReferenceResolverTest
{
    private static final String DEFAULT_PAGE = "defaultpage";

    private static final String CURRENT_PAGE = "currentpage";

    private static final String CURRENT_SPACE = "currentspace";

    private static final String CURRENT_SUBSPACE = "currentsubspace";

    private static final String CURRENT_WIKI = "currentwiki";

    private static final String WIKI = "Wiki";

    private static final String SPACE = "Space";

    private static final String PAGE = "Page";

    private static final String ATTACHMENT = "file.ext";

    @InjectMockComponents
    private DefaultResourceReferenceEntityReferenceResolver resolver;

    @MockComponent
    @Named("current")
    private EntityReferenceResolver<String> currentEntityReferenceResolver;

    @MockComponent
    @Named("current")
    private Provider<DocumentReference> currentDocumentProvider;

    @MockComponent
    private DocumentAccessBridge bridge;

    @MockComponent
    private EntityReferenceProvider defaultEntityProvider;

    private final Set<DocumentReference> existingDocuments = new HashSet<>();

    @BeforeEach
    void before() throws Exception
    {
        when(this.currentDocumentProvider.get())
            .thenReturn(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, CURRENT_PAGE));

        when(this.bridge.exists(any(DocumentReference.class))).then(
            (Answer<Boolean>) invocation -> existingDocuments.contains(invocation.getArguments()[0]));

        when(this.defaultEntityProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new EntityReference(DEFAULT_PAGE, EntityType.DOCUMENT));
    }

    private DocumentResourceReference documentResource(String referenceString, boolean typed)
    {
        DocumentResourceReference reference = new DocumentResourceReference(referenceString);
        reference.setTyped(typed);
        return reference;
    }

    private SpaceResourceReference spaceResource(String referenceString, boolean typed)
    {
        SpaceResourceReference reference = new SpaceResourceReference(referenceString);
        reference.setTyped(typed);
        return reference;
    }

    private AttachmentResourceReference attachmentResource(String referenceString, boolean typed)
    {
        AttachmentResourceReference reference = new AttachmentResourceReference(referenceString);
        reference.setTyped(typed);
        return reference;
    }

    private PageResourceReference pageResource(String referenceString, boolean typed)
    {
        PageResourceReference pageReference = new PageResourceReference(referenceString);
        pageReference.setTyped(typed);
        return pageReference;
    }
    // Tests

    @Test
    void resolve()
    {
        assertNull(this.resolver.resolve(null, null));
        assertNull(this.resolver.resolve(new ResourceReference("path", ResourceType.PATH), null));
    }

    @Test
    void resolveTypeDocument()
    {
        assertEquals(new DocumentReference(WIKI, SPACE, PAGE),
            this.resolver.resolve(documentResource(WIKI + ':' + SPACE + '.' + PAGE, true), null));

        assertEquals(new DocumentReference(CURRENT_WIKI, SPACE, PAGE),
            this.resolver.resolve(documentResource(SPACE + '.' + PAGE, true), null));

        assertEquals(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, PAGE),
            this.resolver.resolve(documentResource(PAGE, true), null));

        assertEquals(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, CURRENT_PAGE),
            this.resolver.resolve(documentResource("", true), null));

        when(this.currentEntityReferenceResolver.resolve(eq(WIKI + ':' + SPACE + '.' + PAGE), eq(EntityType.DOCUMENT),
            any())).thenReturn(new DocumentReference(WIKI, SPACE, PAGE));

        ResourceReference withBaseReference = documentResource("", true);
        withBaseReference.addBaseReference(WIKI + ':' + SPACE + '.' + PAGE);
        assertEquals(new DocumentReference(WIKI, SPACE, PAGE),
            this.resolver.resolve(withBaseReference, null));

        assertEquals(new DocumentReference(WIKI, SPACE, PAGE), this.resolver
            .resolve(documentResource("", true), null, new DocumentReference(WIKI, SPACE, PAGE)));
    }

    @Test
    void resolveUntypeDocument()
    {
        // When the page does not exist

        assertEquals(new DocumentReference(WIKI, List.of(SPACE, PAGE), DEFAULT_PAGE),
            this.resolver.resolve(documentResource(WIKI + ':' + SPACE + '.' + PAGE, false), null));
        assertEquals(new DocumentReference(CURRENT_WIKI, List.of(SPACE, PAGE), DEFAULT_PAGE),
            this.resolver.resolve(documentResource(SPACE + '.' + PAGE, false), null));
        assertEquals(new DocumentReference(CURRENT_WIKI, List.of(CURRENT_SPACE, PAGE), DEFAULT_PAGE),
            this.resolver.resolve(documentResource(PAGE, false), null));

        // Already ends with default page name

        assertEquals(new DocumentReference(WIKI, SPACE, DEFAULT_PAGE), this.resolver
            .resolve(documentResource(WIKI + ':' + SPACE + '.' + DEFAULT_PAGE, false), null));
        assertEquals(new DocumentReference(CURRENT_WIKI, SPACE, DEFAULT_PAGE),
            this.resolver.resolve(documentResource(SPACE + '.' + DEFAULT_PAGE, false), null));
        assertEquals(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, DEFAULT_PAGE),
            this.resolver.resolve(documentResource(DEFAULT_PAGE, false), null));

        // When the page exist

        this.existingDocuments.add(new DocumentReference(WIKI, SPACE, PAGE));
        assertEquals(new DocumentReference(WIKI, SPACE, PAGE), this.resolver
            .resolve(documentResource(WIKI + ':' + SPACE + '.' + PAGE, false), null));
        this.existingDocuments.add(new DocumentReference(CURRENT_WIKI, SPACE, PAGE));
        assertEquals(new DocumentReference(CURRENT_WIKI, SPACE, PAGE),
            this.resolver.resolve(documentResource(SPACE + '.' + PAGE, false), null));
        this.existingDocuments.add(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, PAGE));
        assertEquals(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, PAGE),
            this.resolver.resolve(documentResource(PAGE, false), null));

        // When the reference cannot be parsed by the relative resolver

        assertEquals(
            new DocumentReference(CURRENT_WIKI,
                List.of(CURRENT_SPACE, CURRENT_SPACE, CURRENT_SPACE, CURRENT_PAGE), DEFAULT_PAGE),
            this.resolver.resolve(documentResource("...", false), null));

        // When the page is current page

        assertEquals(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, CURRENT_PAGE),
            this.resolver.resolve(documentResource(CURRENT_PAGE, false), null));

        assertEquals(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, CURRENT_PAGE),
            this.resolver.resolve(documentResource("", false), null));
    }

    @Test
    void resolveUntypeDocumentWhenCurrentPageIsSpace()
    {
        // Current is top level space

        when(this.currentDocumentProvider.get())
            .thenReturn(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, DEFAULT_PAGE));

        assertEquals(new DocumentReference(CURRENT_WIKI, List.of(CURRENT_SPACE, PAGE), DEFAULT_PAGE),
            this.resolver.resolve(documentResource(PAGE, false), null));

        assertEquals(new DocumentReference(CURRENT_WIKI, List.of(SPACE, PAGE), DEFAULT_PAGE),
            this.resolver.resolve(documentResource(SPACE + '.' + PAGE, false), null));

        assertEquals(new DocumentReference(CURRENT_WIKI, List.of(CURRENT_SPACE, PAGE), DEFAULT_PAGE),
            this.resolver.resolve(documentResource('.' + PAGE, false), null));

        assertEquals(new DocumentReference(CURRENT_WIKI, List.of(CURRENT_SPACE, PAGE), DEFAULT_PAGE),
            this.resolver.resolve(documentResource('.' + PAGE + '.' + DEFAULT_PAGE, false), null));

        // Current is subspace

        // When sibling page does not exist

        when(this.currentDocumentProvider.get()).thenReturn(
            new DocumentReference(CURRENT_WIKI, List.of(CURRENT_SPACE, CURRENT_SUBSPACE), DEFAULT_PAGE));

        assertEquals(new DocumentReference(CURRENT_WIKI, List.of(CURRENT_SPACE, CURRENT_SUBSPACE, PAGE),
                DEFAULT_PAGE),
            this.resolver.resolve(documentResource(PAGE, false), null));

        assertEquals(new DocumentReference(CURRENT_WIKI, List.of(SPACE, PAGE), DEFAULT_PAGE),
            this.resolver.resolve(documentResource(SPACE + '.' + PAGE, false), null));

        assertEquals(new DocumentReference(CURRENT_WIKI, List.of(CURRENT_SPACE, CURRENT_SUBSPACE, PAGE),
                DEFAULT_PAGE),
            this.resolver.resolve(documentResource('.' + PAGE, false), null));

        assertEquals(
            new DocumentReference(CURRENT_WIKI, List.of(CURRENT_SPACE, CURRENT_SUBSPACE, PAGE), DEFAULT_PAGE),
            this.resolver.resolve(documentResource('.' + PAGE + '.' + DEFAULT_PAGE, false),
                null));

        // When sibling page exist

        this.existingDocuments.add(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, PAGE));

        assertEquals(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, PAGE),
            this.resolver.resolve(documentResource(PAGE, false), null));

        assertEquals(new DocumentReference(CURRENT_WIKI, List.of(SPACE, PAGE), DEFAULT_PAGE),
            this.resolver.resolve(documentResource(SPACE + '.' + PAGE, false), null));

        // FIXME: This should always be resolved to a child (terminal or non-terminal) page and never to a sibling.
        assertEquals(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, PAGE),
            this.resolver.resolve(documentResource('.' + PAGE, false), null));

        assertEquals(
            new DocumentReference(CURRENT_WIKI, List.of(CURRENT_SPACE, CURRENT_SUBSPACE, PAGE), DEFAULT_PAGE),
            this.resolver.resolve(documentResource('.' + PAGE + '.' + DEFAULT_PAGE, false),
                null));
    }

    @Test
    void resolveTypeSpace()
    {
        assertEquals(new SpaceReference(WIKI, SPACE),
            this.resolver.resolve(spaceResource(WIKI + ':' + SPACE, true), null));

        assertEquals(new SpaceReference(CURRENT_WIKI, SPACE),
            this.resolver.resolve(spaceResource(SPACE, true), null));

        assertEquals(new SpaceReference(CURRENT_WIKI, CURRENT_SPACE),
            this.resolver.resolve(spaceResource("", true), null));

        when(this.currentEntityReferenceResolver.resolve(eq(WIKI + ':' + SPACE + '.' + PAGE), eq(EntityType.DOCUMENT),
            any())).thenReturn(new DocumentReference(WIKI, SPACE, PAGE));

        ResourceReference withBaseReference = spaceResource("", true);
        withBaseReference.addBaseReference(WIKI + ':' + SPACE + '.' + PAGE);
        assertEquals(new SpaceReference(WIKI, SPACE),
            this.resolver.resolve(withBaseReference, null));

        assertEquals(new SpaceReference(WIKI, SPACE), this.resolver
            .resolve(spaceResource("", true), null, new DocumentReference(WIKI, SPACE, PAGE)));
    }

    @Test
    void resolveTypeAttachment()
    {
        // When the page does not exist

        assertEquals(
            new AttachmentReference(ATTACHMENT, new DocumentReference(WIKI, List.of(SPACE, PAGE), DEFAULT_PAGE)),
            this.resolver
                .resolve(attachmentResource(WIKI + ':' + SPACE + '.' + PAGE + '@' + ATTACHMENT, true), null));

        assertEquals(
            new AttachmentReference(ATTACHMENT,
                new DocumentReference(CURRENT_WIKI, List.of(SPACE, PAGE), DEFAULT_PAGE)),
            this.resolver.resolve(attachmentResource(SPACE + '.' + PAGE + '@' + ATTACHMENT, true),
                null));

        assertEquals(
            new AttachmentReference(ATTACHMENT,
                new DocumentReference(CURRENT_WIKI, List.of(CURRENT_SPACE, PAGE), DEFAULT_PAGE)),
            this.resolver.resolve(attachmentResource(PAGE + '@' + ATTACHMENT, true), null));

        // When the page exist

        this.existingDocuments.add(new DocumentReference(WIKI, SPACE, PAGE));
        assertEquals(new AttachmentReference(ATTACHMENT, new DocumentReference(WIKI, SPACE, PAGE)),
            this.resolver
                .resolve(attachmentResource(WIKI + ':' + SPACE + '.' + PAGE + '@' + ATTACHMENT, true), null));

        this.existingDocuments.add(new DocumentReference(CURRENT_WIKI, SPACE, PAGE));
        assertEquals(new AttachmentReference(ATTACHMENT, new DocumentReference(CURRENT_WIKI, SPACE, PAGE)),
            this.resolver.resolve(attachmentResource(SPACE + '.' + PAGE + '@' + ATTACHMENT, true), null));

        this.existingDocuments.add(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, PAGE));
        assertEquals(new AttachmentReference(ATTACHMENT, new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, PAGE)),
            this.resolver.resolve(attachmentResource(PAGE + '@' + ATTACHMENT, true), null));

        // When page is current page

        assertEquals(
            new AttachmentReference(ATTACHMENT, new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, CURRENT_PAGE)),
            this.resolver.resolve(attachmentResource(ATTACHMENT, true), null));

        this.existingDocuments.add(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, CURRENT_PAGE));
        assertEquals(
            new AttachmentReference(ATTACHMENT, new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, CURRENT_PAGE)),
            this.resolver.resolve(attachmentResource(ATTACHMENT, true), null));
    }

    class VoidResourceReferenceEntityReferenceResolve extends AbstractResourceReferenceEntityReferenceResolver
    {
        VoidResourceReferenceEntityReferenceResolve()
        {
            this.documentAccessBridge = DefaultResourceReferenceEntityReferenceResolverTest.this.bridge;
        }

        @Override
        protected EntityReference resolveTyped(ResourceReference resourceReference, EntityReference baseReference)
        {
            return null;
        }
    }

    @Test
    void trySpaceSiblingFallback()
    {
        VoidResourceReferenceEntityReferenceResolve resolver = new VoidResourceReferenceEntityReferenceResolve();

        String defaultDocumentName = "Foo";
        EntityReference sourceReference = new EntityReference("Bar", EntityType.DOCUMENT);
        DocumentReference finalReference = new DocumentReference("xwiki", "Bar",
            "WebHome");
        EntityReference baseReference = sourceReference;

        assertFalse(resolver.trySpaceSiblingFallback(sourceReference, finalReference, null,
            defaultDocumentName));
        assertFalse(resolver.trySpaceSiblingFallback(sourceReference, finalReference, baseReference,
            defaultDocumentName));

        defaultDocumentName = "Bar";
        assertTrue(resolver.trySpaceSiblingFallback(sourceReference, finalReference, baseReference,
            defaultDocumentName));

        this.existingDocuments.add(finalReference);
        assertFalse(resolver.trySpaceSiblingFallback(sourceReference, finalReference, baseReference,
            defaultDocumentName));
    }

    @Test
    void resolveTypedPage()
    {
        assertEquals(new PageReference(WIKI, SPACE, PAGE),
            this.resolver.resolve(pageResource(WIKI + ':' + SPACE + '/' + PAGE, true), null));

        assertEquals(new PageReference(WIKI, SPACE),
            this.resolver.resolve(pageResource(WIKI + ':' + SPACE, true), null));

        assertEquals(new PageReference(CURRENT_WIKI, SPACE),
            this.resolver.resolve(pageResource(SPACE, true), null));

        assertEquals(new PageReference(CURRENT_WIKI, CURRENT_SPACE, CURRENT_PAGE),
            this.resolver.resolve(pageResource("", true), null));

        assertEquals(new PageReference(WIKI, SPACE, PAGE),
            this.resolver.resolve(pageResource("", true), null, new DocumentReference(WIKI, SPACE, PAGE)));

        assertEquals(new DocumentReference(WIKI, List.of(SPACE, PAGE), DEFAULT_PAGE),
            this.resolver.resolve(pageResource(WIKI + ':' + SPACE + '/' + PAGE, true), EntityType.DOCUMENT));

        // FIXME: See https://jira.xwiki.org/browse/XWIKI-22699
        //this.existingDocuments.add(new DocumentReference(WIKI, SPACE, PAGE));
        //assertEquals(new DocumentReference(WIKI, SPACE, PAGE),
        //    this.resolver.resolve(pageResource(WIKI + ':' + SPACE + '/' + PAGE, true), EntityType.DOCUMENT));
    }
}
