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
 * Date: 24 nov. 2003
 * Time: 18:19:44
 */
package com.xpn.xwiki.test;

import junit.framework.TestCase;
import java.io.*;
import java.util.*;
import com.xpn.xwiki.util.Util;


public class UtilTest extends TestCase {

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testTopicInfo() throws IOException {
        String topicinfo;
        Hashtable params;

        topicinfo = "author=\"ludovic\"";
        params = Util.keyValueToHashtable(topicinfo);
        assertTrue(params.get("author").equals("ludovic"));
        topicinfo = "author=\"ludovic\" date=\"1026671586\" format=\"1.0beta2\" version=\"1.1\"";
        params = Util.keyValueToHashtable(topicinfo);
        assertTrue(params.get("author").equals("ludovic"));
        assertTrue(params.get("date").equals("1026671586"));
        assertTrue(params.get("format").equals("1.0beta2"));
        assertTrue(params.get("version").equals("1.1"));
        topicinfo = "author=\"Ludovic Dubost\" format=\"1.0 beta\" version=\"1.2\"";
        params = Util.keyValueToHashtable(topicinfo);
        assertTrue(params.get("author").equals("Ludovic Dubost"));
        assertTrue(params.get("format").equals("1.0 beta"));
        assertTrue(params.get("version").equals("1.2"));
        topicinfo = "test=\"%_Q_%Toto%_Q_%\"";
        params = Util.keyValueToHashtable(topicinfo);
        assertTrue(params.get("test").equals("\"Toto\""));
        topicinfo = "test=\"Ludovic%_N_%Dubost\"";
        params = Util.keyValueToHashtable(topicinfo);
        assertTrue(params.get("test").equals("Ludovic\nDubost"));
        topicinfo = "   test=\"Ludovic%_N_%Dubost\"   ";
        params = Util.keyValueToHashtable(topicinfo);
        assertTrue(params.get("test").equals("Ludovic\nDubost"));
    }
}
