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
package org.xwiki.model.internal.reference;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link org.xwiki.model.internal.reference.ExplicitStringAttachmentReferenceResolver}.
 *
 * @version $Id$
 * @since 3.0M1
 */
@ComponentList({
    DefaultSymbolScheme.class
})
public class ExplicitStringAttachmentReferenceResolverTest
{
    @Rule
    public MockitoComponentMockingRule<ExplicitStringEntityReferenceResolver> mocker =
        new MockitoComponentMockingRule<>(ExplicitStringEntityReferenceResolver.class);

    private AttachmentReferenceResolver<String> resolver;

    @Before
    public void setUp() throws Exception
    {
        this.resolver = new ExplicitStringAttachmentReferenceResolver();
        ReflectionUtils.setFieldValue(this.resolver, "entityReferenceResolver", this.mocker.getComponentUnderTest());
    }

    @Test
    public void resolveWithExplicitAttachmentReference()
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        AttachmentReference reference = this.resolver.resolve("", new AttachmentReference("file", documentReference));

        Assert.assertEquals("file", reference.getName());
        Assert.assertEquals(documentReference, reference.getDocumentReference());
    }
}
