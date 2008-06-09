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

package com.xpn.xwiki.web;

import com.xpn.xwiki.doc.XWikiDocument;

public class EditBean extends Object
{
    // ---- Fields ------------------------------------------------------
    private XWikiDocument xWikiDoc;
    

    // ---- Accessor Methods --------------------------------------------
    
    public XWikiDocument getXWikiDoc()
    {
        return this.xWikiDoc;
    }
    
    public void setXWikiDoc(XWikiDocument xWikiDoc)
    {
        this.xWikiDoc = xWikiDoc;
    }

    /*
    public String[] getLanguages()
    {
        return languages;
    }
    
    public void setLanguages(String[] languages)
    {
        this.languages = languages;   
    }

    // Convenience method to simplify repopulation of select lists
    public Properties getLanguagesAsMap()
    {
        Properties p = new Properties();
        if (languages != null)
        {
            for (int i = 0; i < languages.length; i++)
                p.setProperty((String)languages[i], "SELECTED");
        }            
        return p;
    } 
    */
}




