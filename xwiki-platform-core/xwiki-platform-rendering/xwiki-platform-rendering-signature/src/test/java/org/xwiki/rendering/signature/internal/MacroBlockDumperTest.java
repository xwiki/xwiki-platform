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
package org.xwiki.rendering.signature.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit test for {@link org.xwiki.rendering.signature.internal.MacroBlockDumper}.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class MacroBlockDumperTest
{
    @Rule
    public final MockitoComponentMockingRule<BlockDumper> mocker =
        new MockitoComponentMockingRule<BlockDumper>(MacroBlockDumper.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Map<String, String> params = new HashMap<>();

    private BlockDumper dumper;

    @Before
    public void setUp() throws Exception
    {
        dumper = mocker.getComponentUnderTest();
    }

    @Test
    public void testDumpMacroBlockWithoutSource() throws Exception
    {
        byte[] dump1 = dumper.dump(new MacroBlock("macro", params, "content", false));
        byte[] dump2 = dumper.dump(new MacroBlock("macro", params, "content", false));
        assertThat(dump1, equalTo(dump2));

        dump2 = dumper.dump(new MacroBlock("macro", params, "content", true));
        assertThat(dump1, equalTo(dump2));

        dump2 = dumper.dump(new MacroBlock("macro2", params, "content", false));
        assertThat(dump1, not(equalTo(dump2)));

        dump2 = dumper.dump(new MacroBlock("macro", params, false));
        assertThat(dump1, not(equalTo(dump2)));

        dump2 = dumper.dump(new MacroBlock("macro", params, "content2", false));
        assertThat(dump1, not(equalTo(dump2)));

        params.put("param", "value");
        dump2 = dumper.dump(new MacroBlock("macro", params, "content", false));
        assertThat(dump1, not(equalTo(dump2)));

        dump1 = dumper.dump(new MacroBlock("macro", params, "content", false));
        assertThat(dump1, equalTo(dump2));

        params.put("param", "value2");
        dump2 = dumper.dump(new MacroBlock("macro", params, "content", false));
        assertThat(dump1, not(equalTo(dump2)));

        params.clear();
        params.put("param2", "value");
        dump2 = dumper.dump(new MacroBlock("macro", params, "content", false));
        assertThat(dump1, not(equalTo(dump2)));

        params.clear();
        params.put("param", "value");
        params.put("param2", "value2");
        dump2 = dumper.dump(new MacroBlock("macro", params, "content", false));
        assertThat(dump1, not(equalTo(dump2)));
    }

    @Test
    public void testDumpMacroBlockWithSource() throws Exception
    {
        Block macro1 = new MacroBlock("macro", params, "content", false);
        Block macro2 = new MacroBlock("macro", params, "content", false);
        XDOM xdom1 = new XDOM(Collections.singletonList(macro1));
        XDOM xdom2 = new XDOM(Collections.singletonList(macro2));
        xdom1.getMetaData().addMetaData(MetaData.SOURCE, "source");
        xdom2.getMetaData().addMetaData(MetaData.SOURCE, "source");

        byte[] dump1 = dumper.dump(macro1);
        byte[] dump2 = dumper.dump(macro2);
        assertThat(dump1, equalTo(dump2));

        dump2 = dumper.dump(new MacroBlock("macro", params, "content", false));
        assertThat(dump1, not(equalTo(dump2)));

        xdom2.getMetaData().addMetaData(MetaData.SOURCE, "other");
        dump2 = dumper.dump(macro2);
        assertThat(dump1, not(equalTo(dump2)));

        xdom2.getMetaData().addMetaData(MetaData.SOURCE, null);
        dump1 = dumper.dump(new MacroBlock("macro", params, "content", false));
        dump2 = dumper.dump(macro2);
        assertThat(dump1, equalTo(dump2));
    }

    @Test
    public void testDumpMacroMarkerBlockWithoutSource() throws Exception
    {
        byte[] dump1 = dumper.dump(new MacroBlock("macro", params, "content", false));
        byte[] dump2 = dumper.dump(new MacroMarkerBlock("macro", params, "content", Collections.<Block>emptyList(), false));
        assertThat(dump1, equalTo(dump2));

        params.put("param", "value");
        params.put("param2", "value2");
        dump1 = dumper.dump(new MacroBlock("macro", params, "content", false));
        dump2 = dumper.dump(new MacroMarkerBlock("macro", params, "content", Collections.<Block>emptyList(), false));
        assertThat(dump1, equalTo(dump2));
    }

    @Test
    public void testDumpMacroMarkerBlockWithSource() throws Exception
    {
        Block macro1 = new MacroBlock("macro", params, "content", false);
        Block macro2 = new MacroMarkerBlock("macro", params, "content", Collections.<Block>emptyList(), false);
        XDOM xdom1 = new XDOM(Collections.singletonList(macro1));
        XDOM xdom2 = new XDOM(Collections.singletonList(macro2));
        xdom1.getMetaData().addMetaData(MetaData.SOURCE, "source");
        xdom2.getMetaData().addMetaData(MetaData.SOURCE, "source");

        byte[] dump1 = dumper.dump(macro1);
        byte[] dump2 = dumper.dump(macro2);
        assertThat(dump1, equalTo(dump2));
    }

    @Test
    public void testIllegalArgumentException() throws Exception
    {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported block [org.xwiki.rendering.block.WordBlock].");

        dumper.dump(new WordBlock("macro"));
    }
}


