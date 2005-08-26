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
package com.xpn.xwiki.plugin.ipresolver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import com.cdyne.ipresolver.IP2GeoLocator;
import com.cdyne.ipresolver.IP2GeoSoap;
import com.cdyne.ipresolver.IPInformation;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

public class IPResolverPluginApi extends Api {
    private IPResolverPlugin plugin;

    public IPResolverPluginApi(IPResolverPlugin plugin, XWikiContext context) {
            super(context);
            setPlugin(plugin);
        }

    public IPResolverPlugin getPlugin() {
        return plugin;
    }

    public void setPlugin(IPResolverPlugin plugin) {
        this.plugin = plugin;
    }

    public IPInformation resolveIP(String key, String ip) throws ServiceException, RemoteException {

        // Make a service
        IP2GeoLocator service = new IP2GeoLocator();

        // Now use the service to get a stub to the Service Definition Interface (SDI)
        IP2GeoSoap ipresolver = service.getIP2GeoSoap();

       return ipresolver.resolveIP(getIP(ip), key);
    }

    public String getIP(String host) {
        try {
            return InetAddress.getByName( host).getHostAddress();
        } catch (UnknownHostException e) {
            return host;
        }
    }
}
