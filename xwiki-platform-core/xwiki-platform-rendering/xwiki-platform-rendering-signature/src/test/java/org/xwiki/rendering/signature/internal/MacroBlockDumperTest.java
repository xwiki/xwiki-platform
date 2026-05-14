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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test for {@link org.xwiki.rendering.signature.internal.MacroBlockDumper}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@ComponentTest
class MacroBlockDumperTest
{
    @InjectMockComponents
    private MacroBlockDumper dumper;

    private Map<String, String> params = new HashMap<>();

    @Test
    void dumpMacroBlockWithoutSource() throws Exception
    {
        byte[] dump1 = this.dumper.dump(new MacroBlock("macro", this.params, "content", false));
        byte[] dump2 = this.dumper.dump(new MacroBlock("macro", this.params, "content", false));
        assertArrayEquals(dump1, dump2);

        dump2 = this.dumper.dump(new MacroBlock("macro", this.params, "content", true));
        assertArrayEquals(dump1, dump2);

        dump2 = this.dumper.dump(new MacroBlock("macro2", this.params, "content", false));
        assertFalse(Arrays.equals(dump1, dump2));

        dump2 = this.dumper.dump(new MacroBlock("macro", this.params, false));
        assertFalse(Arrays.equals(dump1, dump2));

        dump2 = this.dumper.dump(new MacroBlock("macro", this.params, "content2", false));
        assertFalse(Arrays.equals(dump1, dump2));

        this.params.put("param", "value");
        dump2 = this.dumper.dump(new MacroBlock("macro", this.params, "content", false));
        assertFalse(Arrays.equals(dump1, dump2));

        dump1 = this.dumper.dump(new MacroBlock("macro", this.params, "content", false));
        assertArrayEquals(dump1, dump2);

        this.params.put("param", "value2");
        dump2 = this.dumper.dump(new MacroBlock("macro", this.params, "content", false));
        assertFalse(Arrays.equals(dump1, dump2));

        this.params.clear();
        this.params.put("param2", "value");
        dump2 = this.dumper.dump(new MacroBlock("macro", this.params, "content", false));
        assertFalse(Arrays.equals(dump1, dump2));

        this.params.clear();
        this.params.put("param", "value");
        this.params.put("param2", "value2");
        dump2 = this.dumper.dump(new MacroBlock("macro", this.params, "content", false));
        assertFalse(Arrays.equals(dump1, dump2));
    }

    @Test
    void dumpMacroBlockWithSource() throws Exception
    {
        Block macro1 = new MacroBlock("macro", this.params, "content", false);
        Block macro2 = new MacroBlock("macro", this.params, "content", false);
        XDOM xdom1 = new XDOM(List.of(macro1));
        XDOM xdom2 = new XDOM(List.of(macro2));
        xdom1.getMetaData().addMetaData(MetaData.SOURCE, "source");
        xdom2.getMetaData().addMetaData(MetaData.SOURCE, "source");

        byte[] dump1 = this.dumper.dump(macro1);
        byte[] dump2 = this.dumper.dump(macro2);
        assertArrayEquals(dump1, dump2);

        dump2 = this.dumper.dump(new MacroBlock("macro", this.params, "content", false));
        assertFalse(Arrays.equals(dump1, dump2));

        xdom2.getMetaData().addMetaData(MetaData.SOURCE, "other");
        dump2 = this.dumper.dump(macro2);
        assertFalse(Arrays.equals(dump1, dump2));

        xdom2.getMetaData().addMetaData(MetaData.SOURCE, null);
        dump1 = this.dumper.dump(new MacroBlock("macro", this.params, "content", false));
        dump2 = this.dumper.dump(macro2);
        assertArrayEquals(dump1, dump2);
    }

    @Test
    void dumpMacroMarkerBlockWithoutSource() throws Exception
    {
        byte[] dump1 = this.dumper.dump(new MacroBlock("macro", this.params, "content", false));
        byte[] dump2 = this.dumper.dump(
            new MacroMarkerBlock("macro", this.params, "content", List.of(), false));
        assertArrayEquals(dump1, dump2);

        this.params.put("param", "value");
        this.params.put("param2", "value2");
        dump1 = this.dumper.dump(new MacroBlock("macro", this.params, "content", false));
        dump2 = this.dumper.dump(
            new MacroMarkerBlock("macro", this.params, "content", List.of(), false));
        assertArrayEquals(dump1, dump2);
    }

    @Test
    void dumpMacroMarkerBlockWithSource() throws Exception
    {
        Block macro1 = new MacroBlock("macro", this.params, "content", false);
        Block macro2 = new MacroMarkerBlock("macro", this.params, "content", List.of(), false);
        XDOM xdom1 = new XDOM(List.of(macro1));
        XDOM xdom2 = new XDOM(List.of(macro2));
        xdom1.getMetaData().addMetaData(MetaData.SOURCE, "source");
        xdom2.getMetaData().addMetaData(MetaData.SOURCE, "source");

        byte[] dump1 = this.dumper.dump(macro1);
        byte[] dump2 = this.dumper.dump(macro2);
        assertArrayEquals(dump1, dump2);
    }

    @Test
    void illegalArgumentException()
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> this.dumper.dump(new WordBlock("macro")));
        assertEquals("Unsupported block [org.xwiki.rendering.block.WordBlock].", exception.getMessage());
    }
}
