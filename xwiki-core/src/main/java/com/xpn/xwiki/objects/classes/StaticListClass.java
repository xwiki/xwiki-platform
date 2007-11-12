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

package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.XWiki;

import java.util.List;
import java.util.Map;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;

import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import org.apache.ecs.xhtml.input;

public class StaticListClass extends ListClass {

    public StaticListClass(PropertyMetaClass wclass) {
        super("staticlist", "Static List", wclass);
        setSeparators(" ,|");
    }

    public StaticListClass() {
        this(null);
    }

    public String getValues() {
        return getStringValue("values");
    }

    public void setValues(String values) {
        setStringValue("values", values);
    }

    public List getList(XWikiContext context) {
        String values = getValues();
        return getListFromString(values);
    }

    public Map getMap(XWikiContext context) {
        String values = getValues();
        return getMapFromString(values);
    }
    
    public void displayEdit(StringBuffer buffer, String name, String prefix,
        BaseCollection object, XWikiContext context)
    {
        if (getDisplayType().equals("input")) {
            input input = new input();
            BaseProperty prop = (BaseProperty) object.safeget(name);
            if (prop != null) {
                input.setValue(prop.toFormString());
            }
            input.setType("text");
            input.setSize(getSize());
            input.setName(prefix + name);
            input.setID(prefix + name);
            
            if(isPicker()) {
            	input.addAttribute("autocomplete", "off");
            	String path = "";
            	try {
               	 	XWiki xwiki = context.getWiki();
               	 	path = xwiki.getURL("Main.WebHome", "view", context);
            	} catch(XWikiException e) {
            		e.printStackTrace();
            	}
            	
            	String classname = this.getObject().getName();
           	 	String fieldname = this.getName();
           	 	String secondCol = "-", firstCol = "-";
           	 	
           	 	String script = "\""+path+"?xpage=suggest&amp;classname="+classname+"&amp;fieldname="+fieldname+"&amp;firCol="+firstCol+"&amp;secCol="+secondCol+"&amp;\"";
        	 	String varname = "\"input\"";
        	 	String seps = "\""+this.getSeparators()+"\"";
        	 	if(isMultiSelect())
        	 		input.setOnFocus("new ajaxSuggest(this, {script:"+script+", varname:"+varname+", seps:"+seps+"} )");
        	 	else
        	 		input.setOnFocus("new ajaxSuggest(this, {script:"+script+", varname:"+varname+"} )");
            }
            
            buffer.append(input.toString());
            
        } else if (getDisplayType().equals("radio") || getDisplayType().equals("checkbox")) {
            displayRadioEdit(buffer, name, prefix, object, context);
        } else {
            displaySelectEdit(buffer, name, prefix, object, context);
        }
    		
        if (!getDisplayType().equals("input")) {
            org.apache.ecs.xhtml.input hidden = new input(input.hidden, prefix + name, "");
            buffer.append(hidden);
        }
    }
}
