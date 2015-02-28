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
package com.xpn.xwiki.web;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;

import com.xpn.xwiki.util.Util;

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

    private boolean isMinorEdit = false;

    private String tags;

    private boolean lockForce;

    private String syntaxId;

    private String hidden;

    @Override
    public void readRequest()
    {
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
        setDefaultLanguage(request.getParameter("defaultLanguage"));
        setTags(request.getParameterValues("tags"));
        setLockForce("1".equals(request.getParameter("force")));
        setMinorEdit(request.getParameter("minorEdit") != null);
        setSyntaxId(request.getParameter("syntaxId"));
        setHidden(request.getParameter("xhidden"));
    }

    public void setTags(String[] parameter)
    {
        if (parameter == null) {
            this.tags = null;
            return;
        }
        StringBuilder tags = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < parameter.length; ++i) {
            if (!parameter[i].equals("")) {
                if (first) {
                    first = false;
                } else {
                    tags.append("|");
                }
                tags.append(parameter[i]);
            }
        }
        this.tags = tags.toString();
    }

    public String getTags()
    {
        return this.tags;
    }

    public String getContent()
    {
        return this.content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getWeb()
    {
        return this.web;
    }

    public void setWeb(String web)
    {
        this.web = web;
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
        this.language = Util.normalizeLanguage(language);
    }

    public int getObjectNumbers(String prefix)
    {
        String nb = getRequest().getParameter(prefix + "_nb");
        return NumberUtils.toInt(nb);
    }

    public Map<String, String[]> getObject(String prefix)
    {
        @SuppressWarnings("unchecked")
        Map<String, String[]> allParameters = getRequest().getParameterMap();
        Map<String, String[]> result = new HashMap<String, String[]>();
        for (String name : allParameters.keySet()) {
            if (name.startsWith(prefix + "_")) {
                String newname = name.substring(prefix.length() + 1);
                result.put(newname, allParameters.get(name));
            }
        }
        return result;
    }

    public String getParent()
    {
        return this.parent;
    }

    public void setParent(String parent)
    {
        this.parent = parent;
    }

    public String getCreator()
    {
        return this.creator;
    }

    public void setCreator(String creator)
    {
        this.creator = creator;
    }

    public String getTemplate()
    {
        return this.template;
    }

    public void setTemplate(String template)
    {
        this.template = template;
    }

    public String getDefaultTemplate()
    {
        return this.defaultTemplate;
    }

    public void setDefaultTemplate(String defaultTemplate)
    {
        this.defaultTemplate = defaultTemplate;
    }

    public String getDefaultLanguage()
    {
        return this.defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage)
    {
        this.defaultLanguage = Util.normalizeLanguage(defaultLanguage);
    }

    public String getTitle()
    {
        return this.title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getComment()
    {
        return this.comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public boolean isMinorEdit()
    {
        return this.isMinorEdit;
    }

    public void setMinorEdit(boolean isMinorEdit)
    {
        this.isMinorEdit = isMinorEdit;
    }

    public boolean isLockForce()
    {
        return this.lockForce;
    }

    public void setLockForce(boolean lockForce)
    {
        this.lockForce = lockForce;
    }

    public String getSyntaxId()
    {
        return this.syntaxId;
    }

    public void setSyntaxId(String syntaxId)
    {
        this.syntaxId = syntaxId;
    }

    public String getHidden()
    {
        return this.hidden;
    }

    public void setHidden(String hidden)
    {
        this.hidden = hidden;
    }

}
