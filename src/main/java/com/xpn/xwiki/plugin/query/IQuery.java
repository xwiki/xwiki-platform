/**
 * ===================================================================
 *
 * Copyright (c) 2005 Artem Melentev, All rights reserved.
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
 * User: Artem Melentev
 */
package com.xpn.xwiki.plugin.query;

import java.util.List;

import com.xpn.xwiki.XWikiException;

/** XWiki Query interface */
public interface IQuery {
	public List list() throws XWikiException;
	// TODO: setParameter ? how parameter names parse?
	public IQuery setMaxResults(int fs);
	public IQuery setFirstResult(int fr);
	public IQuery setDistinct(boolean d);
}
