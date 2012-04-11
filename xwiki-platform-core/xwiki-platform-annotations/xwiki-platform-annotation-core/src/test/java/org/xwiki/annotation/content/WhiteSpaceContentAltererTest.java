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
package org.xwiki.annotation.content;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * @version $Id$
 * @since 2.3M1
 */
@RunWith(Parameterized.class)
public class WhiteSpaceContentAltererTest extends AbstractComponentTestCase
{
    /**
     * The initial String to alter.
     */
    private String initial;

    /**
     * The expected altered string.
     */
    private String altered;

    /**
     * The content alterer to test.
     */
    private ContentAlterer alterer;

    /**
     * @param initial the original string
     * @param altered the altered string after being whitespace filtered
     */
    public WhiteSpaceContentAltererTest(String initial, String altered)
    {
        this.initial = initial;
        this.altered = altered;
    }

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        alterer = getComponentManager().getInstance(ContentAlterer.class, "whitespace");
    }

    /**
     * @return list of corpus files to instantiate tests for
     */
    @Parameters
    public static Collection<String[]> data()
    {
        Collection<String[]> params = new ArrayList<String[]>();
        // unbreakable space
        params.add(new String[] {"not\u00A0to be", "nottobe"});
        // tabs
        params.add(new String[] {"to be or not\tto be", "tobeornottobe"});
        // commas, signs with regular spaces
        params.add(new String[] {"roses, see I in her cheeks;", "roses,seeIinhercheeks;"});
        // new lines
        params.add(new String[] {"eyes nothing\nlike the sun", "eyesnothinglikethesun"});
        // new line carriage return
        params.add(new String[] {"eyes\n\rnothing", "eyesnothing"});
        return params;
    }

    /**
     * Tests that the content alterer filters correctly the characters out of the Strings.
     */
    @Test
    public void testFiltering()
    {
        AlteredContent alteredContent = alterer.alter(initial);
        assertEquals(altered, alteredContent.getContent().toString());
    }
}
