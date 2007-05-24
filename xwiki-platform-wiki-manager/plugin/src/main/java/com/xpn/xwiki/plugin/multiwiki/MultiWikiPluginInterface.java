package com.xpn.xwiki.plugin.multiwiki;

import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import java.util.Map;

/**
 * Plugin interface for multiwiki operations
 */
public interface MultiWikiPluginInterface extends XWikiPluginInterface
{
    /**
     * Create a user account with the given params. Allow email validation and username generation
     *
     * @parmam context
     */
    public void createAccount(Map params, String username, boolean emailValidation,
        boolean generateUsername, XWikiContext context) throws MultiWikiPluginException;

    /**
     * Validate the Account with the given username. If the key match, XWikiUsers's parameter active
     * is set to 1.
     *
     * @param xwikiname the username of the account to validate
     * @param validkey the key to validate the account against
     * @param withConfirmationEmail indicates if an confirmation email must be sent after
     * validation
     * @return true on success (key match), false otherwise
     */
    public boolean validateAccount(String xwikiname, String validkey, boolean withConfirmationEmail,
        XWikiContext context) throws MultiWikiPluginException;

    //public int createServerAdmin(String serverName, String owner, String language, XWikiContext context) throws MultiWikiPluginException;

    public int createWiki(String wikiName, String wikiUrl, String wikiAdmin,
        String baseWikiName, String description, String language, boolean failOnExist,
        XWikiContext context) throws MultiWikiPluginException;

    public boolean isAccount(String username, XWikiContext context);

    public boolean isServer(String serverName, XWikiContext context);
}
