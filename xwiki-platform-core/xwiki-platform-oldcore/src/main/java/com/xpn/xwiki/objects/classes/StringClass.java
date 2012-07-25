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
package com.xpn.xwiki.objects.classes;

import org.apache.ecs.xhtml.input;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

public class StringClass extends PropertyClass
{
    private static final String XCLASSNAME = "string";

    public StringClass(String name, String prettyname, PropertyMetaClass wclass)
    {
        super(name, prettyname, wclass);
        setSize(30);
    }

    public StringClass(PropertyMetaClass wclass)
    {
        this(XCLASSNAME, "String", wclass);
    }

    public StringClass()
    {
        this(null);
    }

    public int getSize()
    {
        return getIntValue("size");
    }

    public void setSize(int size)
    {
        setIntValue("size", size);
    }

    public boolean isPicker()
    {
        return (getIntValue("picker") == 1);
    }

    public void setPicker(boolean picker)
    {
        setIntValue("picker", picker ? 1 : 0);
    }

    /*
     * Returns the current default custom displayer for the PropertyClass
     * When it cannot find one for the current class it will call the same
     * function for the super class
     * This function should be implemented by any derivative PropertyClass
     * if this PropertyClass wants to have a default custom displayer
     */
    @Override
    public String getDefaultCustomDisplayer(XWikiContext context) {
        String customDisplayer = getDefaultCustomDisplayer(XCLASSNAME, context);
        if (customDisplayer==null)
            return super.getDefaultCustomDisplayer(context);
        else
            return customDisplayer;
    }

    @Override
    public BaseProperty fromString(String value)
    {
        BaseProperty property = newProperty();
        property.setValue(value);
        return property;
    }

    @Override
    public BaseProperty newProperty()
    {
        BaseProperty property = new StringProperty();
        property.setName(getName());
        return property;
    }

    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context)
    {
        input input = new input();
        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop != null) {
            input.setValue(prop.toFormString());
        }

        input.setType("text");
        input.setName(prefix + name);
        input.setID(prefix + name);
        input.setSize(getSize());
        input.setDisabled(isDisabled());

        if (isPicker()) {
            input.setClass("suggested");
            String path = "";
            XWiki xwiki = context.getWiki();
            path = xwiki.getURL("Main.WebHome", "view", context);

            String classname = this.getObject().getName();
            String fieldname = this.getName();
            String secondCol = "-", firstCol = "-";

            String script =
                "\"" + path + "?xpage=suggest&amp;classname=" + classname + "&amp;fieldname=" + fieldname
                    + "&amp;firCol=" + firstCol + "&amp;secCol=" + secondCol + "&amp;\"";
            String varname = "\"input\"";
            input.setOnFocus("new ajaxSuggest(this, {script:" + script + ", varname:" + varname + "} )");
        }

        buffer.append(input.toString());
    }

}
