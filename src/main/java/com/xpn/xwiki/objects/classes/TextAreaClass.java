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
 * Date: 24 janv. 2004
 * Time: 10:29:38
 */
package com.xpn.xwiki.objects.classes;

import org.apache.ecs.xhtml.textarea;
import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

public class TextAreaClass extends StringClass {

    public TextAreaClass(PropertyMetaClass wclass) {
        super("textarea", "Text Area", wclass);
        setSize(40);
        setRows(5);
    }

    public TextAreaClass() {
        this(null);
    }

    public BaseProperty newProperty() {
        BaseProperty property = new LargeStringProperty();
        property.setName(getName());
        return property;
    }

    public int getRows() {
        return getIntValue("rows");
    }

    public void setRows(int rows) {
        setIntValue("rows", rows);
    }

    public String getEditor() {
        return getStringValue("editor").toLowerCase();
    }

    public boolean isWysiwyg(XWikiContext context) {
        String editor = context.getRequest().get("xeditmode");
        if (editor!=null) {
            if (editor.equals("text"))
                return false;
            if (editor.equals("wysiwyg"))
                return true;
        }

        editor = getEditor();

        if (editor!=null) {
            if (editor.equals("text"))
                return false;
            if (editor.equals("wysiwyg"))
                return true;
        }

        editor = context.getWiki().getEditorPreference(context);

        if (editor!=null) {
            if (editor.equals("text"))
                return false;
            if (editor.equals("wysiwyg"))
                return true;
        }

        return false;
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        boolean isWysiwyg = isWysiwyg(context);
        textarea textarea = new textarea();
        String tname = prefix + name;
        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop!=null) textarea.addElement(prop.toFormString());

        textarea.setName(tname);
        textarea.setID(tname);
        textarea.setCols(getSize());
        textarea.setRows(getRows() + (isWysiwyg ? 2 : 0));


        // Let's add the Wysiwyg JS
        if (isWysiwyg) {
            String wysiwyg = (String) context.get("editor_wysiwyg");
            if (wysiwyg==null) {
                wysiwyg = tname;
            } else {
                wysiwyg += "," + tname;
            }
            context.put("editor_wysiwyg", wysiwyg);
        } else {
            VelocityContext vcontext = (VelocityContext) context.get("vcontext");
            vcontext.put("textareaName", tname);
            vcontext.put("textarea", textarea);
            String addscript = context.getWiki().parseTemplate("textarea_text.vm", context);
            buffer.append(addscript);
        }
        buffer.append(textarea.toString());
    }
}
