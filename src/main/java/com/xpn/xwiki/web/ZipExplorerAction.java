package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.ArchiveAttachmentExplorer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Created by IntelliJ IDEA.
 * User: ravenees
 * Date: Sep 27, 2006
 * Time: 5:03:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class ZipExplorerAction extends XWikiAction{

    /**
     *
     * Gives the rennderer i.e the response templete (zipexplorer.vm)
     * */
    public String render(XWikiContext context) throws XWikiException {
        return "zipexplorer";
    }

    /**
     * ZIPEXPLORERACTION :- is to explore the archive files with extension .zip
     *
     * */
    public boolean action(XWikiContext context) throws XWikiException {
        System.out.println("((((((( inside ZipExplorerAction )))))))");

        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        XWikiAttachment attachment = null;
        /*
         The complete URL String including the filename
        */
        String path = request.getPathInfo();
        String filename;
        if (context.getMode() == XWikiContext.MODE_PORTLET){
            /*
             Getting the file name from the request parameter "filename"
            */
            filename = request.getParameter("filename");
            
        }
        else{
            /*
            Parsing the URL to get the filename
            */
            filename = Utils.decode(path.substring(path.lastIndexOf("/") + 1), context);
        }

        /*
        Getting the attachment form the attachment list
        */

        request.setAttribute("zipfile",filename);

        if (request.getParameter("id") != null) {
            int id = Integer.parseInt(request.getParameter("id"));
            attachment = (XWikiAttachment) doc.getAttachmentList().get(id);
        } else {
            attachment = doc.getAttachment(filename);
        }
        return true;
    }
}
