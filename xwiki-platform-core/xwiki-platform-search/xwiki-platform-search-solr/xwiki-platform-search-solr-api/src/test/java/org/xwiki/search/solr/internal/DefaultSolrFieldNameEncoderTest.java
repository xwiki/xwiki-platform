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
package org.xwiki.search.solr.internal;

import static org.junit.Assert.*;

import org.junit.Test;
import org.xwiki.search.solr.internal.api.SolrFieldNameEncoder;

/**
 * Unit tests for {@link DefaultSolrFieldNameEncoder}.
 * 
 * @version $Id$
 * @since 5.3RC1
 */
public class DefaultSolrFieldNameEncoderTest
{
    /**
     * The object being tested.
     */
    private SolrFieldNameEncoder encoder = new DefaultSolrFieldNameEncoder();

    @Test
    public void encode()
    {
        assertNull(encoder.encode(null));
        assertSame("", encoder.encode(""));
        assertSame("a1_.-\u0103", encoder.encode("a1_.-\u0103"));
        assertEquals("$3A$20$5E$2B$24", encoder.encode(": ^+$"));
    }

    @Test
    public void decode()
    {
        assertNull(encoder.decode(null));
        assertSame("", encoder.decode(""));
        assertSame("a1_.-\u0103", encoder.decode("a1_.-\u0103"));
        assertEquals(": ^+$", encoder.decode("$3A$20$5E$2B$24"));
    }
}
