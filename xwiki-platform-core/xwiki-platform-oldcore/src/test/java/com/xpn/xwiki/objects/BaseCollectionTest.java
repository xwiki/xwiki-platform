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

import java.util.Map;
import java.util.HashMap;

import org.junit.Assert;

import org.dom4j.Element;
import org.junit.Test;

import com.xwiki.model.EntityType;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

/**
 * Unit tests for the {@link BaseCollection} class.
 *
 * @version $Id$
 */
public class BaseCollectionTest extends AbstractBridgedComponentTestCase
{

    private static class BaseCollectionTestSubclass
    {
        @Override
        public Element toXML(BaseClass bclass)
        {
            return null;
        }
    }

    @Test
    public void testGetXClassWithNullReference() throws Exception
    {
        BaseCollection collection = new BaseCollectionTestSubclass();

        Assert.assertNull(collection.getXClass(getContext()));
    }

    @Test
    public void testHashCode()
    {
        EntityReference reference = new EntityReference("test", EntityType.WIKI);
        Map map = new HashMap();

        BaseCollection collection1 = new BaseCollectionTestSubclass();
        BaseCollection collection2 = new BaseCollectionTestSubclass();

        collection1.setXClassReference(reference);
        collcetion2.setXClassReference(reference);

        collection1.setFields(map);
        collection2.setFields(map);

        Assert.assertEquals(collection1.hashCode(), collection2.hashCode());
    }
}
