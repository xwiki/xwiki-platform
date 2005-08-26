/**
 * ===================================================================
 *
 * Copyright (c) 2003-2005 Ludovic Dubost, All rights reserved.
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
 */
package com.xpn.xwiki.test;

import java.io.File;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;

public class StoreObjectRCSFileTest extends AbstractStoreObjectTest {

    private XWiki xwiki;
    private XWikiContext context;

    private void cleanUp() {
        File file = new File(Utils.rcspath + "/" + Utils.web + "/" + Utils.name + ".txt");
        file.delete();
        file = new File(Utils.rcspath + "/" + Utils.web + "/" + Utils.name + ".txt,v");
        file.delete();
        file = new File(Utils.rcspath + "/" + Utils.web + "/" + Utils.name2 + ".txt");
        file.delete();
        file = new File(Utils.rcspath + "/" + Utils.web + "/" + Utils.name2 + ".txt,v");
        file.delete();
    }

    protected void setUp() throws Exception {

        cleanUp();

        XWikiConfig config = new XWikiConfig();
        config.put("xwiki.store.class", "com.xpn.xwiki.store.XWikiRCSFileStore");
        config.put("xwiki.store.rcs.path", Utils.rcspath);
        config.put("xwiki.store.rcs.attachmentpath", Utils.rcsattachmentpath);
        
        this.context = new XWikiContext();
        this.xwiki = new XWiki(config, this.context);
        this.context.setWiki(this.xwiki);
    }

    protected void tearDown() {
        this.xwiki = null;
        this.context = null;
        System.gc();
    }
    
    protected XWikiContext getXWikiContext()
    {
        return this.context;
    }
}
