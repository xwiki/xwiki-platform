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

public class ParamFilter
{
    /**
     * Encode the parameter to meet a specific browser's requirements, before executing the command.
     * 
     * @param cmd The command whose parameter is encoded
     * @param param The parameter to be encoded
     * @return The result of the encoding of the given parameter
     */
    String encode(Command cmd, String param)
    {
        // no encoding needed by default
        return param;
    }

    /**
     * Decode the value returned by querying the command.
     * 
     * @param cmd The command that has been queried
     * @param value The result of the query
     * @return The decoding of the given value
     */
    String decode(Command cmd, String value)
    {
        // no decoding needed by default
        return value;
    }
}
