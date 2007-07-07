/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
package com.xpn.xwiki.tool.backup;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.web.XWikiServletURLFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Common code for importing and exporting.
 *
 * @version $Id: Importer.java 1632 2006-11-23 16:34:23Z vmassol $
 */
public class AbstractPackager
{
    /**
     * @param databaseName some database name (TODO: find out what this name is really)
     * @param hibernateConfig the Hibernate config fill containing the database definition (JDBC
     *        driver, username and password, etc)
     * @return a valid XWikiContext using the passed Hibernate configuration and passed database
     *         name
     * @throws com.xpn.xwiki.XWikiException if the packaging operation failed for any reason
     * @todo Replace the Hibernate config file with a list of parameters required for the
     *       packaging operation
     */
    protected XWikiContext createXWikiContext(String databaseName, File hibernateConfig)
        throws XWikiException
    {
        XWikiContext context = new XWikiContext();
        context.setUser("XWiki.superadmin");
        context.setDatabase(databaseName);

        XWikiConfig config = new XWikiConfig();
        config.put("xwiki.store.class", "com.xpn.xwiki.store.XWikiHibernateStore");

        // The XWikiConfig object requires path to be in unix format (i.e. with forward slashes)
        String hibernateConfigInUnixFormat = hibernateConfig.getPath().replace('\\', '/');
        config.put("xwiki.store.hibernate.path", hibernateConfigInUnixFormat);

        config.put("xwiki.store.hibernate.updateschema", "1");
        config.put("xwiki.virtual", "1");

        // Enable backlinks so that when documents are imported their backlinks will be saved too
        config.put("xwiki.backlinks", "1");

        XWiki xwiki = new XWiki(config, context);
        context.setWiki(xwiki);

        try {
            context.setURLFactory(new XWikiServletURLFactory(new URL("http://localhost:8080"), "xwiki/", "bin/"));
        } catch (MalformedURLException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }

        return context;
    }
}
