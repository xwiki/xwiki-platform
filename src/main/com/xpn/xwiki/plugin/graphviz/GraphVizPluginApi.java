/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
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
 * Date: 23 avr. 2005
 * Time: 00:57:33
 */
package com.xpn.xwiki.plugin.graphviz;

import com.amazon.api.alexa.*;
import com.amazon.api.alexa.holders.OperationRequestHolder;
import com.amazon.api.alexa.holders.UrlInfoResultHolder;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.alexa.AlexaPlugin;
import com.xpn.xwiki.api.Api;

import javax.xml.rpc.ServiceException;
import java.rmi.RemoteException;
import java.io.*;

public class GraphVizPluginApi extends Api {
    private GraphVizPlugin plugin;

    public GraphVizPluginApi(GraphVizPlugin plugin, XWikiContext context) {
            super(context);
            setPlugin(plugin);
        }

    public GraphVizPlugin getPlugin() {
        return plugin;
    }

    public void setPlugin(GraphVizPlugin plugin) {
        this.plugin = plugin;
    }

    public byte[] getDotImage(String dot) throws IOException {
        return plugin.getDotImage(dot, "gif");
    }

    public byte[] getDotImage(String dot, String extension) throws IOException {
        return plugin.getDotImage(dot, extension);
    }

    public String getDotImageURL(String dot) throws IOException {
        return plugin.getDotImageURL(dot, context);
    }

    public String writeDotImage(String dot) throws IOException {
        return plugin.writeDotImage(dot, "gif");
    }

    public String writeDotImage(String dot, String extension) throws IOException {
        return plugin.writeDotImage(dot, extension);
    }

    public void outputDotImage(String dot) throws IOException {
        plugin.outputDotImage(dot, "gif", context);
    }

    public void outputDotImage(String dot, String extension) throws IOException {
        plugin.outputDotImage(dot, extension, context);
    }

}
