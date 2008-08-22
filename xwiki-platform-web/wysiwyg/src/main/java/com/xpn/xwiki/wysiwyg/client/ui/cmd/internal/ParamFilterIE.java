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
package com.xpn.xwiki.wysiwyg.client.ui.cmd.internal;

import com.xpn.xwiki.wysiwyg.client.ui.cmd.Command;

public class ParamFilterIE extends ParamFilter
{
    /**
     * {@inheritDoc}
     * 
     * @see ParamFilterImpl#filter(Command, String)
     */
    public String encode(Command cmd, String param)
    {
        if (cmd == Command.FORMAT_BLOCK) {
            return encodeFormatBlock(param);
        } else {
            return super.encode(cmd, param);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ParamFilter#decode(Command, String)
     */
    public String decode(Command cmd, String value)
    {
        if (cmd == Command.FORMAT_BLOCK) {
            return decodeFormatBlock(value);
        } else {
            return super.decode(cmd, value);
        }
    }

    /**
     * Internet Explorer supports only heading tags H1 - H6, ADDRESS, and PRE, which must also include the tag
     * delimiters &lt; &gt;, such as "&lt;H1&gt;".
     */
    private String encodeFormatBlock(String tagName)
    {
        if (tagName != null && tagName.length() > 0) {
            if (tagName.charAt(0) != '<') {
                tagName = "<" + tagName;
            }
            if (tagName.charAt(tagName.length() - 1) != '>') {
                tagName += '>';
            }
        }
        return tagName;
    }

    /**
     * @see #encodeFormatBlock(String)
     */
    protected String decodeFormatBlock(String tagName)
    {
        if (tagName != null && tagName.length() > 0) {
            if (tagName.charAt(0) == '<') {
                tagName = tagName.substring(1);
            }
            if (tagName.charAt(tagName.length() - 1) == '>') {
                tagName = tagName.substring(0, tagName.length() - 1);
            }
        }
        return tagName;
    }
}
