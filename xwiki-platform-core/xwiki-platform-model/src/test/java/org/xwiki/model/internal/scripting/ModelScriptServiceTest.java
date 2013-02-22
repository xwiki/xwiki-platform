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
package org.xwiki.model.internal.scripting;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.model.internal.scripting.ModelScriptService}.
 *
 * @version $Id$
 * @since 2.3M1
 */
public class ModelScriptServiceTest
{
    private ModelScriptService service;

    private ComponentManager componentManager;

    private DocumentReferenceResolver<EntityReference> resolver;

    private EntityReferenceValueProvider valueProvider;

    private Logger logger;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp()
    {
        this.service = new ModelScriptService();
        this.componentManager = mock(ComponentManager.class);
        ReflectionUtils.setFieldValue(this.service, "componentManager", this.componentManager);
        this.logger = mock(Logger.class);
        ReflectionUtils.setFieldValue(this.service, "logger", this.logger);
        this.resolver = mock(DocumentReferenceResolver.class);
        this.valueProvider = mock(EntityReferenceValueProvider.class);
    }

    @Test
    public void createDocumentReferenceWithSpecifiedHint() throws Exception
    {
        when(this.componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "default"))
            .thenReturn(this.resolver);
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        when(this.resolver.resolve(reference)).thenReturn(reference);

        Assert.assertEquals(reference, this.service.createDocumentReference("wiki", "space", "page", "default"));
    }

    @Test
    public void createDocumentReferenceWithDefaultHint() throws Exception
    {
        when(this.componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current"))
            .thenReturn(this.resolver);
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        when(this.resolver.resolve(reference)).thenReturn(reference);

        Assert.assertEquals(reference, this.service.createDocumentReference("wiki", "space", "page"));
    }

    @Test
    public void createDocumentReferenceWhenEmptyParameters() throws Exception
    {
        when(this.componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "default"))
            .thenReturn(this.resolver);
        DocumentReference reference = new DocumentReference("defaultwiki", "defaultspace", "defaultpage");
        when(this.resolver.resolve(null)).thenReturn(reference);

        Assert.assertEquals(reference, this.service.createDocumentReference("", "", "", "default"));
    }

    @Test
    public void createDocumentReferenceWhenWikiParameterEmpty() throws Exception
    {
        when(this.componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "default"))
            .thenReturn(this.resolver);
        DocumentReference reference = new DocumentReference("defaultwiki", "space", "page");
        when(this.resolver.resolve(new EntityReference("page", EntityType.DOCUMENT,
            new EntityReference("space", EntityType.SPACE)))).thenReturn(reference);

        Assert.assertEquals(reference, this.service.createDocumentReference("", "space", "page", "default"));
    }

    @Test
    public void createDocumentReferenceWhenSpaceParameterEmpty() throws Exception
    {
        when(this.componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "default"))
            .thenReturn(this.resolver);
        DocumentReference reference = new DocumentReference("wiki", "defaultspace", "page");
        when(this.resolver.resolve(new EntityReference("page", EntityType.DOCUMENT,
            new EntityReference("wiki", EntityType.WIKI)))).thenReturn(reference);

        Assert.assertEquals(reference, this.service.createDocumentReference("wiki", "", "page", "default"));
    }

    @Test
    public void createDocumentReferenceWhenPageParameterEmpty() throws Exception
    {
        when(this.componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "default"))
            .thenReturn(this.resolver);
        DocumentReference reference = new DocumentReference("wiki", "space", "defaultpage");
        when(this.resolver.resolve(new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)))).thenReturn(reference);

        Assert.assertEquals(reference, this.service.createDocumentReference("wiki", "space", "", "default"));
    }

    @Test
    public void createDocumentReferenceWhenWikiAndSpaceParametersEmpty() throws Exception
    {
        when(this.componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "default"))
            .thenReturn(this.resolver);
        DocumentReference reference = new DocumentReference("wiki", "defaultspace", "defaultpage");
        when(this.resolver.resolve(new EntityReference("wiki", EntityType.WIKI))).thenReturn(reference);

        Assert.assertEquals(reference, this.service.createDocumentReference("wiki", "", "", "default"));
    }

    @Test
    public void createDocumentReferenceWhenInvalidHint() throws Exception
    {
        when(this.componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "invalid"))
            .thenThrow(new ComponentLookupException("error"));
        // Make sure backward compatibility is preserved.
        when(this.componentManager.getInstance(DocumentReferenceResolver.class, "invalid"))
            .thenThrow(new ComponentLookupException("error"));

        Assert.assertNull(this.service.createDocumentReference("wiki", "space", "page", "invalid"));
    }

    @Test
    public void createDocumentReferenceWithDeprecatedHint() throws Exception
    {
        when(this.componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current/reference"))
            .thenThrow(new ComponentLookupException("error"));
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        // Make sure backward compatibility is preserved.
        when(this.componentManager.getInstance(DocumentReferenceResolver.class, "current/reference"))
            .thenReturn(this.resolver);
        when(this.resolver.resolve(reference)).thenReturn(reference);

        Assert.assertEquals(reference,
            this.service.createDocumentReference("wiki", "space", "page", "current/reference"));

        // Verify that we log a warning!
        verify(this.logger).warn("Deprecated usage of DocumentReferenceResolver with hint [{}]. "
            + "Please consider using a DocumentReferenceResolver that takes into account generic types.",
            "current/reference");
    }

    @Test
    public void getEntityReferenceValue() throws Exception
    {
        when(this.componentManager.getInstance(EntityReferenceValueProvider.class, "current"))
            .thenReturn(this.valueProvider);
        when(this.valueProvider.getDefaultValue(EntityType.WIKI)).thenReturn("somewiki");

        Assert.assertEquals("somewiki", this.service.getEntityReferenceValue(EntityType.WIKI));
    }

    @Test
    public void getEntityReferenceValueWithInvalidHint() throws Exception
    {
        when(this.componentManager.getInstance(EntityReferenceValueProvider.class, "invalid"))
            .thenThrow(new ComponentLookupException("error"));

        Assert.assertNull(this.service.getEntityReferenceValue(EntityType.WIKI, "invalid"));
    }

    @Test
    public void getEntityReferenceValueWithNullType() throws Exception
    {
        Assert.assertNull(this.service.getEntityReferenceValue(null));
    }

    @Test
    public void createWikiReference()
    {
        Assert.assertEquals(new WikiReference("wiki"), this.service.createWikiReference("wiki"));
    }

    @Test
    public void createSpaceReference()
    {
        Assert.assertEquals(new SpaceReference("space", new WikiReference("wiki")),
            this.service.createSpaceReference("space", this.service.createWikiReference("wiki")));
    }

    @Test
    public void createEntityReferenceWithoutParent()
    {
        Assert.assertEquals(new EntityReference("page", EntityType.DOCUMENT),
            this.service.createEntityReference("page", EntityType.DOCUMENT));
    }

    @Test
    public void createEntityReferenceWithParent()
    {
        Assert.assertEquals(new EntityReference("page", EntityType.DOCUMENT,
            new EntityReference("space", EntityType.SPACE)),
            this.service.createEntityReference("page", EntityType.DOCUMENT,
                this.service.createEntityReference("space", EntityType.SPACE)));
    }
}
