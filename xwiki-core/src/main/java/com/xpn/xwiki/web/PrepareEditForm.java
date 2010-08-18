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

package com.xpn.xwiki.web;

import com.xpn.xwiki.util.Util;



public class PrepareEditForm extends XWikiForm {
    private String template;
    private String parent;
    private String defaultLanguage;
    private String defaultTemplate;
    private String creator;
    private boolean lockForce;

    public void readRequest() {
        XWikiRequest request = getRequest();
        setTemplate(request.getParameter("template"));
        setParent(request.getParameter("parent"));
        setCreator(request.getParameter("creator"));
        setDefaultLanguage(request.getParameter("defaultLanguage"));
        setDefaultTemplate(request.getParameter("defaultTemplate"));
        setLockForce("1".equals(request.getParameter("force")));
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

    public void setDefaultTemplate(String template) {
        this.defaultTemplate = template;
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
        this.defaultLanguage = Util.normalizeLanguage(defaultLanguage);
    }

    public boolean isLockForce() {
        return lockForce;
    }

    public void setLockForce(boolean lockForce) {
        this.lockForce = lockForce;
    }
}
