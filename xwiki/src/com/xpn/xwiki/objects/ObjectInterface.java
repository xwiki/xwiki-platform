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
 * Time: 11:57:54
 */
package com.xpn.xwiki.objects;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.BaseClass;

public interface ObjectInterface extends ElementInterface {
    public BaseClass getxWikiClass();
    public void setxWikiClass(BaseClass xWikiClass);

    public PropertyInterface get(String name) throws XWikiException;
    void put(String name,PropertyInterface property) throws XWikiException;
    public PropertyInterface safeget(String name);
    void safeput(String name, PropertyInterface property);
}
