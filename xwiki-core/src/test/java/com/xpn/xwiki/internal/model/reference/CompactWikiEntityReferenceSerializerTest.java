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
package com.xpn.xwiki.internal.model.reference;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link com.xpn.xwiki.internal.model.reference.CompactWikiStringEntityReferenceSerializer}.
 *
 * @version $Id$
 * @since 2.2M1
 */
public class CompactWikiEntityReferenceSerializerTest extends AbstractComponentTestCase
{
    private EntityReferenceSerializer<EntityReference> serializer;

    private Mockery mockery = new Mockery();

    private ModelContext mockModelContext;

    @Override protected void registerComponents() throws Exception
    {
        super.registerComponents();

        this.mockModelContext = mockery.mock(ModelContext.class);
        DefaultComponentDescriptor<ModelContext> descriptor = new DefaultComponentDescriptor<ModelContext>();
        descriptor.setRole(ModelContext.class);
        getComponentManager().registerComponent(descriptor, this.mockModelContext);    

        this.serializer = getComponentManager().lookup(EntityReferenceSerializer.class, "compactwiki");
    }

    @org.junit.Test
    public void testSerializeWhenInSameWiki() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");

        this.mockery.checking(new Expectations() {{
            allowing(mockModelContext).getCurrentEntityReference(); will(returnValue(new WikiReference("wiki")));
        }});

        Assert.assertEquals("space.page", this.serializer.serialize(reference));
        Assert.assertEquals("space", this.serializer.serialize(reference.getParent()));
    }

    @org.junit.Test
    public void testSerializeWhenNotInSameWiki() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");

        this.mockery.checking(new Expectations() {{
            allowing(mockModelContext).getCurrentEntityReference(); will(returnValue(new WikiReference("otherwiki")));
        }});

        Assert.assertEquals("wiki:space.page", this.serializer.serialize(reference));
        Assert.assertEquals("wiki:space", this.serializer.serialize(reference.getParent()));
    }
}
