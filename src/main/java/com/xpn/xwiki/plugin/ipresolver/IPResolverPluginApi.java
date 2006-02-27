/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
 *
 * @author sdumitriu
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
