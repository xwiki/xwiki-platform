
import com.xpn.xwiki.store.XWikiRCSFileStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.StoreObjectTest;
import com.xpn.xwiki.test.Utils;

import java.io.File;

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

 * Created by
 * User: Ludovic Dubost
 * Date: 28 janv. 2004
 * Time: 18:22:59
 */

public class StoreObjectRCSFileTest extends StoreObjectTest {
    public void cleanUp() {
        File file = new File(Utils.rcspath + "/" + Utils.web + "/" + Utils.name + ".txt");
        file.delete();
        file = new File(Utils.rcspath + "/" + Utils.web + "/" + Utils.name + ".txt,v");
        file.delete();
        file = new File(Utils.rcspath + "/" + Utils.web + "/" + Utils.name2 + ".txt");
        file.delete();
        file = new File(Utils.rcspath + "/" + Utils.web + "/" + Utils.name2 + ".txt,v");
        file.delete();
    }

    public void setUp() {
        cleanUp();
    }

    public XWikiStoreInterface getStore() {
        XWikiStoreInterface store = new XWikiRCSFileStore(Utils.rcspath);
        return store;
    }
}
