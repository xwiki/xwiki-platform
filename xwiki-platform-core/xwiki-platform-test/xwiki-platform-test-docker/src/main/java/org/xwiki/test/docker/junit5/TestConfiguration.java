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
package org.xwiki.test.docker.junit5;

/**
 * Configuration options for the test.
 *
 * @version $Id$
 * @since 10.9
 */
public class TestConfiguration
{
    private static final String FALSE = "false";

    private static final String BROWSER_PROPERTY = "xwiki.test.ui.browser";

    private static final String DATABASE_PROPERTY = "xwiki.test.ui.database";

    private static final String SERVLETENGINE_PROPERTY = "xwiki.test.ui.servletEngine";

    private static final String DEBUG_PROPERTY = "xwiki.test.ui.debug";

    private static final String SAVEDBDATA_PROPERTY = "xwiki.test.ui.saveDatabaseData";

    private UITest uiTestAnnotation;

    private Browser browser;

    private Database database;

    private ServletEngine servletEngine;

    private boolean debug;

    private boolean saveDatabaseData;

    /**
     * @param uiTestAnnotation the annotation from which to extract the configuration
     */
    public TestConfiguration(UITest uiTestAnnotation)
    {
        this.uiTestAnnotation = uiTestAnnotation;
        resolveBrowser();
        resolveDatabase();
        resolveServletEngine();
        resolveDebug();
        resolveSaveDatabaseData();
    }

    private void resolveBrowser()
    {
        Browser newBrowser = this.uiTestAnnotation.browser();
        if (newBrowser == Browser.SYSTEM) {
            newBrowser = Browser.valueOf(System.getProperty(BROWSER_PROPERTY, Browser.CHROME.name()).toUpperCase());
        }
        this.browser = newBrowser;
    }

    private void resolveDatabase()
    {
        Database newDatabase = this.uiTestAnnotation.database();
        if (newDatabase == Database.SYSTEM) {
            newDatabase = Database.valueOf(System.getProperty(DATABASE_PROPERTY, Database.MYSQL.name()).toUpperCase());
        }
        this.database = newDatabase;
    }

    private void resolveServletEngine()
    {
        ServletEngine newServletEngine = this.uiTestAnnotation.servletEngine();
        if (newServletEngine == ServletEngine.SYSTEM) {
            newServletEngine = ServletEngine.valueOf(System.getProperty(SERVLETENGINE_PROPERTY,
                ServletEngine.TOMCAT.name()).toUpperCase());
        }
        this.servletEngine = newServletEngine;
    }

    private void resolveDebug()
    {
        boolean newDebug = this.uiTestAnnotation.debug();
        if (!newDebug) {
            newDebug = Boolean.valueOf(System.getProperty(DEBUG_PROPERTY, FALSE));
        }
        this.debug = newDebug;
    }

    private void resolveSaveDatabaseData()
    {
        boolean newSaveDatabaseData = this.uiTestAnnotation.saveDatabaseData();
        if (!newSaveDatabaseData) {
            newSaveDatabaseData = Boolean.valueOf(System.getProperty(SAVEDBDATA_PROPERTY, FALSE));
        }
        this.saveDatabaseData = newSaveDatabaseData;
    }

    /**
     * @return the browser to use
     */
    public Browser getBrowser()
    {
        return this.browser;
    }

    /**
     * @return the database to use
     */
    public Database getDatabase()
    {
        return this.database;
    }

    /**
     * @return the Servlet engine to use
     */
    public ServletEngine getServletEngine()
    {
        return this.servletEngine;
    }

    /**
     * @return true if we're in debug mode and should output more information to the console
     */
    public boolean isDebug()
    {
        return this.debug;
    }

    /**
     * @return true true if the database data should be mapped to a local directory on the host computer so that it can
     *         be saved and reused for another run
     */
    public boolean isDatabaseDataSaved()
    {
        return this.saveDatabaseData;
    }
}
