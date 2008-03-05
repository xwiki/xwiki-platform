package org.xwiki.platform.patchservice.web;

import java.io.StringReader;
import java.util.List;

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
     * 
     * @todo Write me!
     */
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        PatchservicePlugin plugin =
            (PatchservicePlugin) context.getWiki().getPlugin("patchservice", context);
        String uri = request.getRequestURI();
        String paction = "patches/";
        String action = uri.substring(uri.indexOf(paction) + paction.length());
        if (action.equals("all")) {
            Document doc = createEmptyDocument();
            doc.appendChild(doc.createElement("patches"));
            List<Patch> patches = plugin.getStorage().loadAllPatches();
            for (Patch p : patches) {
                doc.getDocumentElement().appendChild(p.toXml(doc));
            }
            outputXml(doc, response);
        } else if (action.equals("id")) {
            Document doc = createEmptyDocument();
            doc.appendChild(doc.createElement("patches"));
            PatchId id = new PatchIdImpl();
            id.fromXml(parseString(request.getParameter("patch_id")).getDocumentElement());
            List<Patch> patches = plugin.getStorage().loadAllPatchesSince(id);
            for (Patch p : patches) {
                doc.getDocumentElement().appendChild(p.toXml(doc));
            }
            outputXml(doc, response);
        } else if (action.equals("delta")) {
            Document doc = createEmptyDocument();
            doc.appendChild(doc.createElement("patches"));
            PatchId startId = new PatchIdImpl();
            startId.fromXml(parseString(request.getParameter("patch_id_from"))
                .getDocumentElement());
            PatchId endId = new PatchIdImpl();
            endId.fromXml(parseString(request.getParameter("patch_id_to")).getDocumentElement());
            List<Patch> patches = plugin.getStorage().loadAllPatchesSince(startId);
            for (Patch p : patches) {
                if (p.getId().getTime().before(endId.getTime())) {
                    doc.getDocumentElement().appendChild(p.toXml(doc));
                }
            }
            outputXml(doc, response);
        }
        return false;
    }

    private Document createEmptyDocument()
    {
        try {
            return (Document) DOMImplementationRegistry.newInstance().getDOMImplementation("")
                .createDocument(null, null, null);
        } catch (Exception e) {
            return null;
        }
    }

    private Document parseString(String content)
    {
        try {
            DOMImplementationLS lsImpl =
                (DOMImplementationLS) DOMImplementationRegistry.newInstance()
                    .getDOMImplementation("LS 3.0");
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
