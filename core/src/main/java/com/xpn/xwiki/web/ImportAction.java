package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.plugin.packaging.PackageAPI;
import com.xpn.xwiki.plugin.packaging.DocumentInfo;
import com.xpn.xwiki.plugin.packaging.DocumentInfoAPI;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 11 dec. 2006
 * Time: 17:18:05
 * To change this template use File | Settings | File Templates.
 */
public class ImportAction extends XWikiAction {

    public String render(XWikiContext context) throws XWikiException {
        try {
            XWikiRequest request = context.getRequest();
            XWikiResponse response = context.getResponse();
            XWikiDocument doc = context.getDoc();
            String name =  request.get("name");
            String action =  request.get("action");
            String[] pages = request.getParameterValues("pages");

            if (!context.getWiki().getRightService().hasAdminRights(context)) {
                context.put("message", "needadminrights");
                return "exception";
            }

            if (name==null) {
                return "import";
            }

            PackageAPI importer = ((PackageAPI) context.getWiki().getPluginApi("package", context));

            // list files
            if ("getPackageInfos".equals(action)) {
                response.setContentType("text/xml");
                XWikiAttachment packFile = doc.getAttachment(name);
                importer.Import(packFile.getContent(context));
                String xml = importer.toXml();
                response.setContentLength(xml.getBytes().length);
                response.getWriter().write(xml);
                return null;
            } else if ("import".equals(action)) {
                XWikiAttachment packFile = doc.getAttachment(name);
                importer.Import(packFile.getContent(context));
                String all = request.get("all");
                if (!"1".equals(all)) {
                    if (pages!=null) {
                        List filelist = importer.getFiles();
                        for (int i=0;i<filelist.size();i++) {
                            DocumentInfoAPI dia = (DocumentInfoAPI) filelist.get(i);
                            dia.setAction(DocumentInfo.ACTION_SKIP);
                        }

                        for (int i=0;i<pages.length;i++) {
                            String pageName = pages[i];
                            String language = request.get("language_" + pageName);
                            String actionName =  "action_" + pageName;
                            if (language!=null)
                             actionName = "_" + language;
                            String defaultAction = request.get(actionName);
                            int iAction;
                            if ((defaultAction==null)||(defaultAction.equals(""))) {
                                iAction = DocumentInfo.ACTION_OVERWRITE;

                            } else {
                                try {
                                    iAction = Integer.parseInt(defaultAction);
                                } catch (Exception e) {
                                    iAction = DocumentInfo.ACTION_SKIP;
                                }
                            }

                            String docName = pageName.replaceAll(":.*$", "");
                            if (language==null)
                             importer.setDocumentAction(docName, iAction);
                            else
                             importer.setDocumentAction(docName, language, iAction);
                        }
                    }
                    // Import files
                    importer.install();
                    return "import";
                }
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_EXPORT,
                    "Exception while importing", e);
        }
        return null;
    }
}
