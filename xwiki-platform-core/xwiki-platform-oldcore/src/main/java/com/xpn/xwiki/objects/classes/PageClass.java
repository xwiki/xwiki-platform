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

import com.xpn.xwiki.objects.meta.PropertyMetaClass;

/**
 * Page Field Class allows to create a field for storing page references.
 *
 * @version $Id$
 * @since 4.2M2
 */
public class PageClass extends DBListClass
{
    /**
     * Constant defining the field name.
     **/
    protected static final String XCLASSNAME = "page";

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for PageClass.
     *
     * @param wclass MetaClass
     */
    public PageClass(PropertyMetaClass wclass)
    {
        super(XCLASSNAME, "Page", wclass);
    }

    /**
     * Constructor for PageClass.
     */
    public PageClass()
    {
        super();
    }
}
