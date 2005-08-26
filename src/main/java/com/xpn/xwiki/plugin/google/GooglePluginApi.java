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
package com.xpn.xwiki.plugin.google;

import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import com.google.api.search.GoogleSearchPort;
import com.google.api.search.GoogleSearchResult;
import com.google.api.search.GoogleSearchService;
import com.google.api.search.GoogleSearchServiceLocator;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

public class GooglePluginApi extends Api {
    private GooglePlugin plugin;

    public GooglePluginApi(GooglePlugin plugin, XWikiContext context) {
            super(context);
            setPlugin(plugin);
        }

    public GooglePlugin getPlugin() {
        return plugin;
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
