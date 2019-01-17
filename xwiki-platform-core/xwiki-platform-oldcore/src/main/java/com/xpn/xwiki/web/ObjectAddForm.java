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
package com.xpn.xwiki.web;

import java.util.Map;

import com.xpn.xwiki.util.Util;

/**
 * Note: We store the class name as a String and not a Document Reference since Struts Form beans are called before
 * there's an Execution Context available and before there's a current document in the Execution Context. This bean
 * should be considered only as a helper placeholder for getting parameters from the HTTP Request.
 *
 * @version $Id$
 */
public class ObjectAddForm extends XWikiForm
{
    private String className;

    public String getClassName()
    {
        return this.className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    @Override
    public void readRequest()
    {
        // We need to support both 'classname' and 'className' since an inconsistency has been introduced.
        String className = getRequest().getParameter("classname");
        if (className == null) {
            className = getRequest().getParameter("className");
        }
        setClassName(className);
    }

    public Map<String, String[]> getObject(String prefix)
    {
        return Util.getObject(getRequest(), prefix);
    }
}
