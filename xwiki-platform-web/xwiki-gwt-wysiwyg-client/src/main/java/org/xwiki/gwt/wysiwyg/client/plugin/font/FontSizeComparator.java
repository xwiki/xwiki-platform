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

import java.util.Comparator;

import org.xwiki.gwt.dom.client.Style;

/**
 * Compares two font sizes. This is not trivial as it may seem because font-size CSS property can take different kind of
 * values.
 * 
 * @version $Id$
 */
public class FontSizeComparator extends AbstractFontMatcher implements Comparator<String>
{
    /**
     * Creates a new font size comparator.
     */
    public FontSizeComparator()
    {
        super("m");
    }

    /**
     * {@inheritDoc}
     * 
     * @see Comparator#compare(Object, Object)
     */
    public int compare(String leftValue, String rightValue)
    {
        left.getStyle().setProperty(Style.FONT_SIZE.getJSName(), leftValue);
        right.getStyle().setProperty(Style.FONT_SIZE.getJSName(), rightValue);
        return left.getOffsetWidth() - right.getOffsetWidth();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Matcher#match(Object, Object)
     */
    public boolean match(String leftValue, String rightValue)
    {
        return super.match(leftValue, rightValue) || compare(leftValue, rightValue) == 0;
    }
}
