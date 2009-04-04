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
 *
 */
package org.xwiki.platform.patchservice.web;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSParser;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.context.Execution;
import org.xwiki.platform.patchservice.api.Patch;
import org.xwiki.platform.patchservice.api.PatchId;
import org.xwiki.platform.patchservice.impl.PatchIdImpl;
import org.xwiki.platform.patchservice.impl.PatchImpl;
import org.xwiki.platform.patchservice.plugin.PatchservicePlugin;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletResponse;

/**
 * @version $Id$
 */
public class PatchServiceServlet extends HttpServlet implements Servlet
{
    /** Logging helper object. */
    private static final Log LOG = LogFactory.getLog(PatchServiceServlet.class);

    protected List<String> allVerbs;

    protected List<String> getVerbs;

    protected List<String> putVerbs;

    protected List<String> postVerbs;

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
     */
    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        this.allVerbs = new ArrayList<String>();
        this.allVerbs.add("all");
        this.allVerbs.add("id");
        this.allVerbs.add("updates");
        this.allVerbs.add("delta");
        this.allVerbs.add("keys");
        this.allVerbs.add("key");
        this.allVerbs.add("patch");
        this.getVerbs = new ArrayList<String>();
        this.getVerbs.add("all");
        this.getVerbs.add("id");
        this.getVerbs.add("updates");
        this.getVerbs.add("delta");
        this.getVerbs.add("keys");
        this.putVerbs = new ArrayList<String>();
        this.putVerbs.add("patch");
        this.putVerbs.add("key");
        this.postVerbs = new ArrayList<String>();
        this.postVerbs.add("id");
        this.postVerbs.add("updates");
        this.postVerbs.add("delta");
        this.postVerbs.add("patch");
        this.postVerbs.add("key");
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        handleRequest(req, resp, this.getVerbs);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        handleRequest(req, resp, this.postVerbs);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.servlet.http.HttpServlet#doPut(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        handleRequest(req, resp, this.putVerbs);
    }

    protected void handleRequest(HttpServletRequest req, HttpServletResponse resp, List<String> acceptedCommands)
    {
        XWikiContext context = null;
        try {
            // Get the command, between the first and the second /
            String command = req.getPathInfo();
            if (command != null) {
                command = command.substring(1);
                if (command.indexOf('/') > 0) {
                    command = command.substring(0, command.indexOf('/'));
                }
                // Check that this is something supported
                if (!this.allVerbs.contains(command)) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().print("Unknown command: " + command);
                    return;
                }
                // Check that the HTTP method was correct
                if (!acceptedCommands.contains(command)) {
                    resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    resp.getWriter().print("Bad HTTP method for command: " + command);
                    return;
                }
                context = initializeXWikiContext(command, req, resp);

                XWiki.getXWiki(context);
                execute(command, context);
            }
        } catch (Exception ex) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                resp.getWriter().print("Unhandled server error: " + ex.getMessage());
            } catch (IOException ex1) {
                // Cannot send message, ignore.
            }
            ex.printStackTrace();
        } finally {
            if (context != null) {
                cleanupComponents();
            }
        }
    }

    protected void execute(String command, XWikiContext context) throws Exception
    {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        PatchservicePlugin plugin = (PatchservicePlugin) context.getWiki().getPlugin("patchservice", context);
        if (command.equals("all")) {
            processAll(request, response, plugin, context);
        } else if (command.equals("patch")) {
            processPatch(request, response, plugin, context);
        } else if (command.equals("id")) {
            processId(request, response, plugin, context);
        } else if (command.equals("updates")) {
            processUpdates(request, response, plugin, context);
        } else if (command.equals("delta")) {
            processDelta(request, response, plugin, context);
        } else if (command.equals("keys")) {
            processKeys(request, response, plugin, context);
        } else if (command.equals("key")) {
            processKey(request, response, plugin, context);
        }
    }

    protected void processAll(XWikiRequest request, XWikiResponse response, PatchservicePlugin plugin,
        XWikiContext context) throws Exception
    {
        Document doc = createEmptyDocument();
        doc.appendChild(doc.createElement("patches"));
        for (Patch p : plugin.getAllPatches()) {
            doc.getDocumentElement().appendChild(p.toXml(doc));
        }
        outputXml(doc, response);
    }

    protected void processUpdates(XWikiRequest request, XWikiResponse response, PatchservicePlugin plugin,
        XWikiContext context) throws Exception
    {
        if (StringUtils.isEmpty(request.getParameter("patch_id"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("The request must contain a patch_id parameter.");
            return;
        }
        PatchId id = new PatchIdImpl();
        try {
            id.fromXml(parseString(request.getParameter("patch_id")).getDocumentElement());
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("Invalid patch_id parameter: " + ex.getMessage());
            return;
        }
        Document doc = createEmptyDocument();
        doc.appendChild(doc.createElement("patches"));
        for (Patch p : plugin.getUpdatesFrom(id)) {
            doc.getDocumentElement().appendChild(p.toXml(doc));
        }
        outputXml(doc, response);
    }

    protected void processDelta(XWikiRequest request, XWikiResponse response, PatchservicePlugin plugin,
        XWikiContext context) throws Exception
    {
        if (StringUtils.isEmpty(request.getParameter("patch_id_from"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("The request must contain a patch_id_from parameter.");
            return;
        }
        if (StringUtils.isEmpty(request.getParameter("patch_id_to"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("The request must contain a patch_id_from parameter.");
            return;
        }
        PatchId startId = new PatchIdImpl();
        try {
            startId.fromXml(parseString(request.getParameter("patch_id_from")).getDocumentElement());
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("Invalid patch_id_from parameter: " + ex.getMessage());
            return;
        }
        PatchId endId = new PatchIdImpl();
        try {
            endId.fromXml(parseString(request.getParameter("patch_id_to")).getDocumentElement());
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("Invalid patch_id_to parameter: " + ex.getMessage());
            return;
        }
        Document doc = createEmptyDocument();
        doc.appendChild(doc.createElement("patches"));
        for (Patch p : plugin.getDelta(startId, endId)) {
            doc.getDocumentElement().appendChild(p.toXml(doc));
        }
        outputXml(doc, response);
    }

    protected void processId(XWikiRequest request, XWikiResponse response, PatchservicePlugin plugin,
        XWikiContext context) throws Exception
    {
        if (StringUtils.isEmpty(request.getParameter("patch_id"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("The request must contain a patch_id parameter.");
            return;
        }
        Document doc = createEmptyDocument();
        PatchId id = new PatchIdImpl();
        try {
            id.fromXml(parseString(request.getParameter("patch_id")).getDocumentElement());
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("Invalid patch_id parameter: " + ex.getMessage());
            return;
        }
        Patch p = plugin.getPatch(id);
        if (p == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().print("No patch with the given id");
            return;
        }
        doc.appendChild(p.toXml(doc));
        outputXml(doc, response);
    }

    protected void processPatch(XWikiRequest request, XWikiResponse response, PatchservicePlugin plugin,
        XWikiContext context) throws Exception
    {
        if (StringUtils.isEmpty(request.getParameter("patch_id"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("The request must contain a patch_id parameter.");
            return;
        }
        if (StringUtils.isEmpty(request.getParameter("patch"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("The request must contain a patch parameter.");
            return;
        }
        PatchId id = new PatchIdImpl();
        try {
            id.fromXml(parseString(request.getParameter("patch_id")).getDocumentElement());
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("Invalid patch_id parameter: " + ex.getMessage());
            return;
        }
        Patch p = new PatchImpl();
        try {
            p.fromXml(parseString(request.getParameter("patch")).getDocumentElement());
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("Invalid patch parameter: " + ex.getMessage());
            return;
        }
        try {
            if (plugin.getDocumentUpdatesFrom(id).size() > 1) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.getWriter().print("needs update");
                return;
            } else {
                try {
                    plugin.applyPatch(p, id, context);
                    plugin.logPatch(p);
                    response.getWriter().print("OK");
                } catch (Exception ex) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().print("Failed to apply patch: " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            LOG.warn("Failed to send response");
        }
    }

    protected void processKeys(XWikiRequest request, XWikiResponse response, PatchservicePlugin plugin,
        XWikiContext context) throws Exception
    {
        response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
        response.getWriter().print("Not implemented yet");
    }

    protected void processKey(XWikiRequest request, XWikiResponse response, PatchservicePlugin plugin,
        XWikiContext context) throws Exception
    {
        response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
        response.getWriter().print("Not implemented yet");
    }

    private Document createEmptyDocument()
    {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Document parseString(String content)
    {
        try {
            DOMImplementationLS lsImpl =
                (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS 3.0");
            LSInput input = lsImpl.createLSInput();
            input.setCharacterStream(new StringReader(content));
            LSParser p = lsImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);
            return p.parse(input);
        } catch (Exception ex) {
            return null;
        }
    }

    private void outputXml(Document doc, XWikiResponse response)
    {
        response.setContentType("text/xml");
        DOMImplementationLS ls = (DOMImplementationLS) doc.getImplementation();
        try {
            LSOutput o = ls.createLSOutput();
            o.setByteStream(response.getOutputStream());
            ls.createLSSerializer().write(doc, o);
        } catch (Exception e) {
        }
    }

    protected XWikiContext initializeXWikiContext(String command, HttpServletRequest req, HttpServletResponse resp)
        throws XWikiException, ServletException
    {
        XWikiRequest request = new XWikiServletRequest(req);
        XWikiResponse response = new XWikiServletResponse(resp);
        XWikiContext context =
            Utils.prepareContext(command, request, response, new XWikiServletContext(this.getServletContext()));

        // Initialize the Container component which is the new of transporting the Context in the new
        // component architecture.
        initializeContainerComponent(context);

        return context;
    }

    protected void initializeContainerComponent(XWikiContext context) throws ServletException
    {
        // Initialize the Container fields (request, response, session).
        // Note that this is a bridge between the old core and the component architecture.
        // In the new component architecture we use ThreadLocal to transport the request,
        // response and session to components which require them.
        // In the future this Servlet will be replaced by the XWikiPlexusServlet Servlet.
        ServletContainerInitializer containerInitializer =
            (ServletContainerInitializer) Utils.getComponent(ServletContainerInitializer.class);

        try {
            containerInitializer.initializeRequest(context.getRequest().getHttpServletRequest(), context);
            containerInitializer.initializeResponse(context.getResponse().getHttpServletResponse());
            containerInitializer.initializeSession(context.getRequest().getHttpServletRequest());
        } catch (ServletContainerException e) {
            throw new ServletException("Failed to initialize Request/Response or Session", e);
        }
    }

    protected void cleanupComponents()
    {
        Container container = (Container) Utils.getComponent(Container.class);
        Execution execution = (Execution) Utils.getComponent(Execution.class);

        // We must ensure we clean the ThreadLocal variables located in the Container and Execution
        // components as otherwise we will have a potential memory leak.
        container.removeRequest();
        container.removeResponse();
        container.removeSession();
        execution.removeContext();
    }
}
