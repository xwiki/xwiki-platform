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
package org.xwiki.xmlrpc.model;

import java.util.List;
import java.util.Map;

import org.codehaus.swizzle.confluence.Page;

/**
 * Extends Page with information about minor version, translations and language.
 * 
 * @version $Id$
 *  
 */
public class XWikiPage extends Page
{
    public XWikiPage()
    {
        super();
    }

    public XWikiPage(Map data)
    {
        super(data);
    }

    public int getMinorVersion()
    {
        return super.getInt("minorVersion");
    }

    public void setMinorVersion(int minorVersion)
    {
        super.setInt("minorVersion", minorVersion);
    }

    public String getLanguage()
    {
        return super.getString("language");
    }

    public void setLanguage(String language)
    {
        setString("language", language);
    }

    public void setTranslations(List<String> translations)
    {
        setList("translations", translations);
    }

    public List<String> getTranslations()
    {
        return getList("translations");
    }

    public String getSyntaxId()
    {
        return super.getString("syntaxId");
    }

    public void setSyntaxId(String syntaxId)
    {
        super.setString("syntaxId", syntaxId);
    }
}
