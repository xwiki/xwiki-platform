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
package com.xpn.xwiki.plugin.graphviz;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.environment.Environment;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiResponse;

/**
 * Plugin which wraps the <a href="http://graphviz.org/">GraphViz</a> <tt>dot</tt> executable; transforming dot source
 * files (representing graphs) into images, image maps, or other output formats supported by GraphViz.
 * <p>
 * See http://www.graphviz.org/doc/info/lang.html for the dot language specification. See
 * http://www.graphviz.org/doc/info/output.html for the possible output formats
 * </p>
 *
 * @deprecated the plugin technology is deprecated
 * @version $Id$
 */
@Deprecated
public class GraphVizPlugin extends XWikiDefaultPlugin
{
    /** Detects HTML character references produced by the {@link com.xpn.xwiki.render.filter.EscapeFilter}. */
    private static final Pattern HTML_ESCAPE_PATTERN = Pattern.compile("&#([0-9]++);");

    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(com.xpn.xwiki.plugin.graphviz.GraphVizPlugin.class);

    /** The default output format to use: PNG image. */
    private static final String DEFAULT_FORMAT = "png";

    /** The default engine to use: dot. */
    private static final String DOT_ENGINE = "dot";

    /** An alternative engine to use: neato. */
    private static final String NEATO_ENGINE = "neato";

    /** Temporary directory where generated files are stored. */
    private File tempDir;

    /** The path to the dot executable. */
    private String dotPath;

    /** The path to the neato executable. */
    private String neatoPath;

    /**
     * Used to get the temporary directory.
     */
    private Environment environment = Utils.getComponent((Type) Environment.class);

    /**
     * The mandatory plugin constructor, this is the method called (through reflection) by the plugin manager.
     *
     * @param name the plugin name
     * @param className the name of this class, ignored
     * @param context the current request context
     */
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
            FileUtils.cleanDirectory(this.tempDir);
        } catch (Exception e) {
            // Public APIs shouldn't throw errors; this shouldn't happen anyway
        }
    }

    @Override
    public void init(XWikiContext context)
    {
        super.init(context);

        File dir = this.environment.getTemporaryDirectory();
        this.tempDir = new File(dir, this.getName());
        try {
            this.tempDir.mkdirs();
        } catch (Exception ex) {
            LOGGER.warn("Failed to create temporary file", ex);
        }

        this.dotPath = context.getWiki().Param("xwiki.plugin.graphviz.dotpath", DOT_ENGINE);
        if (!this.dotPath.equals(DOT_ENGINE)) {
            try {
                File dfile = new File(this.dotPath);
                if (!dfile.exists()) {
                    LOGGER.error("Cannot find graphiz dot program at " + this.dotPath);
                }
            } catch (Exception e) {
                // Access restrictions, not important
            }
        }

        this.neatoPath = context.getWiki().Param("xwiki.plugin.graphviz.neatopath", NEATO_ENGINE);
        if (!this.neatoPath.equals(NEATO_ENGINE)) {
            try {
                File dfile = new File(this.neatoPath);
                if (!dfile.exists()) {
                    LOGGER.error("Cannot find graphiz neato program at " + this.neatoPath);
                }
            } catch (Exception e) {
                // Access restrictions, not important
            }
        }

    }

    /**
     * Executes GraphViz and returns the URL for the produced file, a PNG image.
     *
     * @param content the dot source
     * @param dot which engine to execute: {@code dot} if {@code true}, {@code neato} if {@code false}
     * @param context the current request context
     * @return the URL which can be used to access the generated image
     * @throws IOException if writing the input or output files to the disk fails
     * @see #getDotResultURL(String, boolean, String, XWikiContext) allows to chose another output format instead of PNG
     */
    public String getDotImageURL(String content, boolean dot, XWikiContext context) throws IOException
    {
        return getDotResultURL(content, dot, DEFAULT_FORMAT, context);
    }

    /**
     * Executes GraphViz and returns the URL for the produced file.
     *
     * @param content the dot source code
     * @param dot which engine to execute: {@code dot} if {@code true}, {@code neato} if {@code false}
     * @param outputFormat the output format to use
     * @param context the current request context
     * @return the URL which can be used to access the result
     * @throws IOException if writing the input or output files to the disk fails
     * @see #getDotImageURL(String, boolean, XWikiContext) if the output should be a simple PNG image
     */
    public String getDotResultURL(String content, boolean dot, String outputFormat, XWikiContext context)
        throws IOException
    {
        String filename = writeDotImage(content, outputFormat, dot);
        return context.getDoc().getAttachmentURL(filename, DOT_ENGINE, context);
    }

    /**
     * Executes GraphViz and return the content of the resulting image (PNG format).
     *
     * @param content the dot source code
     * @param dot which engine to execute: {@code dot} if {@code true}, {@code neato} if {@code false}
     * @return the content of the generated image
     * @throws IOException if writing the input or output files to the disk fails
     */
    public byte[] getDotImage(String content, boolean dot) throws IOException
    {
        return getDotImage(content, DEFAULT_FORMAT, dot);
    }

    /**
     * Executes GraphViz and return the content of the resulting image (PNG format).
     *
     * @param content the dot source code
     * @param extension the output file extension
     * @param dot which engine to execute: {@code dot} if {@code true}, {@code neato} if {@code false}
     * @return the content of the generated file
     * @throws IOException if writing the input or output files to the disk fails
     */
    public byte[] getDotImage(String content, String extension, boolean dot) throws IOException
    {
        int hashCode = Math.abs(content.hashCode());
        return getDotImage(hashCode, content, extension, dot);
    }

    /**
     * Executes GraphViz, writes the resulting image (PNG format) in a temporary file on disk, and returns the filename
     * which can be later used in {@link #outputDotImageFromFile(String, XWikiContext)}.
     *
     * @param content the dot source code
     * @param dot which engine to execute: {@code dot} if {@code true}, {@code neato} if {@code false}
     * @return the name of the file where the generated output is stored
     * @throws IOException if writing the input or output files to the disk fails
     */
    public String writeDotImage(String content, boolean dot) throws IOException
    {
        return writeDotImage(content, DEFAULT_FORMAT, dot);
    }

    /**
     * Executes GraphViz, writes the resulting image (in the requested format) in a temporary file on disk, and returns
     * the filename which can be later used in {@link #outputDotImageFromFile(String, XWikiContext)}.
     *
     * @param content the dot source code
     * @param extension the output file extension
     * @param dot which engine to execute: {@code dot} if {@code true}, {@code neato} if {@code false}
     * @return the name of the file where the generated output is stored
     * @throws IOException if writing the input or output files to the disk fails
     */
    public String writeDotImage(String content, String extension, boolean dot) throws IOException
    {
        int hashCode = Math.abs(content.hashCode());
        getDotImage(hashCode, content, extension, dot);
        String name = (dot ? DOT_ENGINE : NEATO_ENGINE) + '-';
        return name + hashCode + "." + extension;
    }

    /**
     * Executes GraphViz and writes the resulting image (PNG format) into the response.
     *
     * @param content the dot source code
     * @param dot which engine to execute: {@code dot} if {@code true}, {@code neato} if {@code false}
     * @param context the current request context
     * @throws IOException if writing the input or output files to the disk fails, or if writing the response body fails
     */
    public void outputDotImage(String content, boolean dot, XWikiContext context) throws IOException
    {
        outputDotImage(content, DEFAULT_FORMAT, dot, context);
    }

    /**
     * Executes GraphViz and writes the resulting image (in the requested format) into the response.
     *
     * @param content the dot source code
     * @param extension the output file extension
     * @param dot which engine to execute: {@code dot} if {@code true}, {@code neato} if {@code false}
     * @param context the current request context
     * @throws IOException if writing the input or output files to the disk fails, or if writing the response body fails
     */
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

    /**
     * Writes an already generated result from the temporary file into the response.
     *
     * @param filename the name of the temporary file, previously returned by
     *            {@link #writeDotImage(String, String, boolean)}
     * @param context the current request context
     * @throws IOException if reading the file from the disk fails, or if writing the response body fails
     */
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

    /**
     * Executes GraphViz, writes the resulting image (in the requested format) in a temporary file on disk, and returns
     * the generated content from that file.
     *
     * @param hashCode the hascode of the content, to be used as the temporary file name
     * @param content the dot source code
     * @param extension the output file extension
     * @param dot which engine to execute: {@code dot} if {@code true}, {@code neato} if {@code false}
     * @return the content of the generated file
     * @throws IOException if writing the input or output files to the disk fails, or if writing the response body fails
     */
    private byte[] getDotImage(int hashCode, String content, String extension, boolean dot) throws IOException
    {
        File dfile = getTempFile(hashCode, "input.dot", dot);
        if (!dfile.exists()) {
            FileUtils.write(dfile, undoEscapeFilter(content), XWiki.DEFAULT_ENCODING);
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
            final Thread thisThread = Thread.currentThread();
            Thread t = new Thread(new Hangcheck(thisThread), "dot-hangcheck");
            t.run();
            try {
                exitValue = p.waitFor();
                t.interrupt();
            } catch (InterruptedException ex) {
                p.destroy();
                LOGGER.error("Timeout while generating image from dot", ex);
            }

            if (exitValue != 0) {
                LOGGER.error("Error while generating image from dot: "
                    + IOUtils.toString(p.getErrorStream(), XWiki.DEFAULT_ENCODING));
            }
        }
        return FileUtils.readFileToByteArray(ofile);
    }

    /**
     * Get the contents of a previously generated temporary file.
     *
     * @param ofile the file to read
     * @return the content found inside the file, if any
     * @throws IOException when reading the file fails
     */
    private byte[] readDotImage(File ofile) throws IOException
    {
        return FileUtils.readFileToByteArray(ofile);
    }

    /**
     * Return the temporary disk file corresponding to the given parameters.
     *
     * @param hashcode the hashcode of the dot content, used as the main part for the filename
     * @param extension the output file extension
     * @param dot which engine to execute: {@code dot} if {@code true}, {@code neato} if {@code false}
     * @return the corresponding File
     */
    private File getTempFile(int hashcode, String extension, boolean dot)
    {
        String name = (dot ? DOT_ENGINE : NEATO_ENGINE) + '-';
        return getTempFile(name + hashcode + '.' + extension);
    }

    /**
     * Return the temporary disk file corresponding to the given filename.
     *
     * @param filename the filename to look for
     * @return the corresponding File
     */
    private File getTempFile(String filename)
    {
        return new File(this.tempDir, filename);
    }

    /**
     * Hangcheck runnable, which interrupts the main thread after 10 seconds of waiting for the conversion to end. If
     * the conversion ends normally before the 10 seconds timeout expires, then this runnable should be terminated by
     * {@link Thread#interrupt() interrupting it}.
     *
     * @version $Id$
     */
    private static class Hangcheck implements Runnable
    {
        /** The main thread that should be interrupted if the timeout expires. */
        private Thread converterThread;

        /**
         * Simple constructor which specifies the thread to monitor.
         *
         * @param converterThread the thread to monitor
         */
        Hangcheck(Thread converterThread)
        {
            this.converterThread = converterThread;
        }

        @Override
        public void run()
        {
            try {
                Thread.sleep(10000);
                this.converterThread.interrupt();
            } catch (InterruptedException ex) {
                // Expected result if the dot process terminates on time
            }
        }
    }

    /**
     * When rendering using Radeox, {@link com.xpn.xwiki.render.filter.EscapeFilter} replaces all instances of
     * {@code \\char} backslash escapes with a HTML character reference. Unfortunately this also happens for GraphViz
     * content, which isn't right, since some backslash escapes are valid GraphViz syntax. This method undoes this kind
     * of escaping to preserve node and edge label formatting, if the character reference is an ASCII character.
     *
     * @param escapedContent the macro content, already filtered by Radeox and possibly containing broken backslash
     *            escapes
     * @return the content with HTML character references replaced with backslash escapes
     */
    private String undoEscapeFilter(String escapedContent)
    {
        if (StringUtils.isNotEmpty(escapedContent)) {
            Matcher matcher = HTML_ESCAPE_PATTERN.matcher(escapedContent);
            StringBuffer result = new StringBuffer(escapedContent.length());
            while (matcher.find()) {
                int codepoint = Integer.valueOf(matcher.group(1));
                if (codepoint >= 65 && codepoint <= 122) {
                    matcher.appendReplacement(result, new String(new int[] { 92, 92, codepoint }, 0, 3));
                } else {
                    matcher.appendReplacement(result, "$0");
                }
            }
            matcher.appendTail(result);
            return result.toString();
        } else {
            return "";
        }
    }
}
