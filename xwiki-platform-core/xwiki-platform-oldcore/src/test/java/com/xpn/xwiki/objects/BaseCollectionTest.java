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

import java.util.List;

import org.dom4j.Element;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link BaseCollection} class.
 *
 * @version $Id$
 */
@ReferenceComponentList
@OldcoreTest
class BaseCollectionTest
{
    private static final String FIELD = "myField";

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @Test
    void getXClassWithNullReference()
    {
        BaseCollection collection = new BaseCollection()
        {
            @Override
            public Element toXML(BaseClass bclass)
            {
                return null;
            }
        };

        assertNull(collection.getXClass(new XWikiContext()));
    }

    @Test
    void getDiffWhenNewPropertyIsNull()
    {
        PropertyClass propertyClass = mock(PropertyClass.class);
        when(propertyClass.getClassType()).thenReturn("StringClass");
        BaseClass xclass = mock(BaseClass.class);
        when(xclass.getField(FIELD)).thenReturn(propertyClass);

        BaseProperty oldProperty = mock(BaseProperty.class);
        when(oldProperty.getValue()).thenReturn("oldValue");
        when(oldProperty.toText()).thenReturn("oldValue");

        // addField() stores the property as given, so a null one leaves the field name in the collection
        // without a value.
        BaseCollection<EntityReference> newCollection = createCollection(xclass);
        newCollection.addField(FIELD, null);
        BaseCollection<EntityReference> oldCollection = createCollection(xclass);
        oldCollection.addField(FIELD, oldProperty);

        ObjectDiff diff = getChangedPropertyDiff(newCollection.getDiff(oldCollection, this.oldcore.getXWikiContext()));

        assertEquals(FIELD, diff.getPropName());
        assertEquals("StringClass", diff.getPropType());
        assertEquals("oldValue", diff.getPrevValue());
        assertEquals("", diff.getNewValue());
    }

    @Test
    void getDiffWhenNewPropertyIsNullAndNoPropertyClass()
    {
        BaseProperty oldProperty = mock(BaseProperty.class);
        when(oldProperty.toText()).thenReturn("oldValue");

        // Without a property class the diff falls back on the plain values, where a missing property is empty too.
        BaseCollection<EntityReference> newCollection = createCollection(null);
        newCollection.addField(FIELD, null);
        BaseCollection<EntityReference> oldCollection = createCollection(null);
        oldCollection.addField(FIELD, oldProperty);

        ObjectDiff diff = getChangedPropertyDiff(newCollection.getDiff(oldCollection, this.oldcore.getXWikiContext()));

        assertEquals(FIELD, diff.getPropName());
        assertEquals("", diff.getPropType());
        assertEquals("oldValue", diff.getPrevValue());
        assertEquals("", diff.getNewValue());
    }

    /**
     * @param xclass the class to return from {@link BaseCollection#getXClass(XWikiContext)}, so that the test needs
     *            no wiki to resolve it
     * @return a collection using {@link BaseCollection}'s own {@code getDiff()}, which both {@link BaseObject} and
     *         {@link BaseClass} override
     */
    private BaseCollection<EntityReference> createCollection(BaseClass xclass)
    {
        return new BaseCollection<>()
        {
            @Override
            public Element toXML(BaseClass bclass)
            {
                return null;
            }

            @Override
            public BaseClass getXClass(XWikiContext context)
            {
                return xclass;
            }
        };
    }

    private ObjectDiff getChangedPropertyDiff(List<ObjectDiff> diffs)
    {
        return diffs.stream().filter(diff -> ObjectDiff.ACTION_PROPERTYCHANGED.equals(diff.getAction())).findFirst()
            .orElseThrow();
    }
}
