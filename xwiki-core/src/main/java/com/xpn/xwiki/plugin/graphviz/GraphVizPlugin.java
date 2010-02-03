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

package com.xpn.xwiki.plugin.graphviz;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.web.XWikiResponse;

public class GraphVizPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface
{
    private static Log mLogger = LogFactory.getLog(com.xpn.xwiki.plugin.graphviz.GraphVizPlugin.class);

    private File tempDir;

    private String dotPath;

    private String neatoPath;

    public GraphVizPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
    }

    @Override
    public String getName()
    {
        return "graphviz";
    }

    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new GraphVizPluginApi((GraphVizPlugin) plugin, context);
    }

    @Override
    public void flushCache()
    {
        try {
            File[] filelist = this.tempDir.listFiles();
            for (int i = 0; i < filelist.length; i++) {
                try {
                    filelist[i].delete();
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void init(XWikiContext context)
    {
        super.init(context);

        File dir = context.getWiki().getTempDirectory(context);
        this.tempDir = new File(dir, "dot");
        try {
            this.tempDir.mkdirs();
        } catch (Exception ex) {
            mLogger.warn("Failed to create temporary file", ex);
        }

        this.dotPath = context.getWiki().Param("xwiki.plugin.graphviz.dotpath", "dot");
        if (!this.dotPath.equals("dot")) {
            try {
                File dfile = new File(this.dotPath);
                if (!dfile.exists()) {
                    mLogger.error("Cannot find graphiz dot program at " + this.dotPath);
                }
            } catch (Exception e) {
            }
        }

        this.neatoPath = context.getWiki().Param("xwiki.plugin.graphviz.neatopath", "neato");
        if (!this.neatoPath.equals("neato")) {
            try {
                File dfile = new File(this.neatoPath);
                if (!dfile.exists()) {
                    mLogger.error("Cannot find graphiz neato program at " + this.neatoPath);
                }
            } catch (Exception e) {
            }
        }

    }

    public byte[] getDotImage(String content, boolean dot) throws IOException
    {
        return getDotImage(content, "png", dot);
    }

    public byte[] getDotImage(String content, String extension, boolean dot) throws IOException
    {
        int hashCode = Math.abs(content.hashCode());
        return getDotImage(hashCode, content, extension, dot);
    }

    public byte[] getDotImage(int hashCode, String content, String extension, boolean dot) throws IOException
    {
        File dfile = getTempFile(hashCode, "dot", dot);
        if (!dfile.exists()) {
            FileWriter fwriter = new FileWriter(dfile);
            fwriter.write(content);
            fwriter.flush();
            fwriter.close();
        }

        File ofile = getTempFile(hashCode, extension, dot);
        if (!ofile.exists()) {
            Runtime rt = Runtime.getRuntime();
            String[] command = new String[5];
            command[0] = dot ? this.dotPath : this.neatoPath;
            command[1] = "-T" + extension;
            command[2] = dfile.getAbsolutePath();
            command[3] = "-o";
            command[4] = ofile.getAbsolutePath();
            Process p = rt.exec(command);
            int exitValue = -1;
            try {
                int i = 0;
                int max = 10;
                for (i = 0; i < max; i++) {
                    Thread.sleep(1000);
                    try {
                        // exitValue() throws an IllegalThreadStateException if the process is still running. 
                        exitValue = p.exitValue();
                        break;
                    } catch (IllegalThreadStateException e) {
                    }
                }
                // No more than 10 seconds to generate graph
                // Force killing
                if (i >= max) {
                    p.destroy();
                }
            } catch (InterruptedException e) {
                mLogger.error("Error while generating image from dot", e);
            }

            if (exitValue != 0) {
                BufferedReader os = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                StringBuffer error = new StringBuffer();
                while (os.ready()) {
                    error.append(os.readLine());
                }
                mLogger.error("Error while generating image from dot: " + error.toString());
            }
        }
        FileInputStream fis = new FileInputStream(ofile);
        try {
            byte[] result = new byte[(int) ofile.length()];
            fis.read(result);
            return result;
        } finally {
            fis.close();
        }
    }

    public byte[] readDotImage(File ofile) throws FileNotFoundException, IOException
    {
        FileInputStream fis = new FileInputStream(ofile);
        byte[] result = new byte[(int) ofile.length()];
        fis.read(result);
        return result;

    }

    public String writeDotImage(String content, boolean dot) throws IOException
    {
        return writeDotImage(content, "png", dot);
    }

    public String writeDotImage(String content, String extension, boolean dot) throws IOException
    {
        int hashCode = Math.abs(content.hashCode());
        getDotImage(hashCode, content, extension, dot);
        String name = dot ? "dot-" : "neato-";
        return name + hashCode + "." + extension;
    }

    public void outputDotImage(String content, boolean dot, XWikiContext context) throws IOException
    {
        outputDotImage(content, "png", dot, context);
    }

    public void outputDotImage(String content, String extension, boolean dot, XWikiContext context) throws IOException
    {
        byte[] dotbytes = getDotImage(content, extension, dot);
        XWikiResponse response = context.getResponse();
        context.setFinished(true);
        response.setContentLength(dotbytes.length);
        response.setContentType(context.getEngineContext().getMimeType("toto." + extension));
        OutputStream os = response.getOutputStream();
        os.write(dotbytes);
        os.flush();
    }

    public void outputDotImageFromFile(String filename, XWikiContext context) throws IOException
    {
        File ofile = getTempFile(filename);
        byte[] dotbytes = readDotImage(ofile);
        XWikiResponse response = context.getResponse();
        context.setFinished(true);
        response.setDateHeader("Last-Modified", ofile.lastModified());
        response.setContentLength(dotbytes.length);
        response.setContentType(context.getEngineContext().getMimeType(filename));
        OutputStream os = response.getOutputStream();
        os.write(dotbytes);
    }

    public File getTempFile(String filename)
    {
        return new File(this.tempDir, filename);
    }

    public File getTempFile(int hashcode, String extension, boolean dot)
    {
        String name = dot ? "dot-" : "neato-";
        return getTempFile(name + hashcode + "." + extension);
    }

    public String getDotImageURL(String content, boolean dot, XWikiContext context) throws IOException
    {
        return getDotResultURL(content, dot, "png", context);
    }

    /**
     * <p>
     * Executes graphviz and returns the url for the produced file.
     * </p>
     * 
     * @param content GraphViz source code. View http://www.graphviz.org/doc/info/lang.html for the language
     *            specification.
     * @param dot Whether the dot engine should be used instead of the neato engine. Other engines are not supported.
     * @param format Any GraphViz output format. View http://www.graphviz.org/doc/info/output.html for more information.
     * @param context XWikiContext
     */
    public String getDotResultURL(String content, boolean dot, String format, XWikiContext context) throws IOException
    {
        String filename = writeDotImage(content, format, dot);
        return context.getDoc().getAttachmentURL(filename, "dot", context);
    }
}
