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
 * Date: 26 nov. 2003
 * Time: 13:53:42
 */
package com.xpn.xwiki;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class XWikiConfig extends Properties {

    public XWikiConfig(String path) throws XWikiException {
        try {
            FileInputStream fis = new FileInputStream(path);
            load(fis);
        }
        catch (FileNotFoundException e) {
            Object[] args = { path };
            throw new XWikiException(XWikiException.MODULE_XWIKI_CONFIG,
                    XWikiException.ERROR_XWIKI_CONFIG_FILENOTFOUND,
                    "Configuration file {0} not found", e, args);
        }
        catch (IOException e) {
            Object[] args = { path };
            throw new XWikiException(XWikiException.MODULE_XWIKI_CONFIG,
                    XWikiException.ERROR_XWIKI_CONFIG_FORMATERROR,
                    "Error reading configuration file {0}", e, args);
        }
    }
}

