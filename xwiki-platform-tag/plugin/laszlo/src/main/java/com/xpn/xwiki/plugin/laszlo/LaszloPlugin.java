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
 * @author ludovic
 * @author sdumitriu
 */

package com.xpn.xwiki.plugin.laszlo;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LaszloPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface {
    private static Log mLogger =
            LogFactory.getFactory().getInstance(com.xpn.xwiki.plugin.laszlo.LaszloPlugin.class);

    private String laszloBaseURL;
    private String laszloPath;

    public LaszloPlugin(String name, String className, XWikiContext context) {
        super(name, className, context);
        init(context);
    }

    public String getName() {
        return "laszlo";
    }

    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
        return new LaszloPluginApi((LaszloPlugin) plugin, context);
    }
    
    public void flushCache() {
        try {
            File[] filelist = (new File(laszloPath)).listFiles();
            for (int i=0;i<filelist.length;i++) {
                try {
                    filelist[i].delete();
                } catch (Exception e) {}
            }
        } catch (Exception e) {}
    }

    public void init(XWikiContext context) {
        super.init(context);

        laszloBaseURL = context.getWiki().Param("xwiki.plugin.laszlo.baseurl", "/openlaszlo/xwiki/");
        if (!laszloBaseURL.endsWith("/"))
            laszloBaseURL = laszloBaseURL + "/";

        laszloPath = context.getWiki().Param("xwiki.plugin.laszlo.path", "./webapps/openlaszlo/xwiki/");
        if (!laszloPath.endsWith("/"))
            laszloPath = laszloPath + "/";

        flushCache();
    }

    public String writeLaszloFile(String name, String laszlocode) throws IOException, XWikiException {
        File laszloDir = new File(laszloPath);
        if (!laszloDir.exists())
            laszloDir.mkdirs();

        String filename = getFileName(name, laszlocode);

        laszlocode = Util.secureLaszloCode(laszlocode);

        File dfile = new File(laszloDir, filename);
        if (!dfile.exists()) {
            FileWriter fwriter = new FileWriter(dfile);
            fwriter.write(laszlocode);
            fwriter.flush();
            fwriter.close();
        }

        return filename;
    }

    public String getFileName(String name, String laszlocode) {
        if ((name==null)||name.trim().equals(""))
            name = "laszlo";

        String filename = name + "-" + Math.abs(laszlocode.hashCode()) + ".lzx";
        return filename;
    }

    public String getLaszloURL(String name, String laszlocode) throws IOException, XWikiException {
        String filename = writeLaszloFile(name, laszlocode);
        return laszloBaseURL + filename + "?lzt=swf";
    }

    public String getLaszloFlash(String name, String width, String height, String laszlocode, XWikiContext context) throws IOException, XWikiException {
        String url = getLaszloURL(name, laszlocode);
        return context.getWiki().getFlash(url, width, height, context);
    }
}
