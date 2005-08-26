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
package com.xpn.xwiki.plugin.terraserver;

import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import com.ms.terraservice.LonLatPt;
import com.ms.terraservice.Place;
import com.ms.terraservice.PlaceFacts;
import com.ms.terraservice.Scale;
import com.ms.terraservice.TerraServiceLocator;
import com.ms.terraservice.TerraServiceSoap;
import com.ms.terraservice.TileId;
import com.ms.terraservice.TileMeta;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

public class TerraServerPluginApi extends Api {
    private TerraServerPlugin plugin;

    public TerraServerPluginApi(TerraServerPlugin plugin, XWikiContext context) {
            super(context);
            setPlugin(plugin);
        }

    public TerraServerPlugin getPlugin() {
        return plugin;
    }

    public void setPlugin(TerraServerPlugin plugin) {
        this.plugin = plugin;
    }

    public LonLatPt getLonLatPt(String lon, String lat) {
        LonLatPt lonlat = new LonLatPt();
        lonlat.setLon(Double.parseDouble(lon));
        lonlat.setLat(Double.parseDouble(lat));
        return lonlat;
    }

    public LonLatPt getLonLatPt(double lon, double lat) {
        LonLatPt lonlat = new LonLatPt();
        lonlat.setLon(lon);
        lonlat.setLat(lat);
        return lonlat;
    }

    public Place getPlace(String city, String state, String country) {
        Place place = new Place();
        place.setCity(city);
        place.setState(state);
        place.setCountry(country);
        return place;
    }

    public String getNearestPlace(LonLatPt lonlatpt) throws ServiceException, RemoteException {

        // Make a service
        TerraServiceLocator service = new TerraServiceLocator();

        // Now use the service to get a stub to the Service Definition Interface (SDI)
        TerraServiceSoap terraserver = service.getTerraServiceSoap();

       return terraserver.convertLonLatPtToNearestPlace(lonlatpt);
    }

    public PlaceFacts getPlaceFacts(String city, String state, String country) throws ServiceException, RemoteException {

        // Make a service
        TerraServiceLocator service = new TerraServiceLocator();

        // Now use the service to get a stub to the Service Definition Interface (SDI)
        TerraServiceSoap terraserver = service.getTerraServiceSoap();

       return terraserver.getPlaceFacts(getPlace(city, state, country));
    }

    public TileMeta getTileMetaFromLonLatPt(LonLatPt lonlat, String scale) throws ServiceException, RemoteException {

        // Make a service
        TerraServiceLocator service = new TerraServiceLocator();

        // Now use the service to get a stub to the Service Definition Interface (SDI)
        TerraServiceSoap terraserver = service.getTerraServiceSoap();

       return terraserver.getTileMetaFromLonLatPt(lonlat, 1, Scale.fromString(scale));
    }

    public byte[] getTileFromLonLatPt(LonLatPt lonlat, String scale) throws ServiceException, RemoteException {

        // Make a service
        TerraServiceLocator service = new TerraServiceLocator();

        // Now use the service to get a stub to the Service Definition Interface (SDI)
        TerraServiceSoap terraserver = service.getTerraServiceSoap();

       TileMeta tmeta = terraserver.getTileMetaFromLonLatPt(lonlat, 1,Scale.fromString(scale));
       return terraserver.getTile(tmeta.getId());
    }

    public byte[] getTileFromLonLatPt(LonLatPt lonlat, String scale, int theme) throws ServiceException, RemoteException {

        // Make a service
        TerraServiceLocator service = new TerraServiceLocator();

        // Now use the service to get a stub to the Service Definition Interface (SDI)
        TerraServiceSoap terraserver = service.getTerraServiceSoap();

       TileMeta tmeta = terraserver.getTileMetaFromLonLatPt(lonlat, theme,Scale.fromString(scale));
       return terraserver.getTile(tmeta.getId());
    }

    public byte[] getTile(TileId tileId) throws ServiceException, RemoteException {

        // Make a service
        TerraServiceLocator service = new TerraServiceLocator();

        // Now use the service to get a stub to the Service Definition Interface (SDI)
        TerraServiceSoap terraserver = service.getTerraServiceSoap();

       return terraserver.getTile(tileId);
    }


}
