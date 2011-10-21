package org.xwiki.rest.resources.importfile;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.packaging.PackageAPI;

/**
 * Entry point to be able to import xar packages.
 * 
 * @version $Id$
 */
@Component("org.xwiki.rest.resources.xar.ImportResource")
@Path("/wikis/{wikiName}/import")
public class ImportResource extends XWikiResource
{
    @PUT
    public void importXar(@PathParam("wikiName") String wikiName,
        @QueryParam("backup") @DefaultValue("false") boolean backup,
        @QueryParam("history") @DefaultValue("add") String history, InputStream content) throws IOException,
        XWikiException
    {
        XWikiContext xcontext = getXWikiContext();
        String originalDatabase = xcontext.getDatabase();

        try {
            xcontext.setDatabase(wikiName);

            PackageAPI importer = (PackageAPI) Utils.getXWikiApi(componentManager).getPlugin("package");
            if (importer == null) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                    "Can't access Package plugin API. Generally mean you don't have enough rights.");
            }

            importer.Import(content);

            // Set the appropriate strategy to handle versions
            if (StringUtils.equals(history, "reset")) {
                importer.setPreserveVersion(false);
                importer.setWithVersions(false);
            } else if (StringUtils.equals(history, "replace")) {
                importer.setPreserveVersion(false);
                importer.setWithVersions(true);
            } else {
                importer.setPreserveVersion(true);
                importer.setWithVersions(false);
            }

            // Set the backup pack option
            importer.setBackupPack(backup);

            importer.install();
        } finally {
            xcontext.setDatabase(originalDatabase);
        }
    }
}
