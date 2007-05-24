/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
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
 * @author jvelociter
 */
package com.xpn.xwiki.plugin.multiwiki;

import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import java.util.Map;
import java.util.List;

/**
 * API for managing multiwikis
 *
 * @version $Id: $
 * @see com.xpn.xwiki.plugin.multiwiki.MultiWikiPlugin
 */
public class MultiWikiPluginAPI extends Api
{
    /**
     * The MultiWiki plugin hidden by this API class. This is to create a separation between the API
     * used by users in XWiki documents and the actual implementations.
     */
    private MultiWikiPlugin plugin;

    /**
     * @param context the XWiki Context object
     * @param plugin the MutliWikiPlugin plugin
     * @see #getXWikiContext()
     */
    public MultiWikiPluginAPI(MultiWikiPlugin plugin, XWikiContext context)
    {
        super(context);
        this.plugin = plugin;
    }

    /**
     * Create an account on the top level wiki
     *
     * @param params A map that contains account parameters for creation
     * @param username the username of the account to be created
     * @param emailValidation true if the account creation needs validation by mail
     * @param generateUsername true if the user name has to be generated
     * @return true on success, false on failure. On failure the error log is pushed in the
     *         context.
     */
    public void createAccount(Map params, String username, boolean emailValidation,
        boolean generateUsername)
    {
        try {
            plugin.createAccount(params, username, emailValidation, generateUsername, context);
        } catch (MultiWikiPluginException e) {
            context.put(
                "error",
                "Error while creation account : " + e.getMessage()
            );
        }
    }

    /**
     *
     * @param wikiName
     * @param wikiUrl
     * @param wikiAdmin
     * @param baseWikiName
     * @param description
     * @param language
     * @param failOnExist
     * @param context
     * @return
     * @throws XWikiException
     */
    public int createNewWiki(String wikiName, String wikiUrl, String wikiAdmin,
        String baseWikiName, String description, String language, boolean failOnExist,
        XWikiContext context) throws XWikiException
    {
        return plugin.createWiki(wikiName, wikiUrl, wikiAdmin, baseWikiName, description, language,
            failOnExist, context);
    }

    /**
     * Get the list of wiki associated to a given username
     *
     * @param username the name of the user that own the wikis to be retrieved
     * @return the list of wikis owned by the user
     */
    public List getWikiList(String username)
    {
        return plugin.getWikiList(username);
    }

    /**
     * Get all wikis
     *
     * @return a list of all the wikis
     */
    public List getWikiList()
    {
        return plugin.getWikiList();
    }

    /**
     * Validate an account from email
     *
     * @param username the username of the account to be validated
     * @param validKey the validation key the user received by mail
     * @param withConfirmation indicates if the user shall receive a confirmation email of his
     * account validation
     * @return true on success, false on failure
     */
    public int validateAccount(String username, String validKey, boolean withConfirmation)
    {
        try {
            if (plugin.validateAccount(username, validKey, withConfirmation, context)) {
                return 0;
            } else {
                return -1;
            }
        } catch (MultiWikiPluginException e) {
            context.put("error", "Error occured while validating account with username \"" +
                username + "\". Caused by : " + e.getMessage());
            return -2;
        }
    }

    /**
     * Check if a given username is actually an account
     *
     * @return true if the account exists for the given username, false otherwise
     */
    public boolean isAccount(String username)
    {
        return plugin.isAccount(username, context);
    }

    /**
     * Check if a Server of the given name exists in the master Wiki by checking if the
     * "XWiki.XWikiServer{serverName}" document is new
     *
     * @param serverName the name of the server to be checked
     * @param context the context
     * @return true if server exists, false otherwise
     */
    public boolean isServer(String serverName, XWikiContext context)
    {
        return plugin.isServer(serverName, context);
    }
}
