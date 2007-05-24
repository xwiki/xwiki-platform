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

import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.api.*;

import java.util.Map;
import java.util.List;
import java.lang.Object;

import org.apache.log4j.Logger;

/**
 * {@inheritDoc}
 */
public class MultiWikiPlugin extends XWikiDefaultPlugin implements MultiWikiPluginInterface
{
    /**
     * Log4J logger object to log messages in this class.
     */
    private static final Logger LOG = Logger.getLogger(MultiWikiPlugin.class);

    /**
     * {@inheritDoc}
     *
     * @see XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public MultiWikiPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getName()
     */
    public String getName()
    {
        return "multiwiki";
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getPluginApi
     */
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new MultiWikiPluginAPI((MultiWikiPlugin) plugin, context);
    }

    /**
     * {@inheritDoc}
     */
    public void createAccount(Map params, String username, boolean emailValidation,
        boolean generateUsername, XWikiContext context) throws MultiWikiPluginException
    {
        if (isAccount(username, context)) {
            throw new MultiWikiPluginException(
                MultiWikiPluginException.ERROR_MULTIWIKI_ACCOUNT_ALREADY_EXISTS,
                "Account with user name " + username + "already exists"
            );
        }
        try {
            String validKey = "";
            if (emailValidation) {
                validKey = context.getWiki().generateValidationKey(16);
                params.put("validkey", validKey);
                params.put("active", "0");
            } else {
                params.put("active", "1");
            }

            context.getWiki()
                .createUser(username, params, "", getUserTemplate(context), "view, edit", context);

            if (emailValidation) {
                context.getWiki().sendValidationEmail("XWiki." + username,
                    (String) params.get("password"), (String) params.get("email"), validKey,
                    "validation_email_content", context);
            }
        }
        catch (XWikiException e) {
            Object[] args = {username};
            throw new MultiWikiPluginException(
                MultiWikiPluginException.ERROR_MULTIWIKI_CANNOT_CREATE_ACCOUNT,
                "Cannot create account with username {0}",
                e,
                args
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean validateAccount(String xwikiname, String validkey, boolean withConfirmationEmail,
        XWikiContext context) throws MultiWikiPluginException
    {
        //I do not use xwiki.validateAccount() since it depends on request parameters
        try {
            boolean result = false;
            if (xwikiname.indexOf(".") == -1) {
                xwikiname = "XWiki." + xwikiname;
            }

            XWikiDocument docuser = context.getWiki().getDocument(xwikiname, context);
            BaseObject userobj = docuser.getObject("XWiki.XWikiUsers", 0);
            String validkey2 = userobj.getStringValue("validkey");
            String email = userobj.getStringValue("email");
            String password = userobj.getStringValue("password");

            if ((!validkey2.equals("") && (validkey2.equals(validkey)))) {
                userobj.setIntValue("active", 1);
                context.getWiki().saveDocument(docuser, context);

                result = true;

                if (withConfirmationEmail) {
                    context.getWiki().sendValidationEmail(xwikiname, password, email, validkey,
                        "confirmation_email_content", context);
                }
            }
            return result;
        }
        catch (XWikiException e) {
            Object[] args = {xwikiname};
            throw new MultiWikiPluginException(
                MultiWikiPluginException.ERROR_MULTIWIKI_CANNOT_CANNOT_VALIDATE_ACCOUNT,
                "Could not validate user with name {0}",
                e,
                args);
        }
    }

    public int createWiki(String wikiName, String wikiUrl, String wikiAdmin,
        String baseWikiName, String description, String language, boolean failOnExist,
        XWikiContext context) throws MultiWikiPluginException
    {
        try {
            return context.getWiki().createNewWiki(wikiName, wikiUrl, wikiAdmin, baseWikiName,
                description, language, failOnExist, context);
        } catch (XWikiException e) {
            Object args[] = {wikiName};
            throw new MultiWikiPluginException(
                MultiWikiPluginException.ERROR_MULTIWIKI_CANNOT_CREATE_WIKI,
                "Could not create wiki with wiki name {0}",
                e,
                args);
        }
    }

    public List getWikiList(String username)
    {
        //todo
        return null;
    }

    public List getWikiList()
    {
        //todo
        return null;
    }

    public boolean isAccount(String username, XWikiContext context)
    {
        try {
            return !context.getWiki().getDocument("XWiki." + username, context).isNew();
        } catch (XWikiException e) {
            return true;
        }
    }

    public boolean isServer(String serverName, XWikiContext context)
    {
        try {
            return !context.getWiki().getDocument("XWiki.XWikiServer" + serverName, context)
                .isNew();
        }
        catch (XWikiException e) {
            return true;
        }
    }

    private String getUserTemplate(XWikiContext context) throws XWikiException
    {
        return context.getWiki().getDocument("XWiki.XWikiUserTemplate", context).getContent();
    }
}
