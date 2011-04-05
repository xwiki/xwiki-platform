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
package org.xwiki.rendering.internal.macro.rss;

import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.rss.RssMacroParameters;
import org.jmock.Mockery;
import org.jmock.Expectations;
import org.junit.Test;
import org.junit.Before;
import org.junit.Assert;

/**
 * Unit tests for {@link RssMacro}.
 * 
 * @version $Id$
 * @since 1.9
 */
public class RssMacroTest
{
    private RssMacro macro;

    @Before
    public void setUp()
    {
        this.macro = new RssMacro();
    }

    /**
     * Tests whether the macro throws the appropriate exception 
     * in cases where the required 'feed' parameter is missing.
     */
    @Test
    public void testRequiredParameterMissing() throws Exception
    {
        try {
            this.macro.execute(new RssMacroParameters(), null, null);
            Assert.fail("Should have thrown an exception");
        } catch (MacroExecutionException expected) {
            Assert.assertEquals("The required 'feed' parameter is missing", expected.getMessage());
        }
    }

    /**
     * Tests the macro's behavior when the server hosting the feeds doesn't respond.
     */
    @Test(expected = MacroExecutionException.class)
    public void testInvalidDocument() throws Exception
    {
        // Use a Mock SyndFeedInput to control what it returns for the test.
        Mockery context = new Mockery();
        final RomeFeedFactory mockFactory = context.mock(RomeFeedFactory.class);
        final RssMacroParameters parameters = new RssMacroParameters();
        context.checking(new Expectations() {{
            oneOf(mockFactory).createFeed(with(same(parameters))); will(throwException(
                new MacroExecutionException("Error")));
        }});
        this.macro.setFeedFactory(mockFactory);

        // Dummy URL since a feed URL is mandatory
        parameters.setFeed("http://www.xwiki.org");
        
        this.macro.execute(parameters, null, null);
    }
}
