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
 * @author sdumitriu
 */

package com.xpn.xwiki.plugin.adwords;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;

import javax.xml.rpc.ServiceException;

import org.apache.axis.client.Stub;

import com.google.api.adwords.v2.Campaign;
import com.google.api.adwords.v2.CampaignService;
import com.google.api.adwords.v2.CampaignServiceService;
import com.google.api.adwords.v2.CampaignServiceServiceLocator;
import com.google.api.adwords.v2.KeywordEstimate;
import com.google.api.adwords.v2.KeywordRequest;
import com.google.api.adwords.v2.StatsRecord;
import com.google.api.adwords.v2.TrafficEstimatorInterface;
import com.google.api.adwords.v2.TrafficEstimatorService;
import com.google.api.adwords.v2.TrafficEstimatorServiceLocator;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.PluginApi;

/**
 * @version $Id$
 * @deprecated the plugin technology is deprecated
 */
@Deprecated
public class AdWordsPluginApi extends PluginApi<AdWordsPlugin>
{
    /** The namespace used for API headers. **/
    private static final String apiNS = "https://adwords.google.com/api/adwords/v2";

    /**
     * Default plugin API constructor.
     * 
     * @param plugin the wrapped plugin instance
     * @param context the current request context
     */
    public AdWordsPluginApi(AdWordsPlugin plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    /**
     * Return the inner plugin object, if the user has the required programming rights.
     * 
     * @return The wrapped plugin object.
     */
    public AdWordsPlugin getPlugin()
    {
        return getInternalPlugin();
    }

    public KeywordRequest newKeywordRequest()
    {
        return new KeywordRequest();
    }

    public KeywordEstimate[] getTrafficEstimate(String token, String email, String password, KeywordRequest[] kr)
        throws ServiceException, RemoteException
    {
        // Make a service
        TrafficEstimatorService service = new TrafficEstimatorServiceLocator();
        // Now use the service to get a stub to the Service Definition Interface (SDI)
        TrafficEstimatorInterface traffic = service.getTrafficEstimatorService();

        addSecurity((Stub) traffic, email, password, token);

        return traffic.estimateKeywordList(kr);
    }

    public Campaign[] getCampaignList(String token, String email, String password, int[] ids) throws ServiceException,
        RemoteException
    {
        // Make a service
        CampaignServiceService service = new CampaignServiceServiceLocator();
        // Now use the service to get a stub to the Service Definition Interface (SDI)
        CampaignService campaign = service.getCampaignService();

        addSecurity((Stub) campaign, email, password, token);
        return campaign.getCampaignList(ids);
    }

    public StatsRecord[] getCampaignList(String token, String email, String password, int[] ids, Date startDate,
        Date endDate) throws ServiceException, RemoteException
    {
        // Make a service
        CampaignServiceService service = new CampaignServiceServiceLocator();
        // Now use the service to get a stub to the Service Definition Interface (SDI)
        CampaignService campaign = service.getCampaignService();

        addSecurity((Stub) campaign, email, password, token);

        Calendar startcal = Calendar.getInstance();
        Calendar endcal = Calendar.getInstance();
        startcal.setTime(startDate);
        endcal.setTime(endDate);
        return campaign.getCampaignStats(ids, startcal, endcal);
    }

    private void addSecurity(Stub stub, String email, String password, String token)
    {
        // Set the authentication request headers
        stub.setHeader(apiNS, "email", email);
        stub.setHeader(apiNS, "password", password);
        stub.setHeader(apiNS, "useragent", "XWiki AdWords Plugin version 1.0");
        stub.setHeader(apiNS, "token", token);
    }
}
