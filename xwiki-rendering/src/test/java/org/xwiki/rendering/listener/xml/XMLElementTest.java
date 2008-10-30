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
package org.xwiki.rendering.listener.xml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

/**
 * Unit tests for {@link XMLElement}.
 * 
 * @version $Id $
 * @since 1.7MZ
 */
public class XMLElementTest extends TestCase
{
    /**
     * Verify attribute order is kept (i.e. same order as what the user enters).
     * @todo this test is not conclusive since it can happen that the map has the same order even though it's not
     *       using a fixed order. If someone knows of a better test please fix this.
     */
    public void testAttributeOrder()
    {
        Map<String, String> attributes = new HashMap<String, String>();
        for (int i = 0; i < 10; i++) {
            attributes.put("key" + i, "value" + i);
        }
        XMLElement element = new XMLElement("element", attributes);
        
        Iterator<String> it = attributes.keySet().iterator();
        Iterator<String> newIt = element.getAttributes().keySet().iterator();
        while (it.hasNext()) {
            assertEquals(newIt.next(), it.next());
        }
    }
}
