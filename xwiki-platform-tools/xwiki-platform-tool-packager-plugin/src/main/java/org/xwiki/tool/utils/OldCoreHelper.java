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
package org.xwiki.tool.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.maven.plugin.MojoExecutionException;
import org.hibernate.cfg.Environment;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.embed.EmbeddableComponentManager;
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
 * Helper to manipulate oldcore APIs.
 * 
 * @version $Id$
 * @since 9.5RC1
 */
@Component(roles = OldCoreHelper.class)
@Singleton
public class OldCoreHelper implements AutoCloseable
{
    @Inject
    private Execution execution;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private ExecutionContextManager ecim;

    private boolean disposeComponentManager;

    private String wikiId;

    private File hibernateConfig;

    private XWikiContext xcontext;

    /**
     * Public for technical reason, {@link #create(String, File)} should be used instead.
     */
    public OldCoreHelper()
    {

    }

    /**
     * @param wikiId id of the wiki for which to prepare the XWiki Context (e.g. {@code xwiki})
     * @param hibernateConfig the Hibernate config fill containing the database definition (JDBC driver, username and
     *            password, etc)
     * @return an initialized instance of {@link OldCoreHelper}
     * @throws MojoExecutionException when failing to initialize {@link OldCoreHelper} instance
     */
    public static OldCoreHelper create(String wikiId, File hibernateConfig) throws MojoExecutionException
    {
        // Create and initialize a Component Manager
        EmbeddableComponentManager embeddableComponentManager =
            (EmbeddableComponentManager) org.xwiki.environment.System.initialize();

        // Initialize Execution Context
        try {
            ExecutionContextManager ecim = embeddableComponentManager.getInstance(ExecutionContextManager.class);
            ecim.initialize(new ExecutionContext());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to initialize Execution Context Manager.", e);
        }

        OldCoreHelper oldCoreHelper = create(embeddableComponentManager, wikiId, hibernateConfig);

        oldCoreHelper.disposeComponentManager = true;

        return oldCoreHelper;
    }

    /**
     * @param componentManager the component manager
     * @param wikiId id of the wiki for which to prepare the XWiki Context (e.g. {@code xwiki})
     * @param hibernateConfig the Hibernate config fill containing the database definition (JDBC driver, username and
     *            password, etc)
     * @return an initialized instance of {@link OldCoreHelper}
     * @throws MojoExecutionException when failing to initialize {@link OldCoreHelper} instance
     */
    public static OldCoreHelper create(ComponentManager componentManager, String wikiId, File hibernateConfig)
        throws MojoExecutionException
    {
        // Lookup OldCoreHelper
        OldCoreHelper oldcoreHelper;
        try {
            oldcoreHelper = componentManager.getInstance(OldCoreHelper.class);
        } catch (ComponentLookupException e) {
            throw new MojoExecutionException("Failed to get OldCoreHelper component", e);
        }

        // Initialize OldCoreHelper
        try {
            oldcoreHelper.initialize(wikiId, hibernateConfig);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to get initialize OldCoreHelper component", e);
        }

        return oldcoreHelper;
    }

    private void initialize(String wikiId, File hibernateConfig) throws Exception
    {
        this.wikiId = wikiId;
        this.hibernateConfig = hibernateConfig;

        this.xcontext = createXWikiContext();
    }

    @Override
    public void close() throws Exception
    {
        Utils.setComponentManager(null);

        if (this.disposeComponentManager) {
            this.execution.removeContext();

            org.xwiki.environment.System.dispose(this.componentManager);
        }
    }

    /**
     * @return the XWiki context
     */
    public XWikiContext getXWikiContext()
    {
        return xcontext;
    }

    /**
     * @param wikiId id of the wiki for which to prepare the XWiki Context (e.g. {@code xwiki})
     * @param hibernateConfig the Hibernate config fill containing the database definition (JDBC driver, username and
     *            password, etc)
     * @return a valid XWikiContext using the passed Hibernate configuration and passed database name
     * @throws Exception failed to initialize context.
     */
    // TODO: Replace the Hibernate config file with a list of parameters required for the packaging operation
    private XWikiContext createXWikiContext() throws Exception
    {
        Utils.setComponentManager(this.componentManager);

        this.xcontext = new XWikiContext();
        this.xcontext.put(ComponentManager.class.getName(), this.componentManager);

        // Initialize the Container fields (request, response, session).
        try {
            ExecutionContext econtext = new ExecutionContext();

            // Bridge with old XWiki Context, required for old code.
            this.xcontext.declareInExecutionContext(econtext);

            this.ecim.initialize(econtext);
        } catch (ExecutionContextException e) {
            throw new Exception("Failed to initialize Execution Context.", e);
        }

        this.xcontext.setWikiId(wikiId);
        this.xcontext.setMainXWiki(wikiId);

        // Use a dummy URL so that XWiki's initialization can create a Servlet URL Factory. We could also have
        // registered a custom XWikiURLFactory against XWikiURLFactoryService but it's more work.
        this.xcontext.setURL(new URL("http://localhost/xwiki/bin/DummyAction/DumySpace/DummyPage"));

        // Use a dummy Request/Response even in daemon mode so that XWiki's initialization can create a Servlet URL
        // Factory and any code requiring those objects will work.
        this.xcontext.setRequest(new XWikiServletRequestStub(this.xcontext.getURL(), Collections.emptyMap()));
        this.xcontext.setResponse(new XWikiServletResponseStub());

        // Set a dummy Document in the context to act as the current document since when a document containing
        // objects is imported it'll generate Object diff events and the algorithm to compute an object diff
        // currently requires rendering object properties, which requires a current document in the context.
        this.xcontext.setDoc(new XWikiDocument(new DocumentReference(wikiId, "dummySpace", "dummyPage")));

        XWikiConfig config = new XWikiConfig();
        config.put("xwiki.store.class", "com.xpn.xwiki.store.XWikiHibernateStore");

        // The XWikiConfig object requires path to be in unix format (i.e. with forward slashes)
        String hibernateConfigInUnixFormat = hibernateConfig.getPath().replace('\\', '/');
        config.put("xwiki.store.hibernate.path", hibernateConfigInUnixFormat);

        config.put("xwiki.store.hibernate.updateschema", "1");

        // Enable backlinks so that when documents are imported their backlinks will be saved too
        config.put("xwiki.backlinks", "1");

        XWiki xwiki = new XWiki(config, this.xcontext, null, true);

        this.xcontext.setUserReference(new DocumentReference("xwiki", "XWiki", "superadmin"));

        try {
            this.xcontext.setURLFactory(new XWikiServletURLFactory(new URL("http://localhost:8080"), "xwiki/", "bin/"));
        } catch (MalformedURLException e) {
            // TODO: Remove that way of creating exceptions in XWiki as it's a real plain and
            // doesn't work with external code.
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to set up URL Factory", e);
        }

        // Trigger extensions that need to initialize the database (create classes, etc.)
        xwiki.initializeWiki(this.xcontext.getMainXWiki(), true, this.xcontext);

        return this.xcontext;
    }
}
