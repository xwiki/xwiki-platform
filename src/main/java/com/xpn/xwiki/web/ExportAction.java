package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.packaging.PackageAPI;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 11 dec. 2006
 * Time: 17:18:05
 * To change this template use File | Settings | File Templates.
 */
public class ExportAction extends XWikiAction {

    public String render(XWikiContext context) throws XWikiException {
        try {
            XWikiRequest request = context.getRequest();

            String history = request.get("history");
            String backup = request.get("backup");
            String author= request.get("author");
            String description = request.get("description");
            String licence = request.get("licence");
            String version = request.get("version");
            String name = request.get("name");
            String[] pages = request.getParameterValues("pages");
            boolean isBackup = ((pages==null)||(pages.length==0));


            if (!context.getWiki().getRightService().hasAdminRights(context)) {
                context.put("message", "needadminrights");
                return "exception";
            }

            if (name==null) {
                return "export";
            }

            PackageAPI export = ((PackageAPI) context.getWiki().getPluginApi("package", context));
            if ("true".equals(history))
                export.setWithVersions(true);
            else
                export.setWithVersions(false);

            if (author!=null)
                export.setAuthorName(author);

            if (description!=null)
                export.setDescription(description);

            if (licence!=null)
                export.setLicence(licence);

            if (version!=null)
                export.setVersion(version);

            if (name.trim().equals("")) {
                if (isBackup)
                    name = "backup";
                else
                    name = "export";
            }

            if ("true".equals(backup))
                export.setBackupPack(true);

            export.setName(name);

            if (isBackup)
                export.backupWiki();
            else {
                if (pages!=null) {
                    for (int i=0;i<pages.length;i++) {
                        String pageName = pages[i];
                        String defaultAction = request.get("action_" + pageName);
                        int iAction = 0;
                        try {
                            iAction = Integer.parseInt(defaultAction);
                        } catch (Exception e) {
                            iAction = 0;
                        }
                        export.add(pageName, iAction);
                    }
                }
                export.export();
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_EXPORT,
                    "Exception while exporting", e);
        }

        return null;
    }
}
