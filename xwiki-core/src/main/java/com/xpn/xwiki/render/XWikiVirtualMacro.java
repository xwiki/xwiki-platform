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
package com.xpn.xwiki.render;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XWikiVirtualMacro
{
    private String name;

    private String language;

    private String functionName;

    private boolean multiLine = false;

    private List<String> params = new ArrayList<String>();

    private Map<String, String> paramsTypes = new HashMap<String, String>();

    public XWikiVirtualMacro(String mapping)
    {
        String[] mapping1 = StringUtils.split(mapping, "=");
        this.name = mapping1[0];
        String[] map = StringUtils.split(mapping1[1], ":");
        this.language = map[0];
        this.functionName = map[1];
        if (map.length != 2) {
            String[] aparams = StringUtils.split(map[2], ",");
            if (aparams.length > 0) {
                for (int i = 0; i < aparams.length; i++) {
                    String[] param = StringUtils.split(aparams[i], "|");
                    String pname = param[0];
                    this.params.add(pname);
                    if (param.length > 1) {
                        String ptype = param[1];
                        this.paramsTypes.put(pname, ptype);
                    } else {
                        this.paramsTypes.put(pname, "string");
                    }

                }
            }

            if (map.length == 4) {
                this.multiLine = true;
            }
        }
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getLanguage()
    {
        return this.language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public String getFunctionName()
    {
        return this.functionName;
    }

    public void setFunctionName(String functionName)
    {
        this.functionName = functionName;
    }

    public boolean isSingleLine()
    {
        return !this.multiLine;
    }

    public void setSingleLine(boolean singleLine)
    {
        this.multiLine = !singleLine;
    }

    public boolean isMultiLine()
    {
        return this.multiLine;
    }

    public void setMultiLine(boolean multiLine)
    {
        this.multiLine = !multiLine;
    }

    public List<String> getParams()
    {
        return this.params;
    }

    public void setParams(List<String> params)
    {
        this.params = params;
    }

    public Map<String, String> getParamsTypes()
    {
        return this.paramsTypes;
    }

    public void setParamsTypes(Map<String, String> paramsTypes)
    {
        this.paramsTypes = paramsTypes;
    }
}
