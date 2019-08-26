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
package org.xwiki.container.servlet.internal;

import org.apache.commons.lang3.StringUtils;

/**
 * Parser for RFC 7239, section 4: Forwarded.
 * 
 * @version $Id$
 * @since 10.7RC1
 */
public class ForwardedHeader
{
    private String host;

    private String proto;

    /**
     * @param str the value of the header
     */
    public ForwardedHeader(String str)
    {
        String[] elements = StringUtils.split(str, ';');

        for (String element : elements) {
            int index = element.indexOf('=');
            if (index != -1) {
                String elementName = element.substring(0, index).trim().toLowerCase();

                String value = element.substring(index + 1).trim();
                int comaIndex = value.indexOf(',');
                if (comaIndex != -1) {
                    value = value.substring(0, comaIndex).trim();
                }

                switch (elementName) {
                    case "host":
                        this.host = value;
                        break;

                    case "proto":
                        this.proto = value;
                        break;

                    default:
                        // TODO: support other stuff
                        break;
                }
            }
        }
    }

    /**
     * @return the initial host used by the client
     */
    public String getHost()
    {
        return this.host;
    }

    /**
     * @return the initial protocol used by the client
     */
    public String getProto()
    {
        return this.proto;
    }
}
