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
package org.xwiki.platform.wiki.creationjob;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @version $Id$
 */
public class WikiCreationExceptionTest
{
    @Test
    public void equalsAndHashcode() throws Exception
    {
        Throwable throwable = new Throwable();
        
        Exception e1 = new WikiCreationException("message", throwable);
        Exception e2 = new WikiCreationException("message", throwable);
        Exception e3 = new WikiCreationException("message", throwable);
        Exception e4 = new WikiCreationException("message2", throwable);
        Exception e5 = new WikiCreationException("message", new Throwable());
        Exception e6 = new Exception();
        
        // Reflective
        assertEquals(e1, e1);

        // Symetric
        assertEquals(e1, e2);
        assertEquals(e2, e1);
                        
        // Transitivity
        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
        assertEquals(e2, e3);
        assertEquals(e2.hashCode(), e3.hashCode());
        assertEquals(e1, e3);
        assertEquals(e1.hashCode(), e3.hashCode());
        
        // Other
        assertNotEquals(e4, e1);
        assertNotEquals(e4.hashCode(), e1.hashCode());
        assertNotEquals(e1, e5);
        assertNotEquals(e5, e1);
        assertNotEquals(e1.hashCode(), e5.hashCode());
        assertNotEquals(e6, e1);
        assertNotEquals(e1, e6);
        assertNotEquals(e1.hashCode(), e6.hashCode());
    }
}
