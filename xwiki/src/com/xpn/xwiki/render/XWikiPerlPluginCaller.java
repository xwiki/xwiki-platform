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
 * Time: 13:52:39
 */

package com.xpn.xwiki.render;

import org.perl.inline.java.* ;

public class XWikiPerlPluginCaller extends InlineJavaPerlCaller {

    public XWikiPerlPluginCaller() throws InlineJavaException {
      super();
      XWikiPerlPluginRenderer.setPerlCaller("7890", this);
     }

    public XWikiPerlPluginCaller(String port) throws InlineJavaException {
      super();
      XWikiPerlPluginRenderer.setPerlCaller(port, this);
     }

    public void StartCallbackLoop() throws InlineJavaException, InlineJavaPerlException {
        super.StartCallbackLoop();
    }
}

