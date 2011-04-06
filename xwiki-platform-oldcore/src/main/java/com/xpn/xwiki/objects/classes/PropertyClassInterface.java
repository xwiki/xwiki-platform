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
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ObjectInterface;
import com.xpn.xwiki.plugin.query.XWikiCriteria;

public interface PropertyClassInterface extends ObjectInterface {
    public String toString(BaseProperty property);
    public BaseProperty fromString(String value);
    public BaseProperty fromValue(Object value);
    public void displayHidden(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context);
    public void displaySearch(StringBuffer buffer, String name, String prefix, XWikiCriteria criteria, XWikiContext context);
    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context);
    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context);
    public BaseProperty newProperty();
    public void flushCache();
}
