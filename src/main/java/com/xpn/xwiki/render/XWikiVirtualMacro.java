/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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

public class XWikiVirtualMacro {
    private String name;
    private String language;
    private String functionName;
    private boolean multiLine = false;
    private List params = new ArrayList();
    private HashMap paramsTypes = new HashMap();

    public XWikiVirtualMacro(String mapping) {
        String[] mapping1 = StringUtils.split(mapping, "=");
        name = mapping1[0];
        String[] map = StringUtils.split(mapping1[1], ":");
        language = map[0];
        functionName = map[1];
        if (map.length!=2) {
        String[] aparams = StringUtils.split(map[2], ",");
        if (aparams.length>0) {
            for (int i=0;i<aparams.length;i++) {
                String[] param = StringUtils.split(aparams[i], "|");
                String pname = param[0];
                params.add(pname);
                if (param.length>1) {
                    String ptype = param[1];
                    paramsTypes.put(pname, ptype);
                } else {
                    paramsTypes.put(pname, "string");
                }

            }
        }

        if (map.length==4)
            multiLine = true;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public boolean isSingleLine() {
        return !multiLine;
    }

    public void setSingleLine(boolean singleLine) {
        this.multiLine = !singleLine;
    }

    public boolean isMultiLine() {
        return multiLine;
    }

    public void setMultiLine(boolean multiLine) {
        this.multiLine = !multiLine;
    }

    public List getParams() {
        return params;
    }

    public void setParams(List params) {
        this.params = params;
    }

    public HashMap getParamsTypes() {
        return paramsTypes;
    }

    public void setParamsTypes(HashMap paramsTypes) {
        this.paramsTypes = paramsTypes;
    }
}
