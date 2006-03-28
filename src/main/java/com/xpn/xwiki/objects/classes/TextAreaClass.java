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
 * @author sdumitriu
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
        String editor = null;
        if ((context!=null)&&(context.getRequest()!=null))
            editor = context.getRequest().get("xeditmode");

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

        if ((context!=null)&&(context.getWiki()!=null))
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
            if (vcontext!=null) {
             vcontext.put("textareaName", tname);
             vcontext.put("textarea", textarea);
             String addscript = context.getWiki().parseTemplate("textarea_text.vm", context);
             buffer.append(addscript);
            }
        }
        buffer.append(textarea.toString());
    }
}
