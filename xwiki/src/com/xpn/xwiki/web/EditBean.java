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
 * Date: 25 nov. 2003
 * Time: 21:20:04
 */



package com.xpn.xwiki.web;

import com.xpn.xwiki.doc.*;

import java.util.Properties;



public class EditBean extends Object
{
    // ---- Fields ------------------------------------------------------
    private XWikiDocInterface xWikiDoc;
    

    // ---- Accessor Methods --------------------------------------------
    
    public XWikiDocInterface getXWikiDoc()
    {
        return this.xWikiDoc;
    }
    
    public void setXWikiDoc(XWikiDocInterface xWikiDoc)
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




