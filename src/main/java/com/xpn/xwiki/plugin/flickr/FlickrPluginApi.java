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
package com.xpn.xwiki.plugin.flickr;

import com.aetrion.flickr.Authentication;
import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.photos.SearchParameters;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

public class FlickrPluginApi extends Api {
    private FlickrPlugin plugin;

    public FlickrPluginApi(FlickrPlugin plugin, XWikiContext context) {
            super(context);
            setPlugin(plugin);
        }

    public FlickrPlugin getPlugin() {
        return plugin;
    }
    
    /**
     * @return new SearchParameters instance.  Useful for velocity scripts.
     */
    public SearchParameters getSearchParameters() {
    	return new SearchParameters();
    }

    public void setPlugin(FlickrPlugin plugin) {
        this.plugin = plugin;
    }

    public Flickr getFlickr(String apikey) {
        return new Flickr(apikey);
    }

    public void setAuthentication(String email, String password) {
        Authentication auth = new Authentication();
        auth.setEmail(email);
        auth.setPassword(password);
        RequestContext.getRequestContext().setAuthentication(auth);
    }    
}