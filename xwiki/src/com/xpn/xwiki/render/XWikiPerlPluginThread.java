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
 * Date: 30 nov. 2003
 * Time: 15:22:10
 */
package com.xpn.xwiki.render;

import org.perl.inline.java.InlineJavaServer;

import java.io.IOException;
import java.net.Socket;

public class XWikiPerlPluginThread extends Thread {

    private int javaServerDebug;
    private String javaServerPort;
    private XWikiPerlPluginRenderer perlRenderer;
    private boolean ready = false;
    private InlineJavaServer javaserver;

    public XWikiPerlPluginThread(XWikiPerlPluginRenderer perlrenderer,String javaserverport, int javaserverdebug ) {
        setJavaServerDebug(javaserverdebug);
        setJavaServerPort(javaserverport);
        setPerlRenderer(perlrenderer);
    }

    public void run() {
        String[] args = { "" + getJavaServerDebug(), getJavaServerPort() ,"true", "false" };
        setReady(true);
        javaserver = new InlineJavaServer(args);
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean checkServer() {
        try {
            Socket s = new Socket("127.0.0.1",Integer.parseInt(getJavaServerPort()));
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }

    public int getJavaServerDebug() {
        return javaServerDebug;
    }

    public void setJavaServerDebug(int javaServerDebug) {
        this.javaServerDebug = javaServerDebug;
    }

    public String getJavaServerPort() {
        return javaServerPort;
    }

    public void setJavaServerPort(String javaServerPort) {
        this.javaServerPort = javaServerPort;
    }

    public XWikiPerlPluginRenderer getPerlRenderer() {
        return perlRenderer;
    }

    public void setPerlRenderer(XWikiPerlPluginRenderer perlRenderer) {
        this.perlRenderer = perlRenderer;
    }
}
