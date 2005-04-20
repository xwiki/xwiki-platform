/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 30 déc. 2003
 * Time: 09:12:48
 */
package com.xpn.xwiki.web;



public class PrepareEditForm extends XWikiForm {
    private String formTemplate;
    private String template;
    private String parent;
    private String defaultLanguage;
    private String creator;
    private boolean lockForce;

    public void readRequest() {
        XWikiRequest request = getRequest();
        setFormTemplate(request.getParameter("formtemplate"));
        setTemplate(request.getParameter("template"));
        setParent(request.getParameter("parent"));
        setCreator(request.getParameter("creator"));
        setDefaultLanguage(request.getParameter("defaultLanguage"));
        setLockForce("1".equals(request.getParameter("lockforce")));
    }

    public String getFormTemplate() {
        return formTemplate;
    }

    public void setFormTemplate(String formTemplate) {
        this.formTemplate = formTemplate;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
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

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public boolean isLockForce() {
        return lockForce;
    }

    public void setLockForce(boolean lockForce) {
        this.lockForce = lockForce;
    }
}
