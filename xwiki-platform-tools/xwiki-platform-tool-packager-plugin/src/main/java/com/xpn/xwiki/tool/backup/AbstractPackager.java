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
package com.xpn.xwiki.tool.backup;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiServletResponseStub;
import com.xpn.xwiki.web.XWikiServletURLFactory;

/**
 * Common code for importing and exporting.
 *
 * @version $Id$
 */
public abstract class AbstractPackager
{
    /**
     * @param databaseName some database name (TODO: find out what this name is really)
     * @param hibernateConfig the Hibernate config fill containing the database definition (JDBC driver, username and
     *            password, etc)
     * @return a valid XWikiContext using the passed Hibernate configuration and passed database name
     * @throws Exception failed to initialize context.
     * @todo Replace the Hibernate config file with a list of parameters required for the packaging operation
     */
    public XWikiContext createXWikiContext(String databaseName, File hibernateConfig) throws Exception
    {
        // Initialize the Component Manager and Environment
        ComponentManager cm = org.xwiki.environment.System.initialize();
        Utils.setComponentManager(cm);

        XWikiContext xcontext = new XWikiContext();
        xcontext.put(ComponentManager.class.getName(), cm);

        // Initialize the Container fields (request, response, session).
        ExecutionContextManager ecim = cm.getInstance(ExecutionContextManager.class);
        try {
            ExecutionContext econtext = new ExecutionContext();

            // Bridge with old XWiki Context, required for old code.
            xcontext.declareInExecutionContext(econtext);

            ecim.initialize(econtext);
        } catch (ExecutionContextException e) {
            throw new Exception("Failed to initialize Execution Context.", e);
        }

        xcontext.setWikiId(databaseName);
        xcontext.setMainXWiki(databaseName);

        // Use a dummy Request/Response even in daemon mode so that XWiki's initialization can create a Servlet URL
        // Factory and any code requiring those objects will work.
        xcontext.setRequest(new XWikiServletRequestStub());
        xcontext.setResponse(new XWikiServletResponseStub());

        // Use a dummy URL so that XWiki's initialization can create a Servlet URL Factory. We could also have
        // registered a custom XWikiURLFactory against XWikiURLFactoryService but it's more work.
        xcontext.setURL(new URL("http://localhost/xwiki/bin/DummyAction/DumySpace/DummyPage"));

        // Set a dummy Document in the context to act as the current document since when a document containing
        // objects is imported it'll generate Object diff events and the algorithm to compute an object diff
        // currently requires rendering object properties, which requires a current document in the context.
        xcontext.setDoc(new XWikiDocument(new DocumentReference(databaseName, "dummySpace", "dummyPage")));

        XWikiConfig config = new XWikiConfig();
        config.put("xwiki.store.class", "com.xpn.xwiki.store.XWikiHibernateStore");

        // The XWikiConfig object requires path to be in unix format (i.e. with forward slashes)
        String hibernateConfigInUnixFormat = hibernateConfig.getPath().replace('\\', '/');
        config.put("xwiki.store.hibernate.path", hibernateConfigInUnixFormat);

        config.put("xwiki.store.hibernate.updateschema", "1");

        // Enable backlinks so that when documents are imported their backlinks will be saved too
        config.put("xwiki.backlinks", "1");

        XWiki xwiki = new XWiki(config, xcontext, null, true);

        xcontext.setUserReference(new DocumentReference("xwiki", "XWiki", "superadmin"));

        try {
            xcontext.setURLFactory(new XWikiServletURLFactory(new URL("http://localhost:8080"), "xwiki/", "bin/"));
        } catch (MalformedURLException e) {
            // TODO: Remove that way of creating exceptions in XWiki as it's a real plain and
            // doesn't work with external code.
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to set up URL Factory", e);
        }

        // Trigger extensions that need to initialize the database (create classes, etc.)
        xwiki.updateDatabase(xcontext.getMainXWiki(), xcontext);

        return xcontext;
    }

    /**
     * Free resources initialized by {@link #createXWikiContext(String, File)}.
     *
     * @param xcontext the XWiki context
     * @throws ComponentLookupException when failing to dispose component manager
     */
    public void disposeXWikiContext(XWikiContext xcontext) throws ComponentLookupException
    {
        ComponentManager componentManager = Utils.getRootComponentManager();

        // Remove ExecutionContext
        Execution execution = componentManager.getInstance(Execution.class);
        execution.removeContext();

        // Dispose component manager
        org.xwiki.environment.System.dispose(componentManager);

        Utils.setComponentManager(null);
    }

}
