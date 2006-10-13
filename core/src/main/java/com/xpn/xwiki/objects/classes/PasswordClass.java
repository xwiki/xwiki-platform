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
 * @author sdumitriu
 */

package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ElementInterface;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import org.apache.ecs.xhtml.input;

public class PasswordClass extends StringClass {
    public PasswordClass(PropertyMetaClass wclass) {
        super("password", "Password", wclass);
        setxWikiClass(wclass);
    }

    public PasswordClass() {
        this(null);
    }

    public BaseProperty fromString(String value) {
        if (value.equals("********"))
         return null;
        else
         return super.fromString(value);
    }

    public void displayHidden(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        // Passwords cannot go through the preview interface of we don't do something here..
    }

    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        ElementInterface prop = object.safeget(name);
        if (prop!=null)
         buffer.append("********");
    }

   public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        input input = new input();
        ElementInterface prop = object.safeget(name);
        if (prop!=null) input.setValue("********");

        input.setType("password");
        input.setName(prefix + name);
        input.setSize(getSize());
        buffer.append(input.toString());
    }
}