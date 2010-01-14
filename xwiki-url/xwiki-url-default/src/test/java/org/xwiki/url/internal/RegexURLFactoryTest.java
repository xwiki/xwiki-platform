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
 *
 */
package org.xwiki.url.internal;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.AbstractComponentTestCase;
import org.xwiki.url.XWikiDocumentURL;
import org.xwiki.url.XWikiURLFactory;

/**
 * Unit tests for {@link RegexXWikiURLFactory}.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public class RegexURLFactoryTest extends AbstractComponentTestCase
{
    private XWikiURLFactory<String> factory;

    private Mockery mockery = new Mockery();

    private DocumentReferenceResolver<EntityReference> resolver;

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        this.resolver = this.mockery.mock(DocumentReferenceResolver.class);
        DefaultComponentDescriptor<DocumentReferenceResolver> cd = 
        	new DefaultComponentDescriptor<DocumentReferenceResolver>();
        cd.setRole(DocumentReferenceResolver.class);
        cd.setRoleHint("current/reference");
        getComponentManager().registerComponent(cd, this.resolver);

        this.factory = (XWikiURLFactory<String>) getComponentManager().lookup(XWikiURLFactory.class);
    }

    @Test
    public void testCreateURL() throws Exception
    {
        final DocumentReference expected = new DocumentReference("wiki", "Main", "WebHome");

        mockery.checking(new Expectations() {{
            oneOf(resolver).resolve(expected); will(returnValue(expected));
        }});

        XWikiDocumentURL url = (XWikiDocumentURL) this.factory.createURL(
            "http://wiki.domain.com:8080/xwiki/bin/view/Main/WebHome?language=fr");
        Assert.assertEquals("view", url.getAction());
        Assert.assertEquals("fr", url.getParameterValue("language"));
        Assert.assertEquals(expected, url.getDocumentReference());
    }

    @Test
    public void testCreateURLFromPath() throws Exception
    {
        final DocumentReference expected = new DocumentReference("wiki", "Main", "WebHome");

        mockery.checking(new Expectations() {{
            oneOf(resolver).resolve(new EntityReference("WebHome", EntityType.DOCUMENT,
                new EntityReference("Main", EntityType.SPACE))); will(returnValue(expected));
        }});

        XWikiDocumentURL url = (XWikiDocumentURL) this.factory.createURL("/xwiki/bin/view/Main/WebHome");
        Assert.assertEquals("view", url.getAction());
        Assert.assertEquals(expected, url.getDocumentReference());
    }
}
