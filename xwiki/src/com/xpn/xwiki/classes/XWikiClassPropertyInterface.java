/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 9 déc. 2003
 * Time: 12:16:53
 */

package com.xpn.xwiki.classes;

import com.xpn.xwiki.XWikiContext;

public interface XWikiClassPropertyInterface extends XWikiObjectInterface {
    public String toString(XWikiObjectProperty property);
    public XWikiObjectProperty fromString(String value);
    public void displayHidden(StringBuffer buffer, String name, XWikiObject object, XWikiContext context);
    public void displaySearch(StringBuffer buffer, String name, XWikiObject object, XWikiContext context);
    public void displayView(StringBuffer buffer, String name, XWikiObject object, XWikiContext context);
    public void displayEdit(StringBuffer buffer, String name, XWikiObject object, XWikiContext context);
}
