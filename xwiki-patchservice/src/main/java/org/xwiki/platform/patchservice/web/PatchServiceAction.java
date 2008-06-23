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

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.xwiki.platform.patchservice.api.Patch;
import org.xwiki.platform.patchservice.api.PatchId;
import org.xwiki.platform.patchservice.impl.PatchIdImpl;
import org.xwiki.platform.patchservice.impl.PatchImpl;
import org.xwiki.platform.patchservice.plugin.PatchservicePlugin;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.XWikiAction;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

public class PatchServiceAction extends XWikiAction
{
    /** Logging helper object. */
    private static final Log LOG = LogFactory.getLog(PatchServiceAction.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        try {
            XWikiRequest request = context.getRequest();
            XWikiResponse response = context.getResponse();
            PatchservicePlugin plugin = (PatchservicePlugin) context.getWiki().getPlugin("patchservice", context);
            String uri = request.getRequestURI();
            String paction = "patches/";
            System.err.println("uri: " + uri);
            String action = uri.substring(uri.indexOf(paction) + paction.length());
            if (action.startsWith("all")) {
                processAll(request, response, plugin, context);
            } else if (action.startsWith("patch")) {
                processPatch(request, response, plugin, context);
            } else if (action.startsWith("id")) {
                processId(request, response, plugin, context);
            } else if (action.startsWith("updates/id")) {
                processUpdates(request, response, plugin, context);
            } else if (action.startsWith("delta")) {
                processDelta(request, response, plugin, context);
            } else if (action.startsWith("keys")) {
                processKeys(request, response, plugin, context);
            } else if (action.startsWith("key")) {
                processKey(request, response, plugin, context);
            }
        } catch (Exception ex) {
            LOG.warn("Unhandled exception: " + ex.getMessage(), ex);
            try {
                context.getResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                context.getResponse().getWriter().print("Unhandled exception: " + ex.getMessage());
            } catch (Exception ex2) {
                // Cannot send an error message...
            }
        }
        return false;
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
            response.getWriter().write(ls.createLSSerializer().writeToString(doc));
        } catch (Exception e) {
        }
    }
}
