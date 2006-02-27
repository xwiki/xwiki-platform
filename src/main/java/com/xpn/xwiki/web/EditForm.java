/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
 * @author ludovic
 * @author torcq
 */




package com.xpn.xwiki.web;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class EditForm extends XWikiForm
{

    // ---- Form fields -------------------------------------------------
    private String content;
    private String web;
    private String name;
    private String parent;
    private String creator;
    private String template;
    private String language;
    private String defaultLanguage;
    private String defaultTemplate;
    private String title;
    private String comment;

    public void readRequest() {
        XWikiRequest request = getRequest();
        setContent(request.getParameter("content"));
        setWeb(request.getParameter("web"));
        setName(request.getParameter("name"));
        setParent(request.getParameter("parent"));
        setTemplate(request.getParameter("template"));
        setDefaultTemplate(request.getParameter("default_template"));
        setCreator(request.getParameter("creator"));
        setLanguage(request.getParameter("language"));
        setTitle(request.getParameter("title"));
        setComment(request.getParameter("comment"));
        setDefaultLanguage(request.getParameter("default_language"));
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getObjectNumbers(String prefix) {
        String nb = getRequest().getParameter(prefix + "_nb");
        if ((nb==null)||(nb.equals("")))
         return 0;
        return Integer.parseInt(nb);
    }

    public Map getObject(String prefix) {
        Map map = getRequest().getParameterMap();
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

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

	public String getDefaultTemplate() {
		return defaultTemplate;
	}

	public void setDefaultTemplate(String defaultTemplate) {
		this.defaultTemplate = defaultTemplate;
	}
    
    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }



}

