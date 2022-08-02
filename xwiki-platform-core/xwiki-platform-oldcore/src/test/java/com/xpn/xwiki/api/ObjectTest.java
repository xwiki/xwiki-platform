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
package com.xpn.xwiki.api;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link Object}.
 * 
 * @version $Id$
 * @since 12.10.11
 * @since 13.4.6
 * @since 13.10RC1
 */
@OldcoreTest
class ObjectTest
{
    private Object object;

    @Mock
    private BaseObject baseObject;

    @Mock
    private XWikiContext xcontext;

    @MockComponent
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiSerializer;

    @BeforeEach
    void setUp()
    {
        DocumentReference classReference = new DocumentReference("test", "Some", "Class");
        DocumentReference documentReference = new DocumentReference("test", "Some", "Page");
        when(this.compactWikiSerializer.serialize(classReference, documentReference)).thenReturn("Some.Class");
        BaseObjectReference objectReference = new BaseObjectReference(classReference, 0, documentReference);
        when(this.baseObject.getReference()).thenReturn(objectReference);

        this.object = new Object(this.baseObject, this.xcontext);
    }

    @Test
    void getPropertyReference()
    {
        assertEquals(new ObjectPropertyReference("age", this.baseObject.getReference()),
            object.getPropertyReference("age"));
    }
}
