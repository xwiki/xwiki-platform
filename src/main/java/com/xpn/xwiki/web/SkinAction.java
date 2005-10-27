package com.xpn.xwiki.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public class SkinAction extends XWikiAction {
	public String render(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        String path = request.getPathInfo();
        String filename = Utils.decode(path.substring(path.lastIndexOf("/")+1),context);

        if (renderSkin(filename, doc, context))
            return null;

        String baseskin = xwiki.getBaseSkin(context, true);
        if (renderSkin(filename, baseskin, context))
            return null;

        XWikiDocument baseskindoc = xwiki.getDocument(baseskin, context);
        if (renderSkin(filename, baseskindoc, context))
                    return null;

        String defaultbaseskin = xwiki.getDefaultBaseSkin(context);
        renderSkin(filename, defaultbaseskin, context);
        return null;
	}
	
    private boolean renderSkin(String filename, XWikiDocument doc, XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiResponse response = context.getResponse();

        BaseObject object = doc.getObject("XWiki.XWikiSkins", 0);
        String content = null;
        if (object!=null) {
            content = object.getStringValue(filename);
        }

        if ((content!=null)&&(!content.equals(""))) {
            // Choose the right content type
            response.setContentType(xwiki.getEngineContext().getMimeType(filename.toLowerCase()));
            response.setDateHeader("Last-Modified", doc.getDate().getTime());
            // Sending the content of the attachment
            response.setContentLength(content.length());
            try {
                response.getWriter().write(content);
                return true;
            } catch (IOException e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                        XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                        "Exception while sending response", e);
            }
        }
        else {
            XWikiAttachment attachment = doc.getAttachment(filename);
            if (attachment!=null) {
                // Sending the content of the attachment
                byte[] data = attachment.getContent(context);
                response.setContentType(xwiki.getEngineContext().getMimeType(filename.toLowerCase()));
                response.setDateHeader("Last-Modified", attachment.getDate().getTime());
                response.setContentLength(data.length);
                try {
                    response.getOutputStream().write(data);
                    return true;
                } catch (IOException e) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                            XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                            "Exception while sending response", e);
                }
            }
        }
        return false;
    }
    
    private boolean renderSkin(String filename, String skin, XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiResponse response = context.getResponse();
        try {
            response.setDateHeader("Expires", (new Date()).getTime() + 30*24*3600*1000L);
            String path = "/skins/" + skin + "/" + filename;
            // Choose the right content type
            String mimetype = context.getEngineContext().getMimeType(filename.toLowerCase());
            if (mimetype!=null)
                response.setContentType(mimetype);
            else
                response.setContentType("application/octet-stream");

            // Sending the content of the file
            InputStream is  = context.getWiki().getResourceAsStream(path);
            if (is==null)
             return false;

            int nRead = 0;
            byte[] data = new byte[65535];
            while ((nRead = is.read(data)) !=-1) {
                response.getOutputStream().write(data,0,nRead);
            }
            return true;
        } catch (IOException e) {
            if (skin.equals(xwiki.getDefaultBaseSkin(context)))
             throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                    "Exception while sending response", e);
            else
             return false;
        }
    }
}
