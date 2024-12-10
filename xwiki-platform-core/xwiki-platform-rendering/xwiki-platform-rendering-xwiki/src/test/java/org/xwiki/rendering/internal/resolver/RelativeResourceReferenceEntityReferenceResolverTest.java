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

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.internal.reference.DefaultReferenceAttachmentReferenceResolver;
import org.xwiki.model.internal.reference.DefaultReferenceDocumentReferenceResolver;
import org.xwiki.model.internal.reference.DefaultReferenceEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringAttachmentReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringDocumentReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringSpaceReferenceResolver;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.PageAttachmentReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.PageAttachmentResourceReference;
import org.xwiki.rendering.listener.reference.PageResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.listener.reference.SpaceResourceReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Validate {@link RelativeResourceReferenceEntityReferenceResolver}.
 *
 * @version $Id$
 * @since 17.0.0RC1
 */
// @formatter:off
@ComponentList({
    RelativeResourceReferenceEntityReferenceResolver.class,
    RelativeAttachmentResourceReferenceEntityReferenceResolver.class,
    RelativeDocumentResourceReferenceEntityReferenceResolver.class,
    DefaultStringAttachmentReferenceResolver.class,
    DefaultStringDocumentReferenceResolver.class,
    RelativeSpaceResourceReferenceEntityReferenceResolver.class,
    RelativePageResourceReferenceEntityReferenceResolver.class,
    RelativePageAttachmentResourceReferenceEntityReferenceResolver.class,
    DefaultReferenceEntityReferenceResolver.class,
    DefaultStringEntityReferenceResolver.class,
    RelativeStringEntityReferenceResolver.class,
    DefaultReferenceAttachmentReferenceResolver.class,
    DefaultReferenceDocumentReferenceResolver.class,
    DefaultStringSpaceReferenceResolver.class,
    ContextComponentManagerProvider.class,
    DefaultSymbolScheme.class,
    DefaultEntityReferenceProvider.class,
    DefaultModelConfiguration.class
})
@ComponentTest
// @formatter:on
class RelativeResourceReferenceEntityReferenceResolverTest
{
    private static final String DEFAULT_PAGE = "WebHome";

    private static final String WIKI = "Wiki";

    private static final String SPACE = "Space";

    private static final String PAGE = "Page";

    private static final String ATTACHMENT = "file.ext";

    private static final Map<String, Serializable> PARENT_TYPE_PARAMETER_MAP =
        Map.of(EntityReference.PARENT_TYPE_PARAMETER, EntityType.SPACE);
    private static final SpaceReference BASE_REFERENCE = new SpaceReference(WIKI, SPACE);
    private static final EntityReference SPACE_ENTITY_REFERENCE_SPACE_PARENT_TYPE =
        new EntityReference(SPACE, EntityType.SPACE, PARENT_TYPE_PARAMETER_MAP);
    private static final EntityReference SPACE_ENTITY_REFERENCE = new EntityReference(SPACE, EntityType.SPACE);
    private static final EntityReference PAGE_ENTITY_REFERENCE =
        new EntityReference(PAGE, EntityType.DOCUMENT, SPACE_ENTITY_REFERENCE);
    private static final EntityReference PAGE_ALONE_ENTITY_REFERENCE = new EntityReference(PAGE, EntityType.DOCUMENT);
    private static final EntityReference DEFAULT_PAGE_SPACE_ENTITY_REFERENCE =
        new EntityReference(DEFAULT_PAGE, EntityType.DOCUMENT, SPACE_ENTITY_REFERENCE_SPACE_PARENT_TYPE);

    
    @InjectMockComponents
    private RelativeResourceReferenceEntityReferenceResolver resolver;

    @MockComponent
    @Named("current")
    private EntityReferenceResolver<String> currentEntityReferenceResolver;

    @MockComponent
    @Named("current")
    private Provider<DocumentReference> currentDocumentProvider;

    @MockComponent
    private DocumentAccessBridge bridge;

    private final Set<DocumentReference> existingDocuments = new HashSet<>();

    @BeforeEach
    void before() throws Exception
    {
        when(this.bridge.exists(any(DocumentReference.class))).then(
            (Answer<Boolean>) invocation -> existingDocuments.contains(invocation.getArguments()[0]));
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
        PageResourceReference reference = new PageResourceReference(referenceString);
        reference.setTyped(typed);
        return reference;
    }

    private PageAttachmentResourceReference pageAttachmentResource(String referenceString, boolean typed)
    {
        PageAttachmentResourceReference reference = new PageAttachmentResourceReference(referenceString);
        reference.setTyped(typed);
        return reference;
    }

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
        assertEquals(PAGE_ENTITY_REFERENCE, this.resolver.resolve(documentResource(SPACE + '.' + PAGE, true), null));

        assertEquals(new EntityReference(PAGE, EntityType.DOCUMENT),
            this.resolver.resolve(documentResource(PAGE, true), null));

        assertNull(this.resolver.resolve(documentResource("", true), null));

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

        assertEquals(new EntityReference(DEFAULT_PAGE, EntityType.DOCUMENT,
                new EntityReference(PAGE, EntityType.SPACE, SPACE_ENTITY_REFERENCE_SPACE_PARENT_TYPE)),
            this.resolver.resolve(documentResource(SPACE + '.' + PAGE, false), null));
        assertEquals(new EntityReference(DEFAULT_PAGE, EntityType.DOCUMENT,
                new EntityReference(PAGE, EntityType.SPACE, PARENT_TYPE_PARAMETER_MAP)),
            this.resolver.resolve(documentResource(PAGE, false), null));

        assertEquals(new DocumentReference(WIKI, List.of(SPACE, PAGE), DEFAULT_PAGE),
            this.resolver.resolve(documentResource(WIKI + ':' + SPACE + '.' + PAGE, false), null, BASE_REFERENCE));
        assertEquals(new DocumentReference(WIKI, List.of(SPACE, PAGE), DEFAULT_PAGE),
            this.resolver.resolve(documentResource(SPACE + '.' + PAGE, false), null, BASE_REFERENCE));
        assertEquals(new DocumentReference(WIKI, List.of(SPACE, PAGE), DEFAULT_PAGE),
            this.resolver.resolve(documentResource(PAGE, false), null, BASE_REFERENCE));

        // Already ends with default page name
        assertEquals(new DocumentReference(WIKI, SPACE, DEFAULT_PAGE), this.resolver
            .resolve(documentResource(WIKI + ':' + SPACE + '.' + DEFAULT_PAGE, false), null));

        assertEquals(DEFAULT_PAGE_SPACE_ENTITY_REFERENCE,
            this.resolver.resolve(documentResource(SPACE + '.' + DEFAULT_PAGE, false), null));
        assertEquals(new EntityReference(DEFAULT_PAGE, EntityType.DOCUMENT),
            this.resolver.resolve(documentResource(DEFAULT_PAGE, false), null));

        assertEquals(new DocumentReference(WIKI, SPACE, DEFAULT_PAGE), this.resolver
            .resolve(documentResource(WIKI + ':' + SPACE + '.' + DEFAULT_PAGE, false), null, BASE_REFERENCE));
        assertEquals(new DocumentReference(WIKI, SPACE, DEFAULT_PAGE),
            this.resolver.resolve(documentResource(SPACE + '.' + DEFAULT_PAGE, false), null, BASE_REFERENCE));
        assertEquals(new DocumentReference(WIKI, SPACE, DEFAULT_PAGE),
            this.resolver.resolve(documentResource(DEFAULT_PAGE, false), null, BASE_REFERENCE));


        // When the page exist

        this.existingDocuments.add(new DocumentReference(WIKI, SPACE, PAGE));
        assertEquals(new DocumentReference(WIKI, SPACE, PAGE), this.resolver
            .resolve(documentResource(WIKI + ':' + SPACE + '.' + PAGE, false), null));

        assertEquals(new DocumentReference(WIKI, SPACE, PAGE),
            this.resolver.resolve(documentResource(WIKI + ':' + SPACE + '.' + PAGE, false), null, BASE_REFERENCE));
        assertEquals(new DocumentReference(WIKI, SPACE, PAGE),
            this.resolver.resolve(documentResource(SPACE + '.' + PAGE, false), null, BASE_REFERENCE));
        assertEquals(new DocumentReference(WIKI, SPACE, PAGE),
            this.resolver.resolve(documentResource(PAGE, false), null, BASE_REFERENCE));
        
        assertNull(this.resolver.resolve(documentResource("", false), null));
        assertEquals(new DocumentReference("xwiki", List.of("Main", "Main", "Main"), DEFAULT_PAGE),
            this.resolver.resolve(documentResource("...", false), null));
    }

    @Test
    void resolveUntypeDocumentWhenCurrentPageIsSpace()
    {
        assertEquals(new EntityReference(DEFAULT_PAGE, EntityType.DOCUMENT,
                new EntityReference(PAGE, EntityType.SPACE, SPACE_ENTITY_REFERENCE_SPACE_PARENT_TYPE)),
            this.resolver.resolve(documentResource(SPACE + '.' + PAGE, false), null));
        assertEquals(new EntityReference(DEFAULT_PAGE, EntityType.DOCUMENT,
                new EntityReference(PAGE, EntityType.SPACE, PARENT_TYPE_PARAMETER_MAP)),
            this.resolver.resolve(documentResource(PAGE, false), null));

        assertEquals(new EntityReference(DEFAULT_PAGE, EntityType.DOCUMENT,
                new EntityReference(PAGE, EntityType.SPACE, PARENT_TYPE_PARAMETER_MAP)),
            this.resolver.resolve(documentResource('.' + PAGE, false), null));

        assertEquals(new EntityReference(DEFAULT_PAGE, EntityType.DOCUMENT,
                new EntityReference(PAGE, EntityType.SPACE, PARENT_TYPE_PARAMETER_MAP)),
            this.resolver.resolve(documentResource('.' + PAGE + '.' + DEFAULT_PAGE, false), null));
    }

    @Test
    void resolveTypeSpace()
    {
        assertEquals(new SpaceReference(WIKI, SPACE),
            this.resolver.resolve(spaceResource(WIKI + ':' + SPACE, true), null));

        assertEquals(SPACE_ENTITY_REFERENCE,
            this.resolver.resolve(spaceResource(SPACE, true), null));

        assertEquals(new SpaceReference(WIKI, SPACE),
            this.resolver.resolve(spaceResource(SPACE, true), null, new WikiReference(WIKI)));

        assertNull(this.resolver.resolve(spaceResource("", true), null));
        assertEquals(new SpaceReference(WIKI, SPACE), this.resolver
            .resolve(spaceResource("", true), null, new DocumentReference(WIKI, SPACE, PAGE)));

        ResourceReference withBaseReference = spaceResource("", true);
        withBaseReference.addBaseReference(WIKI + ':' + SPACE + '.' + PAGE);
        assertEquals(new SpaceReference(WIKI, SPACE),
            this.resolver.resolve(withBaseReference, null));
    }

    @Test
    void resolveTypeAttachment()
    {
        // When the page does not exist
        EntityReference attachmentInSpacePage =
            new EntityReference(ATTACHMENT, EntityType.ATTACHMENT, PAGE_ENTITY_REFERENCE);
        EntityReference attachmentInPage =
            new EntityReference(ATTACHMENT, EntityType.ATTACHMENT, PAGE_ALONE_ENTITY_REFERENCE);

        SpaceReference baseReference = new SpaceReference(WIKI, SPACE);

        assertEquals(
            new AttachmentReference(ATTACHMENT, new DocumentReference(WIKI, List.of(SPACE, PAGE), DEFAULT_PAGE)),
            this.resolver
                .resolve(attachmentResource(WIKI + ':' + SPACE + '.' + PAGE + '@' + ATTACHMENT, true), null));
        assertEquals(attachmentInSpacePage,
            this.resolver.resolve(attachmentResource(SPACE + '.' + PAGE + '@' + ATTACHMENT, true),
                null));
        assertEquals(attachmentInPage,
            this.resolver.resolve(attachmentResource(PAGE + '@' + ATTACHMENT, true), null));

        assertEquals(
            new AttachmentReference(ATTACHMENT, new DocumentReference(WIKI, List.of(SPACE, PAGE), DEFAULT_PAGE)),
            this.resolver
                .resolve(attachmentResource(WIKI + ':' + SPACE + '.' + PAGE + '@' + ATTACHMENT, true), null,
                    baseReference));
        assertEquals(
            new AttachmentReference(ATTACHMENT, new DocumentReference(WIKI, List.of(SPACE, PAGE), DEFAULT_PAGE)),
            this.resolver.resolve(attachmentResource(SPACE + '.' + PAGE + '@' + ATTACHMENT, true),
                null, baseReference));
        assertEquals(
            new AttachmentReference(ATTACHMENT, new DocumentReference(WIKI, List.of(SPACE, PAGE), DEFAULT_PAGE)),
            this.resolver.resolve(attachmentResource(PAGE + '@' + ATTACHMENT, true), null, baseReference));

        // When the page exist

        this.existingDocuments.add(new DocumentReference(WIKI, SPACE, PAGE));
        assertEquals(new AttachmentReference(ATTACHMENT, new DocumentReference(WIKI, SPACE, PAGE)),
            this.resolver
                .resolve(attachmentResource(WIKI + ':' + SPACE + '.' + PAGE + '@' + ATTACHMENT, true), null));
        assertEquals(attachmentInSpacePage,
            this.resolver.resolve(attachmentResource(SPACE + '.' + PAGE + '@' + ATTACHMENT, true), null));
        assertEquals(attachmentInPage,
            this.resolver.resolve(attachmentResource(PAGE + '@' + ATTACHMENT, true), null));

        assertEquals(new AttachmentReference(ATTACHMENT, new DocumentReference(WIKI, SPACE, PAGE)),
            this.resolver
                .resolve(attachmentResource(WIKI + ':' + SPACE + '.' + PAGE + '@' + ATTACHMENT, true), null,
                    baseReference));

        assertEquals(new AttachmentReference(ATTACHMENT, new DocumentReference(WIKI, SPACE, PAGE)),
            this.resolver.resolve(attachmentResource(SPACE + '.' + PAGE + '@' + ATTACHMENT, true), null,
                baseReference));

        assertEquals(new AttachmentReference(ATTACHMENT, new DocumentReference(WIKI, SPACE, PAGE)),
            this.resolver.resolve(attachmentResource(PAGE + '@' + ATTACHMENT, true), null, baseReference));

        assertEquals(
            new EntityReference(ATTACHMENT, EntityType.ATTACHMENT),
            this.resolver.resolve(attachmentResource(ATTACHMENT, true), null));
        assertEquals(
            new AttachmentReference(ATTACHMENT, new DocumentReference(WIKI, SPACE, DEFAULT_PAGE)),
            this.resolver.resolve(attachmentResource(ATTACHMENT, true), null, baseReference));
    }

    @Test
    void resolveTypePage()
    {
        assertEquals(new EntityReference("page2", EntityType.PAGE, new EntityReference(SPACE, EntityType.PAGE)),
            this.resolver.resolve(pageResource("Space/page2", false), null));

        assertEquals(new PageReference(WIKI, SPACE, "page2"),
            this.resolver.resolve(pageResource("Space/page2", false), null, new WikiReference(WIKI)));

        assertEquals(new EntityReference("page2", EntityType.PAGE, new EntityReference(SPACE, EntityType.PAGE)),
            this.resolver.resolve(pageResource("Space/page2", true), null));

        assertEquals(new PageReference(WIKI, SPACE, "page2"),
            this.resolver.resolve(pageResource("Space/page2", true), null, new WikiReference(WIKI)));

        PageResourceReference pageResourceReference = pageResource("Space/page2", false);
        pageResourceReference.addBaseReference("Wiki:space1.doc1");

        assertEquals(new PageReference(WIKI, SPACE, "page2"),
            this.resolver.resolve(pageResourceReference, null));
    }

    @Test
    void resolveTypePageAttachment()
    {
        EntityReference relativeEntityReference =
            new EntityReference("page2", EntityType.PAGE, new EntityReference(SPACE, EntityType.PAGE));
        assertEquals(new EntityReference("file.ext", EntityType.PAGE_ATTACHMENT, relativeEntityReference),
            this.resolver.resolve(pageAttachmentResource("Space/page2/file.ext", false), null));

        PageReference pageReference = new PageReference(WIKI, SPACE, "page2");
        assertEquals(new PageAttachmentReference("file.ext", pageReference),
            this.resolver.resolve(pageAttachmentResource("Space/page2/file.ext", false), null,
                new WikiReference(WIKI)));

        assertEquals(new EntityReference("file.ext", EntityType.PAGE_ATTACHMENT, relativeEntityReference),
            this.resolver.resolve(pageAttachmentResource("Space/page2/file.ext", true), null));

        assertEquals(new PageAttachmentReference("file.ext", pageReference),
            this.resolver.resolve(pageAttachmentResource("Space/page2/file.ext", true), null, new WikiReference(WIKI)));

        PageAttachmentResourceReference pageAttachmentResourceReference =
            pageAttachmentResource("Space/page2/file.ext", false);
        pageAttachmentResourceReference.addBaseReference("Wiki:space1.doc1");

        assertEquals(new PageAttachmentReference("file.ext", pageReference),
            this.resolver.resolve(pageAttachmentResourceReference, null));
    }
}
