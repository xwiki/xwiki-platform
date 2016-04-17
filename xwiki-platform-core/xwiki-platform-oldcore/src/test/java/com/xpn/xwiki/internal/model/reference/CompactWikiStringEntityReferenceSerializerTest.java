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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.test.MockitoOldcoreRule;

/**
 * Unit tests for {@link CompactWikiStringEntityReferenceSerializer}.
 * 
 * @version $Id$
 * @since 2.2M1
 */
@ComponentList({
    DefaultSymbolScheme.class,
    CurrentEntityReferenceProvider.class,
    DefaultModelConfiguration.class
})
public class CompactWikiStringEntityReferenceSerializerTest
{
    public MockitoComponentMockingRule<EntityReferenceSerializer<String>> mocker =
        new MockitoComponentMockingRule<EntityReferenceSerializer<String>>(
            CompactWikiStringEntityReferenceSerializer.class);

    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule(this.mocker);

    @Test
    public void testSerializeWhenInSameWiki() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");

        this.oldcore.getXWikiContext().setWikiReference(reference.getWikiReference());

        Assert.assertEquals("space.page", this.mocker.getComponentUnderTest().serialize(reference));
        Assert.assertEquals("space", this.mocker.getComponentUnderTest().serialize(reference.getParent()));
    }

    @Test
    public void testSerializeWhenNotInSameWiki() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");

        this.oldcore.getXWikiContext().setWikiId("otherwiki");

        Assert.assertEquals("wiki:space.page", this.mocker.getComponentUnderTest().serialize(reference));
        Assert.assertEquals("wiki:space", this.mocker.getComponentUnderTest().serialize(reference.getParent()));
    }
}
