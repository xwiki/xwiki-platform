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
 * Tests the {@link org.xwiki.annotation.internal.content.SpaceNormalizerContentAlterer}.
 * 
 * @version $Id$
 */
@RunWith(Parameterized.class)
public class SpaceNormalizerContentAltererTest extends AbstractComponentTestCase
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
    public SpaceNormalizerContentAltererTest(String initial, String altered)
    {
        this.initial = initial;
        this.altered = altered;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.test.AbstractComponentTestCase#setUp()
     */
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        alterer = getComponentManager().lookup(ContentAlterer.class, "space-normalizer");
    }

    /**
     * @return list of corpus files to instantiate tests for
     */
    @Parameters
    public static Collection<String[]> data()
    {
        Collection<String[]> params = new ArrayList<String[]>();
        // unbreakable space
        params.add(new String[] {"not\u00A0to be", "not to be"});
        // tabs
        params.add(new String[] {"to be or not\tto be", "to be or not to be"});
        // commas, signs with regular spaces
        params.add(new String[] {"roses, see I in her cheeks;", "roses, see I in her cheeks;"});
        // new lines
        params.add(new String[] {"eyes nothing\nlike the sun", "eyes nothing like the sun"});
        // new line carriage return
        params.add(new String[] {"eyes\n\rnothing", "eyes nothing"});
        // multiple spaces one after the other
        params.add(new String[] {"roses, see I   in her cheeks;", "roses, see I in her cheeks;"});
        params.add(new String[] {"roses, see I\u00A0  in her cheeks;", "roses, see I in her cheeks;"});
        params.add(new String[] {"roses, see I\n  \n in her cheeks;", "roses, see I in her cheeks;"});
        // trim
        params.add(new String[] {" roses, see I in her cheeks; ", "roses, see I in her cheeks;"});
        params.add(new String[] {"\n\n\nroses, see I in her cheeks;", "roses, see I in her cheeks;"});
        params.add(new String[] {"roses, see I in her cheeks;\n\n", "roses, see I in her cheeks;"});
        // starting or ending with a non-breakable space
        params.add(new String[] {"\u00A0roses, see I in her cheeks;", "roses, see I in her cheeks;"});
        params.add(new String[] {"roses, see I in her cheeks;\u00A0", "roses, see I in her cheeks;"});
        
        // empty string should stay empty string
        params.add(new String[] {"", ""});
        // spaces only string should become empty string
        params.add(new String[] {" \t\n", ""});

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

    // TODO: test indexes to be in the right place
}
