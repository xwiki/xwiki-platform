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
 */
package com.xpn.xwiki.plugin.svg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Vector;

import org.apache.batik.apps.rasterizer.DestinationType;
import org.apache.batik.apps.rasterizer.SVGConverter;
import org.apache.batik.apps.rasterizer.SVGConverterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.web.XWikiResponse;

public class SVGPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface
{
    private static Logger LOGGER = LoggerFactory.getLogger(com.xpn.xwiki.plugin.svg.SVGPlugin.class);

    private File tempDir;

    public SVGPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
    }

    @Override
    public String getName()
    {
        return "svg";
    }

    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new SVGPluginApi((SVGPlugin) plugin, context);
    }

    @Override
    public void flushCache()
    {
        try {
            File[] filelist = tempDir.listFiles();
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
        tempDir = new File(dir, "svg");
        try {
            tempDir.mkdirs();
        } catch (Exception ex) {
            LOGGER.warn("Cannot create temporary files", ex);
        }
    }

    public byte[] getSVGImage(String content, int height, int width) throws IOException, SVGConverterException
    {
        return getSVGImage(content, "png", height, width);
    }

    public byte[] getSVGImage(String content, String extension, int height, int width) throws IOException,
        SVGConverterException
    {
        int hashCode = Math.abs(content.hashCode());
        return getSVGImage(hashCode, content, extension, height, width);
    }

    public byte[] getSVGImage(int hashCode, String content, String extension, int height, int width)
        throws IOException, SVGConverterException
    {
        File dfile = getTempFile(hashCode, "svg");
        if (!dfile.exists()) {
            FileWriter fwriter = new FileWriter(dfile);
            fwriter.write(content);
            fwriter.flush();
            fwriter.close();
        }

        File ofile = getTempFile(hashCode, extension);
        // TODO implement conversion HERE

        SVGConverter conv = new SVGConverter();
        // TODO PNG ONLY
        conv.setDestinationType(DestinationType.PNG);
        conv.setDst(ofile);
        conv.setHeight(height);
        conv.setWidth(width);
        String[] sources = {dfile.getAbsolutePath()};
        conv.setSources(sources);
        conv.execute();

        FileInputStream fis = new FileInputStream(ofile);
        byte[] result = new byte[(int) ofile.length()];
        fis.read(result);
        return result;
    }

    protected String[] expandSources(Vector sources)
    {
        Vector expandedSources = new Vector();
        Iterator iter = sources.iterator();
        while (iter.hasNext()) {
            String v = (String) iter.next();
            File f = new File(v);
            if (f.exists() && f.isDirectory()) {
                File[] fl = f.listFiles(new SVGConverter.SVGFileFilter());
                for (int i = 0; i < fl.length; i++) {
                    expandedSources.addElement(fl[i].getPath());
                }
            } else {
                expandedSources.addElement(v);
            }
        }

        String[] s = new String[expandedSources.size()];
        expandedSources.copyInto(s);
        return s;
    }

    public byte[] readSVGImage(File ofile) throws IOException
    {
        FileInputStream fis = new FileInputStream(ofile);
        byte[] result = new byte[(int) ofile.length()];
        fis.read(result);
        return result;

    }

    public String writeSVGImage(String content, int height, int width) throws IOException, SVGConverterException
    {
        return writeSVGImage(content, "png", height, width);
    }

    public String writeSVGImage(String content, String extension, int height, int width) throws IOException,
        SVGConverterException
    {
        int hashCode = Math.abs(content.hashCode());
        getSVGImage(hashCode, content, extension, height, width);
        return hashCode + "." + extension;
    }

    public void outputSVGImage(String content, int height, int width, XWikiContext context) throws IOException,
        SVGConverterException
    {
        outputSVGImage(content, "png", height, width, context);
    }

    public void outputSVGImage(String content, String extension, int height, int width, XWikiContext context)
        throws IOException, SVGConverterException
    {
        byte[] svgbytes = getSVGImage(content, extension, height, width);
        XWikiResponse response = context.getResponse();
        context.setFinished(true);
        response.setContentLength(svgbytes.length);
        response.setContentType(context.getEngineContext().getMimeType("toto." + extension));
        OutputStream os = response.getOutputStream();
        os.write(svgbytes);
        os.flush();
    }

    public void outputSVGImageFromFile(String filename, XWikiContext context) throws IOException
    {
        File ofile = getTempFile(filename);
        byte[] svgbytes = readSVGImage(ofile);
        XWikiResponse response = context.getResponse();
        context.setFinished(true);
        response.setDateHeader("Last-Modified", ofile.lastModified());
        response.setContentLength(svgbytes.length);
        response.setContentType(context.getEngineContext().getMimeType(filename));
        OutputStream os = response.getOutputStream();
        os.write(svgbytes);
    }

    public File getTempFile(String filename)
    {
        return new File(tempDir, filename);
    }

    public File getTempFile(int hashcode, String extension)
    {
        return getTempFile(hashcode + "." + extension);
    }

    public String getSVGImageURL(String content, int height, int width, XWikiContext context) throws IOException,
        SVGConverterException
    {
        String filename = writeSVGImage(content, "png", height, width);
        return context.getDoc().getAttachmentURL(filename, "svg", context);
    }
}
