/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 25 nov. 2003
 * Time: 21:20:04
 */



package com.xpn.xwiki.web;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class EditForm extends ActionForm
{

    // ---- Form fields -------------------------------------------------
    private String content;
    private String web;
    private String name;
    private String parent;
    private String template;

    private HttpServletRequest request;
    /**
     * Reset all properties to their default values.
     *
     * @param mapping The mapping used to select this instance
     * @param request The servlet request we are processing
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) 
    {
        setContent(request.getParameter("content"));
        setWeb(request.getParameter("web"));
        setName(request.getParameter("name"));
        setParent(request.getParameter("parent"));
        this.request = request;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getWeb() {
        return web;
    }

    public void setWeb(String web) {
        this.web = web;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getObjectNumbers(String prefix) {
        String nb = request.getParameter(prefix + "_nb");
        if ((nb==null)||(nb.equals("")))
         return 0;
        return Integer.parseInt(nb);
    }

    public Map getObject(String prefix) {
        Map map = request.getParameterMap();
        HashMap map2 = new HashMap();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
          String name = (String) it.next();
          if (name.startsWith(prefix + "_")) {
              String newname = name.substring(prefix.length()+1);
              map2.put(newname, map.get(name));
          }
        }
        return map2;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }
}


