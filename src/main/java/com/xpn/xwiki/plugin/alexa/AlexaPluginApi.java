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
package com.xpn.xwiki.plugin.alexa;

import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import com.amazon.api.alexa.AWSAlexa;
import com.amazon.api.alexa.AWSAlexaLocator;
import com.amazon.api.alexa.AWSAlexaPortType;
import com.amazon.api.alexa.UrlInfoRequest;
import com.amazon.api.alexa.UrlInfoResult;
import com.amazon.api.alexa.holders.OperationRequestHolder;
import com.amazon.api.alexa.holders.UrlInfoResultHolder;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

public class AlexaPluginApi extends Api {
    private AlexaPlugin plugin;

    public AlexaPluginApi(AlexaPlugin plugin, XWikiContext context) {
            super(context);
            setPlugin(plugin);
        }

    public AlexaPlugin getPlugin() {
        return plugin;
    }

    public void setPlugin(AlexaPlugin plugin) {
        this.plugin = plugin;
    }

    public UrlInfoRequest getUrlInfoRequest() {
        return new UrlInfoRequest();
    }

    public OperationRequestHolder getOperationRequestHolder() {
        return new OperationRequestHolder();
    }

    public UrlInfoResultHolder getUrlInfoResultHolder() {
        return new UrlInfoResultHolder();
    }

    public UrlInfoResult getUrlInfo(String subscriptionId, String associateTag, String validate,
                                    UrlInfoRequest shared, UrlInfoRequest[] request, OperationRequestHolder operationRequest) throws ServiceException, RemoteException {

        // Make a service
        AWSAlexa service = new AWSAlexaLocator();

        // Now use the service to get a stub to the Service Definition Interface (SDI)
        AWSAlexaPortType alexa = service.getAWSAlexaPort();

        UrlInfoResultHolder urlInfoResult = new UrlInfoResultHolder();
        alexa.urlInfo(subscriptionId, associateTag, validate, shared, request,
                        operationRequest, urlInfoResult);
        return urlInfoResult.value;
    }

    public UrlInfoResult getUrlInfo(String subscriptionId, String url) throws ServiceException, RemoteException {

        // Make a service
        AWSAlexa service = new AWSAlexaLocator();

        // Now use the service to get a stub to the Service Definition Interface (SDI)
        AWSAlexaPortType alexa = service.getAWSAlexaPort();

        UrlInfoResultHolder urlInfoResult = new UrlInfoResultHolder();
        UrlInfoRequest request = new UrlInfoRequest(url,"TrafficData,ContactInfo,Categories,RelatedLinks","");
        try { alexa.urlInfo(subscriptionId, "", "", request, null,
                      getOperationRequestHolder(), urlInfoResult);
        } catch (Exception e) {}
        return urlInfoResult.value;
    }

    public UrlInfoResult getUrlInfo(String subscriptionId, String url, String responseGroup) throws ServiceException, RemoteException {

        // Make a service
        AWSAlexa service = new AWSAlexaLocator();

        // Now use the service to get a stub to the Service Definition Interface (SDI)
        AWSAlexaPortType alexa = service.getAWSAlexaPort();

        UrlInfoResultHolder urlInfoResult = new UrlInfoResultHolder();
        UrlInfoRequest request = new UrlInfoRequest(url,responseGroup,"");
        try {
            alexa.urlInfo(subscriptionId, "", "", request, null,
                    getOperationRequestHolder(), urlInfoResult);
        } catch (Exception e) {}
        return urlInfoResult.value;
    }

    public UrlInfoResult getUrlInfo(String subscriptionId, String url, String responseGroup, String countryCodes) throws ServiceException, RemoteException {

        // Make a service
        AWSAlexa service = new AWSAlexaLocator();

        // Now use the service to get a stub to the Service Definition Interface (SDI)
        AWSAlexaPortType alexa = service.getAWSAlexaPort();

        UrlInfoResultHolder urlInfoResult = new UrlInfoResultHolder();
        UrlInfoRequest request = new UrlInfoRequest(url,responseGroup,countryCodes);
        try {
            alexa.urlInfo(subscriptionId, "", "", request, null,
                    getOperationRequestHolder(), urlInfoResult);
        } catch (Exception e) {}
        return urlInfoResult.value;
    }

        //public void categoryBrowse(java.lang.String subscriptionId, java.lang.String associateTag, java.lang.String validate, com.amazon.api.alexa.CategoryBrowseRequest shared, com.amazon.api.alexa.CategoryBrowseRequest[] request, com.amazon.api.alexa.holders.OperationRequestHolder operationRequest, com.amazon.api.alexa.holders.CategoryBrowseResultHolder categoryBrowseResult) throws java.rmi.RemoteException;
        //public void categoryListings(java.lang.String subscriptionId, java.lang.String associateTag, java.lang.String validate, com.amazon.api.alexa.CategoryListingsRequest shared, com.amazon.api.alexa.CategoryListingsRequest[] request, com.amazon.api.alexa.holders.OperationRequestHolder operationRequest, com.amazon.api.alexa.holders.CategoryListingsResultHolder categoryListingsResult) throws java.rmi.RemoteException;
        //public void crawl(java.lang.String subscriptionId, java.lang.String associateTag, java.lang.String validate, com.amazon.api.alexa.CrawlRequest shared, com.amazon.api.alexa.CrawlRequest[] request, com.amazon.api.alexa.holders.OperationRequestHolder operationRequest, com.amazon.api.alexa.holders.CrawlResultHolder crawlResult) throws java.rmi.RemoteException;
        //public void search(java.lang.String subscriptionId, java.lang.String associateTag, java.lang.String validate, com.amazon.api.alexa.SearchRequest shared, com.amazon.api.alexa.SearchRequest[] request, com.amazon.api.alexa.holders.OperationRequestHolder operationRequest, com.amazon.api.alexa.holders.SearchResultHolder searchResult) throws java.rmi.RemoteException;
        //public void webMap(java.lang.String subscriptionId, java.lang.String associateTag, java.lang.String validate, com.amazon.api.alexa.WebMapRequest shared, com.amazon.api.alexa.WebMapRequest[] request, com.amazon.api.alexa.holders.OperationRequestHolder operationRequest, com.amazon.api.alexa.holders.WebMapResultHolder webMapResult) throws java.rmi.RemoteException;
}
