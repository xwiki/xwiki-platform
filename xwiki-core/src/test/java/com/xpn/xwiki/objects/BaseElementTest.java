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
package com.xpn.xwiki.objects;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

/**
 * Unit tests for the {@link BaseElement} class.
 *
 * @version $Id$
 */
public class BaseElementTest extends AbstractBridgedComponentTestCase
{
    @Test
    public void testSetReferenceGetName() throws Exception
    {
        BaseElement baseElement = new BaseElement()
        {
        };

        baseElement.setDocumentReference(new DocumentReference("wiki", "space", "page"));
        Assert.assertEquals("space.page", baseElement.getName());
        Assert.assertEquals("wiki", baseElement.getDocumentReference().getWikiReference().getName());
        Assert.assertEquals("space", baseElement.getDocumentReference().getLastSpaceReference().getName());
        Assert.assertEquals("page", baseElement.getDocumentReference().getName());

        baseElement.setDocumentReference(new DocumentReference("otherwiki", "otherspace", "otherpage"));
        Assert.assertEquals("otherspace.otherpage", baseElement.getName());
        Assert.assertEquals("otherwiki", baseElement.getDocumentReference().getWikiReference().getName());
        Assert.assertEquals("otherspace", baseElement.getDocumentReference().getLastSpaceReference().getName());
        Assert.assertEquals("otherpage", baseElement.getDocumentReference().getName());
    }
}
