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
package com.xpn.xwiki.plugin.diff;

import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;

/**
 * @version $Id$
 * @deprecated since 4.1 use diff service
 */
@Deprecated
public class DiffPluginApi extends Api
{
    private DiffPlugin plugin;

    public DiffPluginApi(DiffPlugin plugin, XWikiContext context)
    {
        super(context);
        setPlugin(plugin);
    }

    public DiffPlugin getPlugin()
    {
        if (hasProgrammingRights()) {
            return this.plugin;
        }
        return null;
    }

    public void setPlugin(DiffPlugin plugin)
    {
        this.plugin = plugin;
    }

    /**
     * Return a list of Delta objects representing line differences in text1 and text2
     *
     * @param text1 original content
     * @param text2 revised content
     * @return list of Delta objects
     */
    public List getDifferencesAsList(String text1, String text2) throws XWikiException
    {
        return this.plugin.getDifferencesAsList(text1, text2);
    }

    /**
     * Return an html blocks representing line diffs between text1 and text2
     *
     * @param text1 original content
     * @param text2 revised content
     * @return list of Delta objects
     */
    public String getDifferencesAsHTML(String text1, String text2) throws XWikiException
    {
        return this.plugin.getDifferencesAsHTML(text1, text2);
    }

    /**
     * Return an html blocks representing line diffs between text1 and text2
     *
     * @param text1 original content
     * @param text2 revised content
     * @param allDoc view all document or only changes
     * @return list of Delta objects
     */
    public String getDifferencesAsHTML(String text1, String text2, boolean allDoc) throws XWikiException
    {
        return this.plugin.getDifferencesAsHTML(text1, text2, allDoc);
    }

    /**
     * Return a list of Delta objects representing word differences in text1 and text2
     *
     * @param text1 original content
     * @param text2 revised content
     * @return list of Delta objects
     */
    public List getWordDifferencesAsList(String text1, String text2) throws XWikiException
    {
        return this.plugin.getWordDifferencesAsList(text1, text2);
    }

    /**
     * Return an html blocks representing word diffs between text1 and text2
     *
     * @param text1 original content
     * @param text2 revised content
     * @return list of Delta objects
     */
    public String getWordDifferencesAsHTML(String text1, String text2) throws XWikiException
    {
        return this.plugin.getWordDifferencesAsHTML(text1, text2);
    }

}
