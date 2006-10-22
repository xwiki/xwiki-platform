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

package com.xpn.xwiki.plugin.terraserver;

import com.ms.terraservice.*;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

import javax.xml.rpc.ServiceException;
import java.rmi.RemoteException;

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
