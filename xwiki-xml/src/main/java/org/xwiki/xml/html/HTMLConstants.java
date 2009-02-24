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
 * HTML Constants used by the HTML Cleaner.
 *
 * @version $Id: $
 * @since 1.6M2
 */
public interface HTMLConstants
{
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
     * HTML style attribute name.
     */
    String ATTRIBUTE_STYLE = "style";
    
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
