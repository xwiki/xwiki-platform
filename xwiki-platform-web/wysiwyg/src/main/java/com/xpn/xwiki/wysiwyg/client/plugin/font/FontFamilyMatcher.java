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
package com.xpn.xwiki.wysiwyg.client.plugin.font;

import org.xwiki.gwt.dom.client.Style;

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
    public static final String TEST_FONT_SIZE = "50px";

    /**
     * Creates a new font family matcher.
     */
    public FontFamilyMatcher()
    {
        super("mmmmmmmmmwwwwwww");

        left.getStyle().setProperty(Style.FONT_SIZE.getJSName(), TEST_FONT_SIZE);
        right.getStyle().setProperty(Style.FONT_SIZE.getJSName(), TEST_FONT_SIZE);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Matcher#match(Object, Object)
     */
    public boolean match(String leftValue, String rightValue)
    {
        left.getStyle().setProperty(Style.FONT_FAMILY.getJSName(), leftValue);
        right.getStyle().setProperty(Style.FONT_FAMILY.getJSName(), rightValue);
        return left.getOffsetWidth() == right.getOffsetWidth() && left.getOffsetHeight() == right.getOffsetHeight();
    }
}
