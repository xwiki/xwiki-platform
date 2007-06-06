/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.it.framework;

import junit.extensions.TestSetup;
import junit.framework.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;

public class XWikiTestSetup extends TestSetup
{
    private static final String EXECUTION_DIRECTORY = System.getProperty("xwikiExecutionDirectory");
    private static final String START_COMMAND = System.getProperty("xwikiExecutionStartCommand");
    private static final String STOP_COMMAND = System.getProperty("xwikiExecutionStopCommand"); 

    public XWikiTestSetup(Test test)
    {
        super(test);
    }

    protected void setUp() throws Exception
    {
        // Start XWiki
        Runtime.getRuntime().exec(START_COMMAND, null, new File(EXECUTION_DIRECTORY));
    
        // Wait till the main page becomes available which means the server is started fine
        URL url = new URL("http://localhost:8080/xwiki/bin/view/Main/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        boolean connected = false;
        while (!connected) {
            try {
                connection.connect();
                connected = (connection.getResponseCode() == HttpURLConnection.HTTP_OK);
            } catch (IOException e) {
                // Do nothing as it simply means the server is not ready yet...
            }
            Thread.sleep(100L);
        }
    }

    protected void tearDown() throws Exception
    {
        // Stop XWiki
        Runtime.getRuntime().exec(STOP_COMMAND, null, new File(EXECUTION_DIRECTORY));
    }
}
