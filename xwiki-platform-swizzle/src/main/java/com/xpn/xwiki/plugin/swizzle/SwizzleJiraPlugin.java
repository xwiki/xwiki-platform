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
 */
package com.xpn.xwiki.plugin.swizzle;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

import org.codehaus.swizzle.jira.Jira;
import org.codehaus.swizzle.jira.Issue;
import org.codehaus.swizzle.jira.JiraRss;

import java.net.MalformedURLException;

/**
 * Implementation of the Velocity API defined in {@link com.xpn.xwiki.plugin.swizzle.SwizzleJiraPluginApi}.
 *
 * @version $Id$
 */
public class SwizzleJiraPlugin extends XWikiDefaultPlugin
{
    /**
     * {@inheritDoc}
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#XWikiDefaultPlugin(String, String, com.xpn.xwiki.XWikiContext) 
     */
    public SwizzleJiraPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    /**
     * {@inheritDoc}
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getName()
     */
    public String getName()
    {
        return "swizzle";
    }

    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new SwizzleJiraPluginApi((SwizzleJiraPlugin) plugin, context);
    }

    /**
     * @param url the JIRA URL to connect to. For example "http://jira.acme.org/rpc/xmlrpc".
     * @return a Swizzle {@link Jira} object as described on the
     *         <a href="http://swizzle.codehaus.org/Swizzle+Jira">Swizzle JIRA home page</a>.
     * @throws MalformedURLException in case of invalid URL
     */
    public Jira getJira(String url) throws MalformedURLException
    {
        return new Jira(url);
    }

    /**
     * @param url the JIRA RSS URL to connect to. For example
     *        "http://jira.acme.org/secure/IssueNavigator.jspa?view=rss&&pid=11230....".
     * @return a Swizzle {@link JiraRss} object as described on the
     *         <a href="http://swizzle.codehaus.org/Swizzle+Jira">Swizzle JIRA home page</a>.
     * @throws MalformedURLException in case of invalid URL
     */
    public JiraRss getJiraRss(String url) throws Exception
    {
        return new JiraRss(url);
    }

    /**
     * @return a Swizzle {@link Issue} object
     * @see <a href="http://swizzle.codehaus.org/Swizzle+Jira">Swizzle JIRA home page</a> 
     */
    public Issue createIssue()
    {
    	return new Issue();
    }
}