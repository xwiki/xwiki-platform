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
package org.xwiki.gwt.dom.client;

import com.google.gwt.core.client.GWT;

/**
 * Extends the {@link com.google.gwt.dom.client.Style} to add constants for standard property names and values.
 * 
 * @version $Id$
 */
public class Style extends com.google.gwt.dom.client.Style
{
    /**
     * The name of the style attribute.
     */
    public static final String STYLE_ATTRIBUTE = "style";

    /**
     * The name of the style property holding the value of the {@link #STYLE_ATTRIBUTE}.
     */
    public static final String STYLE_PROPERTY = "cssText";

    /**
     * Sets how/if an element is displayed.
     */
    public static final String DISPLAY = "display";

    /**
     * Sets the stack order of an element. An element with greater stack order is always in front of another element
     * with lower stack order.
     * <p>
     * Elements can have negative stack orders.
     * <p>
     * Z-index only works on elements that have been positioned (eg position:absolute;)!
     */
    public static final String Z_INDEX = "zIndex";

    /**
     * Sets how thick or thin characters in text should be displayed.
     */
    public static final Property FONT_WEIGHT =
        new Property("font-weight", "fontWeight", true, false, FontWeight.NORMAL);

    /**
     * Sets the style of a font.
     */
    public static final Property FONT_STYLE = new Property("font-style", "fontStyle", true, false, FontStyle.NORMAL);

    /**
     * Decorates the text.
     */
    public static final Property TEXT_DECORATION = new Property("text-decoration", "textDecoration", false, true,
        TextDecoration.NONE);

    /**
     * The text-align property aligns the text in an element.
     */
    public static final Property TEXT_ALIGN = new Property("text-align", "textAlign", true, false, "");

    /**
     * The font-family property is a prioritized list of font family names and/or generic family names for an element.
     * The browser will use the first value it recognizes.
     * <p>
     * There are two types of font-family values:
     * <ul>
     * <li>family-name: "times", "courier", "arial", etc.</li>
     * <li>generic-family: "serif", "sans-serif", "cursive", "fantasy", "monospace".</li>
     * </ul>
     * Note: Separate each value with a comma, and always offer a generic-family name as the last alternative.
     * <p>
     * Note: If a family-name contains white-space, it should be quoted. Single quotes must be used when using the
     * "style" attribute in HTML.
     */
    public static final Property FONT_FAMILY = new Property("font-family", "fontFamily", true, true, "");

    /**
     * The font-size property sets the size of a font.
     */
    public static final Property FONT_SIZE = new Property("font-size", "fontSize", true, false, FontSize.MEDIUM);

    /**
     * The color property specifies the color of text.
     */
    public static final Property COLOR = new Property("color", true, false, "");

    /**
     * The background-color property sets the background color of an element.
     * <p>
     * The background of an element is the total size of the element, including padding and border (but not the margin).
     */
    public static final Property BACKGROUND_COLOR = new Property("background-color", "backgroundColor", false, false,
        "transparent");

    /**
     * Sets the width of an element.
     */
    public static final String WIDTH = "width";

    /**
     * sets the height of an element.
     */
    public static final String HEIGHT = "height";

    /**
     * Sets how far the top edge of an element is above/below the top edge of the parent element.
     */
    public static final String TOP = "top";

    /**
     * Sets how far the left edge of an element is to the right/left of the left edge of the parent element.
     */
    public static final String LEFT = "left";

    /**
     * Places an element in a static, relative, absolute or fixed position.
     */
    public static final String POSITION = "position";

    /**
     * The visibility property sets if an element should be visible or invisible. Even invisible elements takes up space
     * on the page. Use the {@link #DISPLAY} property to create invisible elements that do not take up space.
     */
    public static final String VISIBILITY = "visibility";

    /**
     * The float property specifies whether or not a box (an element) should float.
     */
    public static final Property FLOAT = GWT.create(FloatProperty.class);

    /**
     * The margin-left property sets the left margin of an element.
     */
    public static final Property MARGIN_LEFT = new Property("margin-left", "marginLeft", false, false, "0");

    /**
     * The margin-right property sets the right margin of an element.
     */
    public static final Property MARGIN_RIGHT = new Property("margin-right", "marginRight", false, false, "0");

    /**
     * The vertical-align property sets the vertical alignment of an element.
     */
    public static final Property VERTICAL_ALIGN = new Property("vertical-align", "verticalAlign", false, false,
        "baseline");

    /**
     * Standard values for {@link Style#DISPLAY}.
     */
    public static final class Display
    {
        /**
         * Default. The element will be displayed as an inline element, with no line break before or after the element.
         */
        public static final String INLINE = "inline";

        /**
         * The element will be displayed as a block-level element, with a line break before and after the element.
         */
        public static final String BLOCK = "block";

        /**
         * The element will not be displayed.
         */
        public static final String NONE = "none";

        /**
         * This is a utility class so it has a private constructor.
         */
        private Display()
        {
        }
    }

    /**
     * Standard values for {@link Style#FONT_WEIGHT}.
     */
    public static final class FontWeight
    {
        /**
         * Default. Defines normal characters.
         */
        public static final String NORMAL = FontStyle.NORMAL;

        /**
         * Defines thick characters.
         */
        public static final String BOLD = "bold";

        /**
         * Defines thicker characters.
         */
        public static final String BOLDER = "bolder";

        /**
         * This is a utility class so it has a private constructor.
         */
        private FontWeight()
        {
        }
    }

    /**
     * Standard values for {@link Style#FONT_STYLE}.
     */
    public static final class FontStyle
    {
        /**
         * Default. The browser displays a normal font.
         */
        public static final String NORMAL = "normal";

        /**
         * The browser displays an italic font.
         */
        public static final String ITALIC = "italic";

        /**
         * This is a utility class so it has a private constructor.
         */
        private FontStyle()
        {
        }
    }

    /**
     * Standard values for {@link Style#TEXT_DECORATION}.
     */
    public static final class TextDecoration
    {
        /**
         * Default. Defines a normal text.
         */
        public static final String NONE = Display.NONE;

        /**
         * Defines a line through the text.
         */
        public static final String LINE_THROUGH = "line-through";

        /**
         * Defines a line under the text.
         */
        public static final String UNDERLINE = "underline";

        /**
         * This is a utility class so it has a private constructor.
         */
        private TextDecoration()
        {
        }
    }

    /**
     * Standard values for {@link Style#POSITION}.
     */
    public static final class Position
    {
        /**
         * Default. An element with position: static always has the position the normal flow of the page gives it (a
         * static element ignores any top, bottom, left, or right declarations).
         */
        public static final String STATIC = "static";

        /**
         * An element with position: relative moves an element relative to its normal position, so "left:20" adds 20
         * pixels to the element's LEFT position.
         */
        public static final String RELATIVE = "relative";

        /**
         * An element with position: absolute is positioned at the specified coordinates relative to its containing
         * block. The element's position is specified with the "left", "top", "right", and "bottom" properties.
         */
        public static final String ABSOLUTE = "absolute";

        /**
         * An element with position: fixed is positioned at the specified coordinates relative to the browser window.
         * The element's position is specified with the "left", "top", "right", and "bottom" properties. The element
         * remains at that position regardless of scrolling. Works in IE7 (strict mode).
         */
        public static final String FIXED = "fixed";

        /**
         * This is a utility class so it has a private constructor.
         */
        private Position()
        {
        }
    }

    /**
     * The text-align property aligns the text in an element.
     * 
     * See http://www.w3.org/TR/css3-text/#justification.
     */
    public static final class TextAlign
    {
        /**
         * Aligns the text to the left.
         */
        public static final String LEFT = Style.LEFT;

        /**
         * Aligns the text to the right.
         */
        public static final String RIGHT = "right";

        /**
         * Centers the text.
         */
        public static final String CENTER = "center";

        /**
         * Increases the spaces between words in order for lines to have the same width.
         */
        public static final String JUSTIFY = "justify";

        /**
         * This is a utility class so it has a private constructor.
         */
        private TextAlign()
        {
        }
    }

    /**
     * The font-size property sets the size of a font.
     */
    public static final class FontSize
    {
        /**
         * Default value.
         */
        public static final String MEDIUM = "medium";

        /**
         * Sets the font-size to a smaller size than the parent element.
         */
        public static final String SMALLER = "smaller";

        /**
         * Sets the font-size to a larger size than the parent element.
         */
        public static final String LARGER = "larger";

        /**
         * This is a utility class so it has a private constructor.
         */
        private FontSize()
        {
        }
    }

    /**
     * Standard values for {@link Style#VISIBILITY}.
     */
    public static final class Visibility
    {
        /**
         * The element is visible.
         */
        public static final String VISIBLE = "visible";

        /**
         * The element is invisible.
         */
        public static final String HIDDEN = "hidden";

        /**
         * When used in table elements, this value removes a row or column, but it does not affect the table layout. The
         * space taken up by the row or column will be available for other content. If this value is used on other
         * elements, it renders as "hidden".
         */
        public static final String COLLAPSE = "collapse";

        /**
         * This is a utility class so it has a private constructor.
         */
        private Visibility()
        {
        }
    }

    /**
     * Defines the {@code float} CSS property.
     */
    public static class FloatProperty extends Property
    {
        /**
         * Default constructor required in order to use the deferred binding mechanism.
         */
        public FloatProperty()
        {
            this("cssFloat");
        }

        /**
         * Protected constructor allowing browser specific implementations that use a different JavaScript name for this
         * property.
         * 
         * @param jsName the name of this CSS property as used in JavaScript
         */
        protected FloatProperty(String jsName)
        {
            super("float", jsName, false, false, Float.NONE);
        }
    }

    /**
     * Specific implementation of {@link FloatProperty} for older versions of Internet Explorer (6, 7 and 8).
     */
    public static final class IEOldFloatProperty extends FloatProperty
    {
        /**
         * Default constructor required in order to use the deferred binding mechanism.
         */
        public IEOldFloatProperty()
        {
            super("styleFloat");
        }
    }

    /**
     * Standard values for {@link Style#FLOAT}.
     */
    public static final class Float
    {
        /**
         * The element floats to the left.
         */
        public static final String LEFT = Style.LEFT;

        /**
         * The element floats the right.
         */
        public static final String RIGHT = TextAlign.RIGHT;

        /**
         * The element is not floated, and will be displayed just where it occurs in the text. This is default.
         */
        public static final String NONE = Display.NONE;

        /**
         * Specifies that the value of the float property should be inherited from the parent element.
         */
        public static final String INHERIT = "inherit";

        /**
         * This is a utility class so it has a private constructor.
         */
        private Float()
        {
        }
    }

    /**
     * Standard values for {@link Style#MARGIN_LEFT} and {@link Style#MARGIN_RIGHT} properties.
     */
    public static final class Margin
    {
        /**
         * The browser calculates a right margin.
         */
        public static final String AUTO = "auto";

        /**
         * Specifies that the right margin should be inherited from the parent element.
         */
        public static final String INHERIT = Float.INHERIT;

        /**
         * This is a utility class so it has a private constructor.
         */
        private Margin()
        {
        }
    }

    /**
     * Default constructor. Needs to be protected because all instances are created from JavaScript.
     */
    protected Style()
    {
    }

    /**
     * Some browsers expect the camel case form of a style property. For instance, the camel case form of "font-weight"
     * is "fontWeight".
     * 
     * @param propertyName The name of style property.
     * @return The camel case form of the given property name.
     */
    public static String toCamelCase(String propertyName)
    {
        int dashIndex = propertyName.indexOf('-');
        if (dashIndex < 0 || dashIndex == propertyName.length() - 1) {
            return propertyName;
        }
        StringBuffer camelCase = new StringBuffer(propertyName.substring(0, dashIndex));
        camelCase.append(propertyName.substring(dashIndex + 1, dashIndex + 2).toUpperCase());
        camelCase.append(propertyName.substring(dashIndex + 2));
        return camelCase.toString();
    }
}
