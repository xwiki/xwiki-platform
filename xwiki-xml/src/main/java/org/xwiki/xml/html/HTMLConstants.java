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
 *
 */
package org.xwiki.xml.html;

/**
 * HTML Constants.
 *
 * @version $Id$
 * @since 1.6M2
 */
public interface HTMLConstants
{
    /**
     * White space characters.<br/>
     * <p>
     * \u0020 - Ascii space.<br/>
     * \u0009 - Ascii tab.<br/>
     * \u000C - Ascii form feed. <br/>
     * \u200B - Zero width space.<br/>
     * \u000A - New line.<br/>
     * \u000D - Carriage return.<br/>
     * </p>
     */
    String WHITE_SPACE_CHARS = "\u0020\u0009\u000C\u200B" + "\n\r";
    
    /**
     * HTML &lt;html@gt; tag name.
     */
    String TAG_HTML = "html";

    /**
     * HTML &lt;head@gt; tag name.
     */
    String TAG_HEAD = "head";

    /**
     * HTML &lt;body@gt; tag name.
     */
    String TAG_BODY = "body";
    
    /**
     * HTML &lt;style&gt; tag name.
     */
    String TAG_STYLE = "style";
    
    /**
     * HTML &lt;script&gt; tag name.
     */
    String TAG_SCRIPT = "script";
    
    /**
     * HTML &lt;a&gt; tag name.
     */
    String TAG_A = "a";
    
    /**
     * HTML &lt;del&gt; tag name.
     */
    String TAG_DEL = "del";

    /**
     * HTML &lt;i&gt; tag name.
     */
    String TAG_I = "i";

    /**
     * HTML &lt;em&gt; tag name.
     */
    String TAG_EM = "em";
    
    /**
     * HTML &lt;dfn&gt; tag name.
     */
    String TAG_DFN = "dfn";
    
    /**
     * HTML &lt;code&gt; tag name.
     */
    String TAG_CODE = "code";
    
    /**
     * HTML &lt;samp&gt; tag name.
     */
    String TAG_SAMP = "samp";
    
    /**
     * HTML &lt;kbd&gt; tag name.
     */
    String TAG_KBD = "kbd";
    
    /**
     * HTML &lt;var&gt; tag name.
     */
    String TAG_VAR = "var";
    
    /**
     * HTML &lt;cite&gt; tag name.
     */
    String TAG_CITE = "cite";
    
    /**
     * HTML &lt;abbr&gt; tag name.
     */
    String TAG_ABBR = "abbr";
    
    /**
     * HTML &lt;acronym&gt; tag name.
     */
    String TAG_ACRONYM = "acronym";
    
    /**
     * HTML &lt;address&gt; tag name.
     */
    String TAG_ADDRESS = "address";
    
    /**
     * HTML &lt;blockquote&gt; tag name.
     */
    String TAG_BLOCKQUOTE = "blockquote";

    /**
     * HTML &lt;q&gt; tag name.
     */
    String TAG_Q = "q";
    
    /**
     * HTML &lt;pre&gt; tag name.
     */
    String TAG_PRE = "pre";
    
    /**
     * HTML &lt;u&gt; tag name.
     */
    String TAG_U = "u";

    /**
     * HTML &lt;s&gt; tag name.
     */
    String TAG_S = "s";

    /**
     * HTML &lt;strike&gt; tag name.
     */
    String TAG_STRIKE = "strike";
    
    /**
     * HTML &lt;ins&gt; tag name.
     */
    String TAG_INS = "ins";

    /**
     * HTML &lt;b&gt; tag name.
     */
    String TAG_B = "b";

    /**
     * HTML &lt;strong&gt; tag name.
     */
    String TAG_STRONG = "strong";

    /**
     * HTML &lt;p&gt; tag name.
     */
    String TAG_P = "p";
    
    /**
     * HTML &lt;div&gt; tag name.
     */
    String TAG_DIV = "div";

    /**
     * HTML &lt;span&gt; tag name.
     */
    String TAG_SPAN = "span";

    /**
     * HTML &lt;center&gt; tag name.
     */
    String TAG_CENTER = "center";

    /**
     * HTML &lt;font&gt; tag name.
     */
    String TAG_FONT = "font";
    
    /**
     * HTML &lt;h1&gt; tag name.
     */
    String TAG_H1 = "h1";

    /**
     * HTML &lt;h2&gt; tag name.
     */
    String TAG_H2 = "h2";

    /**
     * HTML &lt;h3&gt; tag name.
     */
    String TAG_H3 = "h3";

    /**
     * HTML &lt;h4&gt; tag name.
     */
    String TAG_H4 = "h4";

    /**
     * HTML &lt;h5&gt; tag name.
     */
    String TAG_H5 = "h5";

    /**
     * HTML &lt;h6&gt; tag name.
     */
    String TAG_H6 = "h6";
    
    /**
     * HTML &lt;br&gt; tag name.
     */
    String TAG_BR = "br";

    /**
     * HTML &lt;ul&gt; tag name.
     */
    String TAG_UL = "ul";

    /**
     * HTML &lt;ol&gt; tag name.
     */
    String TAG_OL = "ol";
    
    /**
     * HTML &lt;li&gt; tag name.
     */
    String TAG_LI = "li";

    /**
     * HTML &lt;img&gt; tag name.
     */
    String TAG_IMG = "img";

    /**
     * HTML &lt;table&gt; tag name.
     */
    String TAG_TABLE = "table";

    /**
     * HTML &lt;tr&gt; tag name.
     */
    String TAG_TR = "tr";

    /**
     * HTML &lt;td&gt; tag name.
     */
    String TAG_TD = "td";
    
    /**
     * HTML &lt;th&gt; tag name.
     */
    String TAG_TH = "th";

    /**
     * HTML &lt;fieldset&gt; tag name.
     */
    String TAG_FIELDSET = "fieldset";

    /**
     * HTML &lt;form&gt; tag name.
     */
    String TAG_FORM = "form";

    /**
     * HTML &lt;hr&gt; tag name.
     */
    String TAG_HR = "hr";

    /**
     * HTML &lt;noscript&gt; tag name.
     */
    String TAG_NOSCRIPT = "noscript";

    /**
     * HTML &lt;dl&gt; tag name.
     */
    String TAG_DL = "dl";

    /**
     * HTML name attribute name.
     */
    String ATTRIBUTE_NAME = "name";
    
    /**
     * HTML src attribute name.
     */
    String ATTRIBUTE_SRC = "src";
    
    /**
     * HTML alt attribute name.
     */
    String ATTRIBUTE_ALT = "alt";
    
    /**
     * HTML href attribute name.
     */
    String ATTRIBUTE_HREF = "href";
    
    /**
     * HTML style attribute name.
     */
    String ATTRIBUTE_STYLE = TAG_STYLE;
    
    /**
     * HTML class attribute name.
     */
    String ATTRIBUTE_CLASS = "class";
    
    /**
     * HTML align attribute name.
     */
    String ATTRIBUTE_ALIGN = "align";
    
    /**
     * HTML rowspan table attribute.
     */
    String ATTRIBUTE_ROWSPAN = "rowspan";
    
    /**
     * HTML font color attribute.
     */
    String ATTRIBUTE_FONTCOLOR = "color";
    
    /**
     * HTML font face attribute.
     */
    String ATTRIBUTE_FONTFACE = "face";
    
    /**
     * HTML font size attribute.
     */
    String ATTRIBUTE_FONTSIZE = "size";
}
