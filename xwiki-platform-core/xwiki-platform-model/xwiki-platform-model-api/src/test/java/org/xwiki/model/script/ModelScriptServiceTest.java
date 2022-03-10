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
package org.xwiki.model.script;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.PageReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.model.script.ModelScriptService}.
 *
 * @version $Id$
 * @since 2.3M1
 */
public class ModelScriptServiceTest
{
    private ModelScriptService service;

    private ComponentManager componentManager;

    private EntityReferenceResolver<EntityReference> entityResolver;

    private DocumentReferenceResolver<EntityReference> documentResolver;

    private PageReferenceResolver<EntityReference> pageResolver;

    private EntityReferenceResolver<String> stringEntityReferenceResolver;

    private EntityReferenceValueProvider valueProvider;

    private Logger logger;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void beforeEach() throws ComponentLookupException
    {
        this.service = new ModelScriptService();

        this.componentManager = mock(ComponentManager.class);
        ReflectionUtils.setFieldValue(this.service, "componentManager", this.componentManager);

        this.logger = mock(Logger.class);
        ReflectionUtils.setFieldValue(this.service, "logger", this.logger);

        this.entityResolver = mock(EntityReferenceResolver.class);
        when(this.componentManager.getInstance(EntityReferenceResolver.TYPE_REFERENCE, "current"))
            .thenReturn(this.entityResolver);
        this.documentResolver = mock(DocumentReferenceResolver.class);
        when(this.componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current"))
            .thenReturn(this.documentResolver);
        this.pageResolver = mock(PageReferenceResolver.class);
        when(this.componentManager.getInstance(PageReferenceResolver.TYPE_REFERENCE, "current"))
            .thenReturn(this.pageResolver);
        this.stringEntityReferenceResolver = mock(EntityReferenceResolver.class);
        when(this.componentManager.getInstance(EntityReferenceResolver.TYPE_STRING, "current"))
            .thenReturn(this.stringEntityReferenceResolver);

        this.valueProvider = mock(EntityReferenceValueProvider.class);
    }

    @Test
    public void createDocumentReferenceWithSpecifiedHint() throws Exception
    {
        when(this.componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "default"))
            .thenReturn(this.documentResolver);
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        when(this.documentResolver.resolve(reference)).thenReturn(reference);

        assertEquals(reference, this.service.createDocumentReference("wiki", "space", "page", "default"));
    }

    @Test
    public void createDocumentReferenceWithDefaultHint() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        when(this.documentResolver.resolve(reference)).thenReturn(reference);

        assertEquals(reference, this.service.createDocumentReference("wiki", "space", "page"));
    }

    @Test
    public void createDocumentReferenceWhenEmptyParameters() throws Exception
    {
        when(this.componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "default"))
            .thenReturn(this.documentResolver);
        DocumentReference reference = new DocumentReference("defaultwiki", "defaultspace", "defaultpage");
        when(this.documentResolver.resolve(null)).thenReturn(reference);

        assertEquals(reference, this.service.createDocumentReference("", "", "", "default"));
    }

    @Test
    public void createPageReferenceWhenEmptyParameters() throws Exception
    {
        PageReference reference = new PageReference("defaultwiki", "defaultpage");
        when(this.entityResolver.resolve(null, EntityType.PAGE)).thenReturn(reference);

        assertEquals(reference, this.service.createPageReference(""));
    }

    @Test
    public void createDocumentReferenceWhenWikiParameterEmpty() throws Exception
    {
        when(this.componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "default"))
            .thenReturn(this.documentResolver);
        DocumentReference reference = new DocumentReference("defaultwiki", "space", "page");
        when(this.documentResolver
            .resolve(new EntityReference("page", EntityType.DOCUMENT, new EntityReference("space", EntityType.SPACE))))
                .thenReturn(reference);

        assertEquals(reference, this.service.createDocumentReference("", "space", "page", "default"));
    }

    @Test
    public void createDocumentReferenceWhenSpaceParameterEmpty() throws Exception
    {
        when(this.componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "default"))
            .thenReturn(this.documentResolver);
        DocumentReference reference = new DocumentReference("wiki", "defaultspace", "page");
        when(this.documentResolver
            .resolve(new EntityReference("page", EntityType.DOCUMENT, new EntityReference("wiki", EntityType.WIKI))))
                .thenReturn(reference);

        assertEquals(reference, this.service.createDocumentReference("wiki", "", "page", "default"));
        assertEquals(reference,
            this.service.createDocumentReference("wiki", Collections.<String>emptyList(), "page", "default"));
    }

    @Test
    public void createDocumentReferenceWhenPageParameterEmpty() throws Exception
    {
        when(this.componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "default"))
            .thenReturn(this.documentResolver);
        DocumentReference reference = new DocumentReference("wiki", "space", "defaultpage");
        when(this.documentResolver
            .resolve(new EntityReference("space", EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI))))
                .thenReturn(reference);

        assertEquals(reference, this.service.createDocumentReference("wiki", "space", "", "default"));
    }

    @Test
    public void createDocumentReferenceWhenWikiAndSpaceParametersEmpty() throws Exception
    {
        when(this.componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "default"))
            .thenReturn(this.documentResolver);
        DocumentReference reference = new DocumentReference("wiki", "defaultspace", "defaultpage");
        when(this.documentResolver.resolve(new EntityReference("wiki", EntityType.WIKI))).thenReturn(reference);

        assertEquals(reference, this.service.createDocumentReference("wiki", "", "", "default"));
        assertEquals(reference,
            this.service.createDocumentReference("wiki", Collections.<String>emptyList(), "", "default"));
    }

    @Test
    public void createDocumentReferenceWhenInvalidHint() throws Exception
    {
        when(this.componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "invalid"))
            .thenThrow(new ComponentLookupException("error"));
        // Make sure backward compatibility is preserved.
        when(this.componentManager.getInstance(DocumentReferenceResolver.class, "invalid"))
            .thenThrow(new ComponentLookupException("error"));

        assertNull(this.service.createDocumentReference("wiki", "space", "page", "invalid"));
    }

    @Test
    public void createDocumentReferenceWithDeprecatedHint() throws Exception
    {
        when(this.componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current/reference"))
            .thenThrow(new ComponentLookupException("error"));
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        // Make sure backward compatibility is preserved.
        when(this.componentManager.getInstance(DocumentReferenceResolver.class, "current/reference"))
            .thenReturn(this.documentResolver);
        when(this.documentResolver.resolve(reference)).thenReturn(reference);

        assertEquals(reference, this.service.createDocumentReference("wiki", "space", "page", "current/reference"));

        // Verify that we log a warning!
        verify(this.logger).warn(
            "Deprecated usage of DocumentReferenceResolver with hint [{}]. "
                + "Please consider using a DocumentReferenceResolver that takes into account generic types.",
            "current/reference");
    }

    @Test
    public void createDocumentReferenceFromPageNameAndSpaceReference()
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        assertEquals(documentReference, this.service.createDocumentReference(documentReference.getName(),
            documentReference.getLastSpaceReference()));
    }

    @Test
    public void getEntityReferenceValue() throws Exception
    {
        when(this.componentManager.getInstance(EntityReferenceValueProvider.class, "current"))
            .thenReturn(this.valueProvider);
        when(this.valueProvider.getDefaultValue(EntityType.WIKI)).thenReturn("somewiki");

        assertEquals("somewiki", this.service.getEntityReferenceValue(EntityType.WIKI));
    }

    @Test
    public void getEntityReferenceValueWithInvalidHint() throws Exception
    {
        when(this.componentManager.getInstance(EntityReferenceValueProvider.class, "invalid"))
            .thenThrow(new ComponentLookupException("error"));

        assertNull(this.service.getEntityReferenceValue(EntityType.WIKI, "invalid"));
    }

    @Test
    public void getEntityReferenceValueWithNullType() throws Exception
    {
        assertNull(this.service.getEntityReferenceValue(null));
    }

    @Test
    public void createWikiReference()
    {
        assertEquals(new WikiReference("wiki"), this.service.createWikiReference("wiki"));
    }

    @Test
    public void createSpaceReference()
    {
        assertEquals(new SpaceReference("space", new WikiReference("wiki")),
            this.service.createSpaceReference("space", this.service.createWikiReference("wiki")));

        SpaceReference spaceReference =
            new SpaceReference("C", new SpaceReference("B", new SpaceReference("A", new WikiReference("wiki"))));
        assertEquals(spaceReference,
            this.service.createSpaceReference(Arrays.asList("A", "B", "C"), this.service.createWikiReference("wiki")));

        assertEquals(spaceReference,
            this.service.createSpaceReference(spaceReference.getName(), (SpaceReference) spaceReference.getParent()));
    }

    @Test
    public void createEntityReferenceWithoutParent()
    {
        assertEquals(new EntityReference("page", EntityType.DOCUMENT),
            this.service.createEntityReference("page", EntityType.DOCUMENT));
    }

    @Test
    public void createEntityReferenceWithParent()
    {
        assertEquals(new EntityReference("page", EntityType.DOCUMENT, new EntityReference("space", EntityType.SPACE)),
            this.service.createEntityReference("page", EntityType.DOCUMENT,
                this.service.createEntityReference("space", EntityType.SPACE)));
    }

    @Test
    public void resolveSpace() throws Exception
    {
        SpaceReference reference = new SpaceReference("Space", new WikiReference("wiki"));
        when(this.stringEntityReferenceResolver.resolve("x", EntityType.SPACE, new Object[] {})).thenReturn(reference);

        assertEquals(reference, this.service.resolveSpace("x"));
    }

    @Test
    public void resolveSpaceWithHintAndParameters() throws Exception
    {
        when(this.componentManager.getInstance(EntityReferenceResolver.TYPE_STRING, "custom"))
            .thenReturn(this.stringEntityReferenceResolver);
        SpaceReference reference = new SpaceReference("Foo", new WikiReference("bar"));
        Object[] parameters = new Object[] { new DocumentReference("wiki", "Space", "Page"), "extra" };
        when(this.stringEntityReferenceResolver.resolve("reference", EntityType.SPACE, parameters))
            .thenReturn(reference);

        assertEquals(reference, this.service.resolveSpace("reference", "custom", parameters[0], parameters[1]));
    }

    @Test
    public void resolveClassPropertyWithHintAndParameters() throws Exception
    {
        when(this.componentManager.getInstance(EntityReferenceResolver.TYPE_STRING, "custom"))
            .thenReturn(this.stringEntityReferenceResolver);
        ClassPropertyReference reference =
            new ClassPropertyReference("property", new DocumentReference("wiki", "Space", "Class"));
        Object[] parameters = new Object[] { new DocumentReference("wiki", "Space", "Page"), "extra" };
        when(this.stringEntityReferenceResolver.resolve("Class^property", EntityType.CLASS_PROPERTY, parameters))
            .thenReturn(reference);

        assertEquals(reference,
            this.service.resolveClassProperty("Class^property", "custom", parameters[0], parameters[1]));
    }

    @Test
    void createObjectPropertyReference()
    {
        ObjectReference objectReference =
            new ObjectReference("objectName", new DocumentReference("test", "Some", "Page"));
        assertEquals(new ObjectPropertyReference("test", objectReference),
            this.service.createObjectPropertyReference("test", objectReference));
    }
}
