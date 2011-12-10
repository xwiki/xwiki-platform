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

import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

/**
 * Unit tests for the {@link BaseClass} class.
 *
 * @version $Id$
 */
public class BaseClassTest extends AbstractBridgedComponentTestCase
{
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
        String database = getContext().getDatabase();
        BaseClass baseClass = new BaseClass();

        baseClass.setName("space.page");

        Assert.assertEquals(database, baseClass.getDocumentReference().getWikiReference().getName());
        Assert.assertEquals("space", baseClass.getDocumentReference().getLastSpaceReference().getName());
        Assert.assertEquals("page", baseClass.getDocumentReference().getName());
    }

    @Test
    public void testSetNameAloneWithChangingContext() throws Exception
    {
        String database = getContext().getDatabase();
        BaseClass baseClass = new BaseClass();

        baseClass.setName("space.page");

        try {
            getContext().setDatabase("otherwiki");

            Assert.assertEquals(database, baseClass.getDocumentReference().getWikiReference().getName());
            Assert.assertEquals("space", baseClass.getDocumentReference().getLastSpaceReference().getName());
            Assert.assertEquals("page", baseClass.getDocumentReference().getName());

            baseClass.setName("otherspace.otherpage");
        } finally {
            getContext().setDatabase(database);
        }

        Assert.assertEquals(database, baseClass.getDocumentReference().getWikiReference().getName());
        Assert.assertEquals("otherspace", baseClass.getDocumentReference().getLastSpaceReference().getName());
        Assert.assertEquals("otherpage", baseClass.getDocumentReference().getName());

        baseClass = new BaseClass();
        try {
            getContext().setDatabase("otherwiki");
            baseClass.setName("space.page");
        } finally {
            getContext().setDatabase(database);
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
