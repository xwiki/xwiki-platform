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

import org.jmock.Expectations;
import org.jmock.Mockery;
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

/**
 * Unit tests for {@link org.xwiki.model.internal.scripting.ModelScriptService}.
 *
 * @version $Id$
 * @since 2.3M1
 */
public class ModelScriptServiceTest
{
    private ModelScriptService service;

    private ComponentManager mockComponentManager;

    private DocumentReferenceResolver<EntityReference> mockResolver;

    private EntityReferenceValueProvider mockValueProvider;

    private Logger mockLogger;

    private Mockery mockery = new Mockery();

    @SuppressWarnings("unchecked")
    @Before
    public void setUp()
    {
        this.service = new ModelScriptService();
        this.mockComponentManager = this.mockery.mock(ComponentManager.class);
        ReflectionUtils.setFieldValue(this.service, "componentManager", this.mockComponentManager);
        this.mockLogger = this.mockery.mock(Logger.class);
        ReflectionUtils.setFieldValue(this.service, "logger", this.mockLogger);
        this.mockResolver = this.mockery.mock(DocumentReferenceResolver.class);
        this.mockValueProvider = this.mockery.mock(EntityReferenceValueProvider.class);
    }

    @Test
    public void testCreateDocumentReference() throws Exception
    {
        this.mockery.checking(new Expectations()
        {
            {
                allowing(ModelScriptServiceTest.this.mockComponentManager).getInstance(
                    DocumentReferenceResolver.TYPE_REFERENCE, "default");
                will(returnValue(ModelScriptServiceTest.this.mockResolver));

                allowing(ModelScriptServiceTest.this.mockResolver).resolve(
                    new DocumentReference("wiki", "space", "page"));
            }
        });

        this.service.createDocumentReference("wiki", "space", "page", "default");
    }

    @Test
    public void testCreateDocumentReferenceWithDefaultHint() throws Exception
    {
        this.mockery.checking(new Expectations()
        {
            {
                allowing(ModelScriptServiceTest.this.mockComponentManager).getInstance(
                    DocumentReferenceResolver.TYPE_REFERENCE, "current");
                will(returnValue(ModelScriptServiceTest.this.mockResolver));

                allowing(ModelScriptServiceTest.this.mockResolver).resolve(
                    new DocumentReference("wiki", "space", "page"));
            }
        });

        this.service.createDocumentReference("wiki", "space", "page");
    }

    @Test
    public void testCreateDocumentReferenceWhenEmptyParameters() throws Exception
    {
        this.mockery.checking(new Expectations()
        {
            {
                allowing(ModelScriptServiceTest.this.mockComponentManager).getInstance(
                    DocumentReferenceResolver.TYPE_REFERENCE, "default");
                will(returnValue(ModelScriptServiceTest.this.mockResolver));

                allowing(ModelScriptServiceTest.this.mockResolver).resolve(null);
            }
        });

        this.service.createDocumentReference("", "", "", "default");
    }

    @Test
    public void testCreateDocumentReferenceWhenWikiParameterEmpty() throws Exception
    {
        this.mockery.checking(new Expectations()
        {
            {
                allowing(ModelScriptServiceTest.this.mockComponentManager).getInstance(
                    DocumentReferenceResolver.TYPE_REFERENCE, "default");
                will(returnValue(ModelScriptServiceTest.this.mockResolver));

                allowing(ModelScriptServiceTest.this.mockResolver).resolve(
                    new EntityReference("page", EntityType.DOCUMENT,
                        new EntityReference("space", EntityType.SPACE)));
            }
        });

        this.service.createDocumentReference("", "space", "page", "default");
    }

    @Test
    public void testCreateDocumentReferenceWhenSpaceParameterEmpty() throws Exception
    {
        this.mockery.checking(new Expectations()
        {
            {
                allowing(ModelScriptServiceTest.this.mockComponentManager).getInstance(
                    DocumentReferenceResolver.TYPE_REFERENCE, "default");
                will(returnValue(ModelScriptServiceTest.this.mockResolver));

                allowing(ModelScriptServiceTest.this.mockResolver).resolve(
                    new EntityReference("page", EntityType.DOCUMENT,
                        new EntityReference("wiki", EntityType.WIKI)));
            }
        });

        this.service.createDocumentReference("wiki", "", "page", "default");
    }

    @Test
    public void testCreateDocumentReferenceWhenPageParameterEmpty() throws Exception
    {
        this.mockery.checking(new Expectations()
        {
            {
                allowing(ModelScriptServiceTest.this.mockComponentManager).getInstance(
                    DocumentReferenceResolver.TYPE_REFERENCE, "default");
                will(returnValue(ModelScriptServiceTest.this.mockResolver));

                allowing(ModelScriptServiceTest.this.mockResolver).resolve(
                    new EntityReference("space", EntityType.SPACE,
                        new EntityReference("wiki", EntityType.WIKI)));
            }
        });

        this.service.createDocumentReference("wiki", "space", "", "default");
    }

    @Test
    public void testCreateDocumentReferenceWhenWikiAndSpaceParametersEmpty() throws Exception
    {
        this.mockery.checking(new Expectations()
        {
            {
                allowing(ModelScriptServiceTest.this.mockComponentManager).getInstance(
                    DocumentReferenceResolver.TYPE_REFERENCE, "default");
                will(returnValue(ModelScriptServiceTest.this.mockResolver));

                allowing(ModelScriptServiceTest.this.mockResolver)
                    .resolve(new EntityReference("wiki", EntityType.WIKI));
            }
        });

        this.service.createDocumentReference("wiki", "", "", "default");
    }

    @Test
    public void testCreateDocumentReferenceWhenInvalidHint() throws Exception
    {
        this.mockery.checking(new Expectations()
        {
            {
                allowing(ModelScriptServiceTest.this.mockComponentManager).getInstance(
                    DocumentReferenceResolver.TYPE_REFERENCE, "invalid");
                will(throwException(new ComponentLookupException("error")));
                // Make sure backward compatibility is preserved.
                allowing(ModelScriptServiceTest.this.mockComponentManager).getInstance(DocumentReferenceResolver.class,
                    "invalid");
                will(throwException(new ComponentLookupException("error")));
            }
        });

        Assert.assertNull(this.service.createDocumentReference("wiki", "space", "page", "invalid"));
    }

    @Test
    public void testCreateDocumentReferenceWithDeprecatedHint() throws Exception
    {
        final DocumentReference ref = new DocumentReference("wiki", "space", "page");
        this.mockery.checking(new Expectations()
        {
            {
                allowing(ModelScriptServiceTest.this.mockComponentManager).getInstance(
                    DocumentReferenceResolver.TYPE_REFERENCE, "current/reference");
                will(throwException(new ComponentLookupException("error")));
                // Make sure backward compatibility is preserved.
                allowing(ModelScriptServiceTest.this.mockComponentManager).getInstance(DocumentReferenceResolver.class,
                    "current/reference");
                will(returnValue(ModelScriptServiceTest.this.mockResolver));

                allowing(ModelScriptServiceTest.this.mockResolver).resolve(
                    new DocumentReference("wiki", "space", "page"));
                will(returnValue(ref));
                allowing(ModelScriptServiceTest.this.mockLogger).warn(with(any(String.class)), with(any(String.class)));
            }
        });

        Assert.assertEquals(ref, this.service.createDocumentReference("wiki", "space", "page", "current/reference"));
    }

    @Test
    public void testGetEntityReferenceValue() throws Exception
    {
        this.mockery.checking(new Expectations()
        {
            {
                allowing(ModelScriptServiceTest.this.mockComponentManager).getInstance(
                    EntityReferenceValueProvider.class, "current");
                will(returnValue(ModelScriptServiceTest.this.mockValueProvider));
                allowing(ModelScriptServiceTest.this.mockValueProvider).getDefaultValue(EntityType.WIKI);
                will(returnValue("somewiki"));
            }
        });
        Assert.assertEquals("somewiki", this.service.getEntityReferenceValue(EntityType.WIKI));
    }

    @Test
    public void testGetEntityReferenceValueWithInvalidHint() throws Exception
    {
        this.mockery.checking(new Expectations()
        {
            {
                allowing(ModelScriptServiceTest.this.mockComponentManager).getInstance(
                    EntityReferenceValueProvider.class, "invalid");
                will(throwException(new ComponentLookupException("error")));
            }
        });
        Assert.assertNull(this.service.getEntityReferenceValue(EntityType.WIKI, "invalid"));
    }

    @Test
    public void testGetEntityReferenceValueWithNullType() throws Exception
    {
        Assert.assertNull(this.service.getEntityReferenceValue(null));
    }
}
