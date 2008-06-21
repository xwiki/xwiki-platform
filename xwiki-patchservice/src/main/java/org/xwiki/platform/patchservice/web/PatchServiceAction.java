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

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.xwiki.platform.patchservice.api.Patch;
import org.xwiki.platform.patchservice.api.PatchId;
import org.xwiki.platform.patchservice.impl.PatchIdImpl;
import org.xwiki.platform.patchservice.plugin.PatchservicePlugin;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.XWikiAction;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

public class PatchServiceAction extends XWikiAction
{
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        response.setContentType("text/xml");
        PatchservicePlugin plugin = (PatchservicePlugin) context.getWiki().getPlugin("patchservice", context);
        String uri = request.getRequestURI();
        String paction = "patches/";
        System.err.println("uri: " + uri);
        String action = uri.substring(uri.indexOf(paction) + paction.length());
        if (action.startsWith("all")) {
            System.err.println("generating all patches...");
            Document doc = createEmptyDocument();
            doc.appendChild(doc.createElement("patches"));
            for (Patch p : plugin.getAllPatches()) {
                doc.getDocumentElement().appendChild(p.toXml(doc));
            }
            outputXml(doc, response);
        } else if (action.startsWith("id")) {
            Document doc = createEmptyDocument();
            doc.appendChild(doc.createElement("patches"));
            PatchId id = new PatchIdImpl();
            id.fromXml(parseString(request.getParameter("patch_id")).getDocumentElement());
            for (Patch p : plugin.getUpdatesFrom(id)) {
                doc.getDocumentElement().appendChild(p.toXml(doc));
            }
            outputXml(doc, response);
        } else if (action.startsWith("delta")) {
            Document doc = createEmptyDocument();
            doc.appendChild(doc.createElement("patches"));
            PatchId startId = new PatchIdImpl();
            startId.fromXml(parseString(request.getParameter("patch_id_from")).getDocumentElement());
            PatchId endId = new PatchIdImpl();
            endId.fromXml(parseString(request.getParameter("patch_id_to")).getDocumentElement());
            for (Patch p : plugin.getDelta(startId, endId)) {
                doc.getDocumentElement().appendChild(p.toXml(doc));
            }
            outputXml(doc, response);
        }
        return false;
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
        DOMImplementationLS ls = (DOMImplementationLS) doc.getImplementation();
        try {
            response.getWriter().write(ls.createLSSerializer().writeToString(doc));
        } catch (Exception e) {
        }
    }
}
