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
package org.xwiki.uiextension;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.uiextension.internal.filter.ExcludeFilter;
import org.xwiki.uiextension.internal.filter.SelectFilter;
import org.xwiki.uiextension.internal.filter.SortByCustomOrderFilter;
import org.xwiki.uiextension.internal.filter.SortByIdFilter;
import org.xwiki.uiextension.internal.filter.SortByParameterFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.xwiki.uiextension.UIExtensions.TestUix1valueZ;
import static org.xwiki.uiextension.UIExtensions.TestUix2valueY;
import static org.xwiki.uiextension.UIExtensions.TestUix3valueX;
import static org.xwiki.uiextension.UIExtensions.TestUix4valueW;
import static org.xwiki.uiextension.UIExtensions.TestUix5value1;
import static org.xwiki.uiextension.UIExtensions.TestUix6value11;
import static org.xwiki.uiextension.UIExtensions.TestUix7value2;

class UIExtensionPointFiltersTest
{
    private final static UIExtension TEST_UIX_1_VALUE_Z = new TestUix1valueZ();

    private final static UIExtension TEST_UIX_2_VALUE_Y = new TestUix2valueY();

    private final static UIExtension TEST_UIX_3_VALUE_X = new TestUix3valueX();

    private final static UIExtension TEST_UIX_4_VALUE_W = new TestUix4valueW();

    private final static UIExtension TEST_UIX_5_VALUE_1 = new TestUix5value1();

    private final static UIExtension TEST_UIX_6_VALUE_11 = new TestUix6value11();

    private final static UIExtension TEST_UIX_7_VALUE_2 = new TestUix7value2();

    public static Stream<Arguments> filterSource()
    {
        return Stream.of(
            arguments(
                named("ExcludeFilter", new ExcludeFilter()),
                new String[] { "platform.testuix2", "platform.testuix3" },
                List.of(TEST_UIX_6_VALUE_11, TEST_UIX_1_VALUE_Z, TEST_UIX_5_VALUE_1, TEST_UIX_4_VALUE_W,
                    TEST_UIX_7_VALUE_2)
            ),
            arguments(
                named("SelectFilter", new SelectFilter()),
                new String[] { "platform.testuix2", "platform.testuix3", "platform.testuix4" },
                // The extensions must be ordered as in the select clause above.
                List.of(TEST_UIX_2_VALUE_Y, TEST_UIX_3_VALUE_X, TEST_UIX_4_VALUE_W)
            ),
            arguments(
                named("SortByCustomOrderFilter", new SortByCustomOrderFilter()),
                new String[] { "platform.testuix2", "platform.testuix3" },
                List.of(
                    // The first 2 are placed at the beginning, in the correct order
                    TEST_UIX_2_VALUE_Y, TEST_UIX_3_VALUE_X,
                    // The order of the others is preserved
                    TEST_UIX_6_VALUE_11, TEST_UIX_1_VALUE_Z, TEST_UIX_5_VALUE_1, TEST_UIX_4_VALUE_W, TEST_UIX_7_VALUE_2)
            ),
            arguments(
                named("SortByIdFilter", new SortByIdFilter()),
                new String[] {},
                List.of(TEST_UIX_1_VALUE_Z, TEST_UIX_2_VALUE_Y, TEST_UIX_3_VALUE_X, TEST_UIX_4_VALUE_W,
                    TEST_UIX_5_VALUE_1, TEST_UIX_6_VALUE_11, TEST_UIX_7_VALUE_2)
            ),
            arguments(
                named("SortByParameterFilter", new SortByParameterFilter()),
                new String[] { "key" },
                List.of(TEST_UIX_5_VALUE_1, TEST_UIX_7_VALUE_2, TEST_UIX_6_VALUE_11, TEST_UIX_4_VALUE_W,
                    TEST_UIX_3_VALUE_X, TEST_UIX_2_VALUE_Y, TEST_UIX_1_VALUE_Z)
            )
        );
    }

    @ParameterizedTest(name = "{0} - {1}")
    @MethodSource("filterSource")
    void filter(UIExtensionFilter filter, String[] parameters, List<UIExtension> expected)
    {
        var extensions =
            List.of(TEST_UIX_3_VALUE_X, TEST_UIX_6_VALUE_11, TEST_UIX_1_VALUE_Z, TEST_UIX_5_VALUE_1, TEST_UIX_4_VALUE_W,
                TEST_UIX_7_VALUE_2, TEST_UIX_2_VALUE_Y);
        assertEquals(expected, filter.filter(extensions, parameters));
    }
}
