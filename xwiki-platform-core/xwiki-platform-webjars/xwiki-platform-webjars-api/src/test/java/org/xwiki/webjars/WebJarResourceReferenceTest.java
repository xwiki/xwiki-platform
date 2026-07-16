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
package org.xwiki.webjars;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.webjars.internal.WebJarsResourceReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for {@link org.xwiki.webjars.internal.WebJarsResourceReference}.
 *
 * @version $Id$
 * @since 7.1M1
 */
class WebJarResourceReferenceTest
{
    @Test
    void equalsAndHashCode()
    {
        WebJarsResourceReference reference1 = new WebJarsResourceReference("namespace", List.of("one", "two"));
        reference1.addParameter("key1", "value1");
        reference1.addParameter("key2", new String[]{ "value2", "value3" });

        WebJarsResourceReference reference2 = new WebJarsResourceReference("namespace", List.of("one", "two"));
        reference2.addParameter("key1", "value1");
        reference2.addParameter("key2", new String[]{ "value2", "value3" });

        WebJarsResourceReference reference3 = new WebJarsResourceReference("namespace", List.of("one", "two"));

        WebJarsResourceReference reference4 = new WebJarsResourceReference("namespace2", List.of("one", "two"));

        assertEquals(reference2, reference1);
        assertEquals(reference2.hashCode(), reference1.hashCode());

        assertNotEquals(reference3, reference1);
        assertNotEquals(reference3.hashCode(), reference1.hashCode());

        assertNotEquals(reference4, reference3);
        assertNotEquals(reference4.hashCode(), reference3.hashCode());
    }
}
