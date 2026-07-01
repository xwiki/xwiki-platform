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
package com.xpn.xwiki.objects;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the {@link BaseElement} class.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class BaseElementTest
{
    @Test
    void setReferenceGetName()
    {
        BaseElement baseElement = new BaseElement()
        {
        };

        baseElement.setDocumentReference(new DocumentReference("wiki", "space", "page"));
        assertEquals("space.page", baseElement.getName());
        assertEquals("wiki", baseElement.getDocumentReference().getWikiReference().getName());
        assertEquals("space", baseElement.getDocumentReference().getLastSpaceReference().getName());
        assertEquals("page", baseElement.getDocumentReference().getName());

        baseElement.setDocumentReference(new DocumentReference("otherwiki", "otherspace", "otherpage"));
        assertEquals("otherspace.otherpage", baseElement.getName());
        assertEquals("otherwiki", baseElement.getDocumentReference().getWikiReference().getName());
        assertEquals("otherspace", baseElement.getDocumentReference().getLastSpaceReference().getName());
        assertEquals("otherpage", baseElement.getDocumentReference().getName());
    }
}
