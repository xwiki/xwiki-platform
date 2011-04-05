/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
 */

package com.xpn.xwiki.plugin.google;

import com.google.api.search.GoogleSearchPort;
import com.google.api.search.GoogleSearchResult;
import com.google.api.search.GoogleSearchService;
import com.google.api.search.GoogleSearchServiceLocator;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

import javax.xml.rpc.ServiceException;
import java.rmi.RemoteException;

public class GooglePluginApi extends Api {
    private GooglePlugin plugin;

    public GooglePluginApi(GooglePlugin plugin, XWikiContext context) {
            super(context);
            setPlugin(plugin);
        }

    public GooglePlugin getPlugin() {
        if (hasProgrammingRights()) {
            return plugin;
        }
        return null;
    }

    public void setPlugin(GooglePlugin plugin) {
        this.plugin = plugin;
    }

    public String doSpellingSuggestion(String clientKey, String text) throws ServiceException, RemoteException {
        // Make a service
        GoogleSearchService service = new GoogleSearchServiceLocator();

        // Now use the service to get a stub to the Service Definition Interface (SDI)
        GoogleSearchPort google = service.getGoogleSearchPort();

        // Depending on user input, do search or cache query, then print out result
        return google.doSpellingSuggestion(clientKey, text);
    }

    public byte[] doGetCachedPage(String clientKey, String url) throws RemoteException, ServiceException {
        // Make a service
        GoogleSearchService service = new GoogleSearchServiceLocator();
        // Now use the service to get a stub to the Service Definition Interface (SDI)
        GoogleSearchPort google = service.getGoogleSearchPort();

        return google.doGetCachedPage(clientKey, url);
    }

    public GoogleSearchResult doGoogleSearch(String clientKey, String query, int start, int maxResults,
                                             boolean filter, String restrict, boolean safeSearch,
                                             String lr, String ie, String oe) throws RemoteException, ServiceException {
        // Make a service
        GoogleSearchService service = new GoogleSearchServiceLocator();
        // Now use the service to get a stub to the Service Definition Interface (SDI)
        GoogleSearchPort google = service.getGoogleSearchPort();

        return google.doGoogleSearch(clientKey, query, start, maxResults,  filter,
                restrict, safeSearch,  lr, ie,  oe);
    }

    public GoogleSearchResult doGoogleSearch(String clientKey, String query
                                             ) throws RemoteException, ServiceException {
        // Make a service
        GoogleSearchService service = new GoogleSearchServiceLocator();
        // Now use the service to get a stub to the Service Definition Interface (SDI)
        GoogleSearchPort google = service.getGoogleSearchPort();

        GoogleSearchResult result = google.doGoogleSearch(clientKey, query, 0, 10,  true,
                "", true,  "", "",  "");
        return result;
    }

    public GoogleSearchResult doGoogleSearch(String clientKey, String query, int start
                                             ) throws RemoteException, ServiceException {
        // Make a service
        GoogleSearchService service = new GoogleSearchServiceLocator();
        // Now use the service to get a stub to the Service Definition Interface (SDI)
        GoogleSearchPort google = service.getGoogleSearchPort();

        return google.doGoogleSearch(clientKey, query, start, 10,  true,
                "", true,  "", "",  "");
    }

    public GoogleSearchResult doGoogleSearch(String clientKey, String query, int start, String lr
                                             ) throws RemoteException, ServiceException {
        // Make a service
        GoogleSearchService service = new GoogleSearchServiceLocator();
        // Now use the service to get a stub to the Service Definition Interface (SDI)
        GoogleSearchPort google = service.getGoogleSearchPort();

        return google.doGoogleSearch(clientKey, query, start, 10,  true,
                "", true,  lr, "",  "");
    }
}
