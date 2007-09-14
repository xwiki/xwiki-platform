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
 */
package com.xpn.xwiki.plugin.exoplatform;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

import javax.jcr.*;

import org.exoplatform.services.jcr.RepositoryService;
import org.apache.log4j.Logger;

/**
 * Allows getting an Exoplatform JCR Session so that it becomes possible to get data out of JCR
 * and display it inside XWiki or store XWiki data in Exo's JCR.
 *
 * @version $Id: $
 */
public class ExoPlatformPlugin extends XWikiDefaultPlugin
{
    /**
     * Log4J logger object to log messages in this class.
     */
    private static final Logger LOG = Logger.getLogger(ExoPlatformPlugin.class);

    public ExoPlatformPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    public String getName()
    {
        return "exoplatform";
    }

    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new ExoPlatformPluginApi((ExoPlatformPlugin) plugin, context);
    }

    public Session getRepositorySession(XWikiContext context) throws XWikiException
    {
        Session session;

        try {
            RepositoryService repositoryService =
                (RepositoryService) context.getWiki().getPortalService(
                    "org.exoplatform.services.jcr.RepositoryService");
            Repository repository = repositoryService.getRepository();
            Credentials credentials = new SimpleCredentials("exoadmin", "exo@ecm".toCharArray());
            session = repository.login(credentials);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_INIT_FAILED,
                "Failed to get Session for Repository Service", e);
        }

        return session;
    }
}
