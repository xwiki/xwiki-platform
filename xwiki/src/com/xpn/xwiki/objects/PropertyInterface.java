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
 * Time: 11:37:09
 */
package com.xpn.xwiki.objects;

import org.dom4j.Element;
import com.xpn.xwiki.XWikiContext;

public interface PropertyInterface extends ElementInterface {
    public int getId();
    public void setId(int id);
    public BaseCollection getObject();
    public void setObject(BaseCollection object);
    public String toFormString();
    public Element toXML();
}
