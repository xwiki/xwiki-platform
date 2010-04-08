package org.xwiki.rendering.util;

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
import junit.framework.TestCase;

/**
 * Validate {@link IdGenerator}.
 * 
 * @version $Id$
 */
public class IdGeneratorTest extends TestCase
{
    private IdGenerator idGenerator;
    
    /**
     * {@inheritDoc}
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        this.idGenerator = new IdGenerator();
    }
    
    public void testGenerateUniqueId()
    {
        assertEquals("Itext", this.idGenerator.generateUniqueId("text"));
        assertEquals("Itext-1", this.idGenerator.generateUniqueId("te xt"));
    }
    
    public void testGenerateUniqueIdWithPrefix()
    {
        assertEquals("prefixtext", this.idGenerator.generateUniqueId("prefix", "text"));
        assertEquals("prefixtext-1", this.idGenerator.generateUniqueId("prefix", "te xt"));
    }
    
    public void testGenerateUniqueIdFromNonAlphaNum()
    {
        assertEquals("I:_.-", this.idGenerator.generateUniqueId(":_.-"));
        assertEquals("Iwithspace", this.idGenerator.generateUniqueId("with space"));
        assertEquals("Iwithtab", this.idGenerator.generateUniqueId("with\ttab"));
        assertEquals("IE5AF86E7A081", this.idGenerator.generateUniqueId("\u5BC6\u7801"));
    }

    public void testGenerateUniqueIdWhenInvalidEmptyPrefix()
    {
        try {
            this.idGenerator.generateUniqueId("", "whatever");
            fail("Should have thrown an exception");
        } catch (IllegalArgumentException expected) {
            assertEquals("The prefix [] should only contain alphanumerical characters and not be empty.",
                expected.getMessage());
        }
    }

    public void testGenerateUniqueIdWhenInvalidNonAlphaPrefix()
    {
        try {
            this.idGenerator.generateUniqueId("a-b", "whatever");
            fail("Should have thrown an exception");
        } catch (IllegalArgumentException expected) {
            assertEquals("The prefix [a-b] should only contain alphanumerical characters and not be empty.",
                expected.getMessage());
        }
    }
}
