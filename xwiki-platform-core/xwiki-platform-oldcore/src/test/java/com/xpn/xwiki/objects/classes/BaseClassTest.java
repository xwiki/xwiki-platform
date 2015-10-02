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
package com.xpn.xwiki.objects.classes;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

/**
 * Unit tests for the {@link BaseClass} class.
 *
 * @version $Id$
 */
@ReferenceComponentList
public class BaseClassTest
{
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    @Test
    public void testSetDocumentReference() throws Exception
    {
        BaseClass baseClass = new BaseClass();

        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        baseClass.setDocumentReference(reference);

        Assert.assertEquals(reference, baseClass.getDocumentReference());
    }

    @Test
    public void testSetNameSetWiki() throws Exception
    {
        String database = this.oldcore.getXWikiContext().getWikiId();
        BaseClass baseClass = new BaseClass();

        baseClass.setName("space.page");

        Assert.assertEquals(database, baseClass.getDocumentReference().getWikiReference().getName());
        Assert.assertEquals("space", baseClass.getDocumentReference().getLastSpaceReference().getName());
        Assert.assertEquals("page", baseClass.getDocumentReference().getName());
    }

    @Test
    public void testSetNameAloneWithChangingContext() throws Exception
    {
        String database = this.oldcore.getXWikiContext().getWikiId();
        BaseClass baseClass = new BaseClass();

        baseClass.setName("space.page");

        try {
            this.oldcore.getXWikiContext().setWikiId("otherwiki");

            Assert.assertEquals(database, baseClass.getDocumentReference().getWikiReference().getName());
            Assert.assertEquals("space", baseClass.getDocumentReference().getLastSpaceReference().getName());
            Assert.assertEquals("page", baseClass.getDocumentReference().getName());

            baseClass.setName("otherspace.otherpage");
        } finally {
            this.oldcore.getXWikiContext().setWikiId(database);
        }

        Assert.assertEquals(database, baseClass.getDocumentReference().getWikiReference().getName());
        Assert.assertEquals("otherspace", baseClass.getDocumentReference().getLastSpaceReference().getName());
        Assert.assertEquals("otherpage", baseClass.getDocumentReference().getName());

        baseClass = new BaseClass();
        try {
            this.oldcore.getXWikiContext().setWikiId("otherwiki");
            baseClass.setName("space.page");
        } finally {
            this.oldcore.getXWikiContext().setWikiId(database);
        }

        Assert.assertEquals("otherwiki", baseClass.getDocumentReference().getWikiReference().getName());
        Assert.assertEquals("space", baseClass.getDocumentReference().getLastSpaceReference().getName());
        Assert.assertEquals("page", baseClass.getDocumentReference().getName());

        baseClass.setName("otherspace.otherpage");

        Assert.assertEquals("otherwiki", baseClass.getDocumentReference().getWikiReference().getName());
        Assert.assertEquals("otherspace", baseClass.getDocumentReference().getLastSpaceReference().getName());
        Assert.assertEquals("otherpage", baseClass.getDocumentReference().getName());
    }
}
