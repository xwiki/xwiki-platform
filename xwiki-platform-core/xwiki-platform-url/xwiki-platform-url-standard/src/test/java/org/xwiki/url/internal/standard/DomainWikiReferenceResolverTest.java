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
package org.xwiki.url.internal.standard;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DomainWikiReferenceResolver}.
 *
 * @version $Id$
 * @since 5.1M1
 */
public class DomainWikiReferenceResolverTest
{
    @Rule
    public MockitoComponentMockingRule<DomainWikiReferenceResolver> mocker =
        new MockitoComponentMockingRule(DomainWikiReferenceResolver.class);

    @Test
    public void resolveWhenDomainIsCorrect() throws Exception
    {
        assertEquals(new WikiReference("wiki"), this.mocker.getComponentUnderTest().resolve("wiki.acme.com"));
    }

    @Test
    public void resolveWhenDomainIsAnIPAndNotMainWiki() throws Exception
    {
        EntityReferenceValueProvider valueProvider = this.mocker.getInstance(EntityReferenceValueProvider.class);
        when(valueProvider.getDefaultValue(EntityType.WIKI)).thenReturn("xwiki");

        assertEquals(new WikiReference("xwiki"), this.mocker.getComponentUnderTest().resolve("192.168.0.1"));
    }

    @Test
    public void resolveWhenDomainIsLocalhostAndMainWiki() throws Exception
    {
        EntityReferenceValueProvider valueProvider = this.mocker.getInstance(EntityReferenceValueProvider.class);
        when(valueProvider.getDefaultValue(EntityType.WIKI)).thenReturn("xwiki");

        assertEquals(new WikiReference("xwiki"), this.mocker.getComponentUnderTest().resolve("localhost"));
    }
}
