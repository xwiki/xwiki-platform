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

package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

import java.util.List;
import java.util.Map;

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

    public String getSeparators() {
        return getStringValue("separators");
    }

    public void setSeparators(String separators) {
        setStringValue("separators", separators);
    }

    public List getList(XWikiContext context) {
        String values = getValues();
        return getListFromString(values);
    }

    public Map getMap(XWikiContext context) {
        String values = getValues();
        return getMapFromString(values);
    }
}
