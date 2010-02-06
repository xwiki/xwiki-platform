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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.web.Utils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;

/**
 * Unit tests for {@link CurrentMixedStringDocumentReferenceResolver}.
 *
 * @version $Id$
 * @since 2.2M1
 */
public class CurrentMixedStringDocumentReferenceResolverTest extends AbstractBridgedXWikiComponentTestCase
{
    private static final String CURRENT_SPACE = "currentspace";

    private Mockery mockery = new Mockery();
    
    private EntityReferenceResolver resolver;

    private XWikiContext context;

    private ModelContext mockModelContext;
    
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.context = new XWikiContext();

        Execution execution = getComponentManager().lookup(Execution.class);
        execution.getContext().setProperty("xwikicontext", this.context);
        Utils.setComponentManager(getComponentManager());

        this.mockModelContext = mockery.mock(ModelContext.class);
        DefaultComponentDescriptor<ModelContext> descriptor = new DefaultComponentDescriptor<ModelContext>();
        descriptor.setRole(ModelContext.class);
        getComponentManager().registerComponent(descriptor, this.mockModelContext);
        
        this.resolver = getComponentManager().lookup(EntityReferenceResolver.class, "currentmixed");
    }

    @org.junit.Test
    public void testResolveDocumentReferenceWhenContextDocument() throws Exception
    {
        this.context.setDoc(new XWikiDocument("not used", CURRENT_SPACE, "notused"));

        mockery.checking(new Expectations() {{
            allowing(mockModelContext).getCurrentEntityReference(); will(returnValue(new WikiReference("currentwiki")));
        }});

        EntityReference reference = resolver.resolve("", EntityType.DOCUMENT);
        Assert.assertEquals("currentwiki", reference.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(CURRENT_SPACE, reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("WebHome", reference.getName());
    }

    @org.junit.Test
    public void testResolveDocumentReferenceForDefaultWikiWhenNoContextDocument() throws Exception
    {
        mockery.checking(new Expectations() {{
            allowing(mockModelContext).getCurrentEntityReference(); will(returnValue(new WikiReference("currentwiki")));
        }});

        EntityReference reference = resolver.resolve("space.page", EntityType.DOCUMENT);

        // Make sure the resolved wiki is the current wiki and not the wiki from the current document (since that
        // doc isn't set).
        Assert.assertEquals("currentwiki", reference.extractReference(EntityType.WIKI).getName());
        
        Assert.assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("page", reference.getName());
    }
}
