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
package com.xpn.xwiki.wysiwyg.client.plugin.link;

/**
 * Generates html link blocks for all types of links. 
 * @version $Id$
 */
public final class LinkGenerator
{
    /**
     * The singleton instance of this class.
     */
    private static LinkGenerator instance;

    /**
     * Class constructor, private so that the class is a singleton.
     */
    private LinkGenerator()
    {
    }

    /**
     * @return the instance of this class.
     */
    public static synchronized LinkGenerator getInstance()
    {
        if (instance == null) {
            instance = new LinkGenerator();
        }
        return instance;
    }

    /**
     * Generates link to an external url (web page or email address).
     * 
     * @param label link label
     * @param externalURL external url to link to
     * @return the html link block.
     */
    public String getExternalLink(String label, String externalURL)
    {
        // <!--startwikilink:http://xwiki.org-->
        // <span class="wikiexternallink">
        // <a href="http://xwiki.org">label</a>
        // </span>
        // <!--stopwikilink-->

        // <!--startwikilink:mailto:john@doe.com-->
        // <span class="wikiexternallink">
        // <a href="mailto:john@doe.com">label</a>
        // </span>
        // <!--stopwikilink-->
        return "<!--startwikilink:" + externalURL + "--><span class=\"wikiexternallink\"><a href=\"" + externalURL
            + "\">" + label + "</a></span><!--stopwikilink-->";
    }
}
