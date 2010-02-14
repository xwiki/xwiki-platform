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
package org.xwiki.gwt.wysiwyg.client.plugin.font;

import org.xwiki.gwt.dom.client.Style;

import com.google.gwt.dom.client.Style.Unit;

/**
 * Matches values of the font-family CSS property.
 * 
 * @version $Id$
 */
public class FontFamilyMatcher extends AbstractFontMatcher
{
    /**
     * A larger font size increases the accuracy but may lower the speed of the test.
     */
    public static final int TEST_FONT_SIZE = 50;

    /**
     * The suffix added to a font family name in order to default it to the generic serif family.
     */
    public static final String SERIF_SUFFIX = ",serif";

    /**
     * The suffix added to a font family name in order to default it to the generic sans-serif family.
     */
    public static final String SANS_SERIF_SUFFIX = ",sans-serif";

    /**
     * Creates a new font family matcher.
     */
    public FontFamilyMatcher()
    {
        super("mmmmmmmmmwwwwwww");

        left.getStyle().setFontSize(TEST_FONT_SIZE, Unit.PX);
        right.getStyle().setFontSize(TEST_FONT_SIZE, Unit.PX);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Matcher#match(Object, Object)
     */
    public boolean match(String leftValue, String rightValue)
    {
        if (super.match(leftValue, rightValue)) {
            return true;
        } else {
            // If both values are not supported by the browser then the font-family property is defaulted to the same
            // value causing a false positive result. To prevent this we add different suffixes to make sure the font
            // family defaults to different values.
            left.getStyle().setProperty(Style.FONT_FAMILY.getJSName(), leftValue + SANS_SERIF_SUFFIX);
            right.getStyle().setProperty(Style.FONT_FAMILY.getJSName(), rightValue + SERIF_SUFFIX);
            if (left.getOffsetWidth() != right.getOffsetWidth() || left.getOffsetHeight() != right.getOffsetHeight()) {
                return false;
            } else {
                // Event if the values passed the previous test we are still not 100% sure they match. We can have:
                // left: unsupported1,serif + ,sans-serif
                // right: unsupported2 + ,serif
                // By switching the suffixes we can exclude this rare case.
                left.getStyle().setProperty(Style.FONT_FAMILY.getJSName(), leftValue + SERIF_SUFFIX);
                right.getStyle().setProperty(Style.FONT_FAMILY.getJSName(), rightValue + SANS_SERIF_SUFFIX);
                return left.getOffsetWidth() == right.getOffsetWidth()
                    && left.getOffsetHeight() == right.getOffsetHeight();
            }
        }
    }
}
