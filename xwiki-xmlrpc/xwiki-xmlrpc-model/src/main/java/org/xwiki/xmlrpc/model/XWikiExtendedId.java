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
package org.xwiki.xmlrpc.model;

import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class XWikiExtendedId
{
    private String basePageId;

    private Map<String, String> parametersMap;

    public XWikiExtendedId(String string)
    {
        if (string.indexOf(".") == -1) {
            throw new IllegalArgumentException(
                "An XWiki id must be in the form Space.Page[?param=value&param=value&...]");
        }

        parametersMap = new HashMap<String, String>();

        int queryStringSeparatorIndex = string.indexOf('?');
        if (queryStringSeparatorIndex != -1) {
            basePageId = string.substring(0, queryStringSeparatorIndex);
            String queryString = string.substring(queryStringSeparatorIndex + 1, string.length());

            if (!queryString.equals("")) {
                String[] parameters = queryString.split("&");
                for (String parameter : parameters) {
                    String[] components = parameter.split("=", 2);
                    if (components.length == 2) {
                        parametersMap.put(components[0], components[1]);
                    } else {
                        parametersMap.put(components[0], "");
                    }
                }
            }
        } else {
            basePageId = string;
        }
    }

    public String getBasePageId()
    {
        return basePageId;
    }

    public Set<String> getParameters()
    {
        return parametersMap.keySet();
    }

    public String getParameter(String key)
    {
        return parametersMap.get(key);
    }

    public void setParameter(String key, String value)
    {
        if (value == null) {
            parametersMap.remove(key);
        } else {
            parametersMap.put(key, value);
        }
    }

    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        Formatter f = new Formatter(sb);

        f.format("%s", basePageId);
        if (parametersMap.size() != 0) {
            f.format("?");
            String[] keys = parametersMap.keySet().toArray(new String[0]);
            for (int i = 0; i < keys.length; i++) {
                if (i != (keys.length - 1)) {
                    f.format("%s=%s&", keys[i], parametersMap.get(keys[i]));
                } else {
                    f.format("%s=%s", keys[i], parametersMap.get(keys[i]));
                }
            }
        }

        return sb.toString();
    }
}
