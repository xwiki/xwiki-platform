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
package com.xpn.xwiki.web;

/**
 * Add a backward compatibility layer to the {@link XWikiServletRequest} class.
 * 
 * @version $Id: UtilsCompatibilityAspect.aj 10166 2008-06-09 12:50:40Z sdumitriu $
 */
public privileged aspect XWikiServletRequestCompatibilityAspect
{
    /**
     * Turn Windows CP1252 Characters to iso-8859-1 characters when possible, HTML entities when needed. This filtering
     * works on Tomcat (not Jetty).
     * 
     * @param text The text to filter
     * @return filtered text
     */
    public String XWikiServletRequest.filterString(String text)
    {
        if (text == null) {
            return null;
        }

        // In case we are running in ISO we need to take care or some windows-1252 characters
        // that are commonly copy pasted by users from web sites or desktop applications
        // If we don't transform these characters then some databases running in the latin charset mode
        // will drop the characters and will we only see that on server restart.
        // This happens for example using MySQL both with tomcat and Jetty
        // See bug : http://jira.xwiki.org/jira/browse/XWIKI-2422
        // Source : http://www.microsoft.com/typography/unicode/1252.htm
        if (this.request.getCharacterEncoding().startsWith("ISO-8859")) {
            // EURO SIGN
            text = text.replaceAll("\u0080", "&euro;");
            // SINGLE LOW-9 QUOTATION MARK
            text = text.replaceAll("\u0082", "&sbquo;");
            // LATIN SMALL LETTER F WITH HOOK
            text = text.replaceAll("\u0083", "&fnof;");
            // DOUBLE LOW-9 QUOTATION MARK
            text = text.replaceAll("\u0084", "&bdquo;");
            // HORIZONTAL ELLIPSIS, entity : &hellip;
            text = text.replaceAll("\u0085", "...");
            // DAGGER
            text = text.replaceAll("\u0086", "&dagger;");
            // DOUBLE DAGGER
            text = text.replaceAll("\u0087", "&Dagger;");
            // MODIFIER LETTER CIRCUMFLEX ACCENT
            text = text.replaceAll("\u0088", "&circ;");
            // PER MILLE SIGN
            text = text.replaceAll("\u0089", "&permil;");
            // LATIN CAPITAL LETTER S WITH CARON
            text = text.replaceAll("\u008a", "&Scaron;");
            // SINGLE LEFT-POINTING ANGLE QUOTATION MARK, entity : &lsaquo;
            text = text.replaceAll("\u008b", "'");
            // LATIN CAPITAL LIGATURE OE
            text = text.replaceAll("\u008c", "&OElig;");
            // LATIN CAPITAL LETTER Z WITH CARON
            text = text.replaceAll("\u008e", "&#381;");
            // LEFT SINGLE QUOTATION MARK, entity : &lsquo;
            text = text.replaceAll("\u0091", "'");
            // RIGHT SINGLE QUOTATION MARK, entity : &rsquo;
            text = text.replaceAll("\u0092", "'");
            // LEFT DOUBLE QUOTATION MARK, entity : &ldquo;
            text = text.replaceAll("\u0093", "\"");
            // RIGHT DOUBLE QUOTATION MARK, entity : &rdquo;
            text = text.replaceAll("\u0094", "\"");
            // BULLET
            text = text.replaceAll("\u0095", "&bull;");
            // EN DASH, entity : &ndash;
            text = text.replaceAll("\u0096", "-");
            // EM DASH, entity : &mdash;
            text = text.replaceAll("\u0097", "-");
            // SMALL TILDE
            text = text.replaceAll("\u0098", "&tilde;");
            // TRADE MARK SIGN
            text = text.replaceAll("\u0099", "&trade;");
            // LATIN SMALL LETTER S WITH CARON
            text = text.replaceAll("\u009a", "&scaron;");
            // SINGLE RIGHT-POINTING ANGLE QUOTATION MARK, entity : &rsaquo;
            text = text.replaceAll("\u009b", "'");
            // LATIN SMALL LIGATURE OE
            text = text.replaceAll("\u009c", "&oelig;");
            // LATIN SMALL LETTER Z WITH CARON
            text = text.replaceAll("\u009e", "&#382;");
            // LATIN CAPITAL LETTER Y WITH DIAERESIS
            text = text.replaceAll("\u009f", "&Yuml;");
        }
        return text;
    }

    public String[] XWikiServletRequest.filterStringArray(String[] text)
    {
        if (text == null) {
            return null;
        }

        for (int i = 0; i < text.length; i++) {
            text[i] = filterString(text[i]);
        }
        return text;
    }
}
