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

import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.XWikiContext;

import org.codehaus.swizzle.jira.Jira;
import org.codehaus.swizzle.jira.JiraRss;
import org.codehaus.swizzle.jira.Issue;

import java.net.MalformedURLException;

/**
 * Allows getting Swizzle Objects (http://swizzle.codehaus.org/) so that it's easy to use from a
 * Velocity page in a XWiki page.
 * <p>Example:</p>
 * <code><pre>
 * #set ($jira = $xwiki.swizzle.getJira("http://jira.xwiki.org/jira/rpc/xmlrpc"))
 * $jira.login("username", "password")
 *
 * {table}
 * ID | Key | Project Name | Project Lead | Project URL
 * #foreach ($project in $jira.getProjects())
 *   $project.getId() | $project.getKey() | $project.getName() | $project.getLead() | $project.getProjectUrl()
 * #end
 * {table}
 * </pre></code>
 *
 * @version $Id$
 */
public class SwizzleJiraPluginApi extends Api
{
    private SwizzleJiraPlugin plugin;

    public SwizzleJiraPluginApi(SwizzleJiraPlugin plugin, XWikiContext context)
    {
        super(context);
        this.plugin = plugin;
    }

    /**
     * @param url the JIRA URL to connect to. For example "http://jira.acme.org/rpc/xmlrpc".
     * @return a Swizzle {@link Jira} object as described on the
     *         <a href="http://swizzle.codehaus.org/Swizzle+Jira">Swizzle JIRA home page</a>.
     * @throws MalformedURLException in case of invalid URL
     */
    public Jira getJira(String url) throws MalformedURLException
    {
        return this.plugin.getJira(url);
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
        return this.plugin.getJiraRss(url);
    }
    
    /**
     * @return a Swizzle {@link Issue} object
     * @see <a href="http://swizzle.codehaus.org/Swizzle+Jira">Swizzle JIRA home page</a>
     */
    public Issue createIssue()
    {
    	return this.plugin.createIssue();
    }
}