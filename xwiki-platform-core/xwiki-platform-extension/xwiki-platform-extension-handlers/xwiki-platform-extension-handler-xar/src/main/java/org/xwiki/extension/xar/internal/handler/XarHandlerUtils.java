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
package org.xwiki.extension.xar.internal.handler;

/**
 * Various tools commons to several parts of the handler.
 * 
 * @version $Id$
 * @since 5.4M1
 */
public final class XarHandlerUtils
{
    protected static final String WIKI_NAMESPACEPREFIX = "wiki:";

    private XarHandlerUtils()
    {
        // Utlity class
    }
    
    /**
     * @param namespace the namespace
     * @return the wiki name
     * @throws UnsupportedNamespaceException when it's not a wiki namespace
     */
    public static String getWikiFromNamespace(String namespace) throws UnsupportedNamespaceException
    {
        String wiki = namespace;

        if (wiki != null) {
            if (wiki.startsWith(WIKI_NAMESPACEPREFIX)) {
                wiki = wiki.substring(WIKI_NAMESPACEPREFIX.length());
            } else {
                throw new UnsupportedNamespaceException("Unsupported namespace [" + namespace
                    + "], only wiki:wikiid format is supported");
            }
        }

        return wiki;
    }
}
