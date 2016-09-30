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
package com.xpn.xwiki.tool.doc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.web.Utils;

/**
 * An abstract Mojo that knows how to load a XWikiDocument from XML and to write XML from a XWikiDocument
 * 
 * @version $Id$
 */
public abstract class AbstractDocumentMojo extends AbstractMojo
{
    /**
     * The document to perform the update on
     */
    @Parameter(defaultValue = "${basedir}/src/main/resources/XWiki/XWikiPreferences")
    protected File sourceDocument;

    /**
     * The target directory to write the document back to
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}")
    protected File outputDirectory;

    /**
     * An empty context that will hold the base classes encountered in the passed XML document. This is needed in order
     * not to lose the class definition when writing back to XML.
     */
    private XWikiContext context;

    public AbstractDocumentMojo() throws MojoExecutionException
    {
        this.context = createXWikiContext();
    }

    protected XWikiContext createXWikiContext() throws MojoExecutionException
    {
        EmbeddableComponentManager ecm = new EmbeddableComponentManager();
        ecm.initialize(this.getClass().getClassLoader());
        Utils.setComponentManager(ecm);

        // Context component manager is totally useless here
        ecm.unregisterComponent(ComponentManager.class, "context");

        // We need to initialize the Component Manager so that the components can be looked up
        XWikiContext xcontext = new XWikiContext();
        xcontext.put(ComponentManager.class.getName(), ecm);

        // Initialize the Container fields (request, response, session).
        try {
            ExecutionContextManager ecim = ecm.getInstance(ExecutionContextManager.class);

            ExecutionContext econtext = new ExecutionContext();

            // Bridge with old XWiki Context, required for old code.
            xcontext.declareInExecutionContext(econtext);

            ecim.initialize(econtext);
        } catch (ExecutionContextException | ComponentLookupException e) {
            throw new MojoExecutionException("Failed to initialize Execution Context.", e);
        }

        return xcontext;
    }

    /**
     * Loads a XWikiDocument from a XML file
     * 
     * @param file the xml file to load
     * @return the XWiki document loaded from XML
     * @throws MojoExecutionException
     */
    protected XWikiDocument loadFromXML(File file) throws MojoExecutionException
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("xwiki", "XWiki", "WebHome"));
        FileInputStream fis;
        try {
            // Load the document as a XWikiDocument from XML
            fis = new FileInputStream(file);
            doc.fromXML(fis);

            // get XML tree
            FileReader fr = new FileReader(file);
            SAXReader reader = new SAXReader();
            Document domdoc;
            domdoc = reader.read(fr);
            Element root = domdoc.getRootElement();

            // Lookup all class nodes, and add them to our xwiki context as BaseClass definitions
            List<Node> classNodes = root.selectNodes("//xwikidoc/object/class");
            for (Iterator<Node> it = classNodes.iterator(); it.hasNext();) {
                BaseClass bClass = new BaseClass();
                bClass.fromXML((Element) it.next());
                context.addBaseClass(bClass);
            }

            fis.close();
            return doc;
        } catch (Exception e) {
            throw new MojoExecutionException("Error loading XWikiDocument [" + file + "]", e);
        }
    }

    /**
     * Write a XWiki document to a XML file, without rendering and without versions.
     * 
     * @param doc the document to write XML for
     * @param file the file to write the document to
     * @throws MojoExecutionException
     */
    protected void writeToXML(XWikiDocument doc, File file) throws MojoExecutionException
    {
        try {
            FileWriter fw = new FileWriter(file);

            context.setWiki(new XWiki());

            // Write to XML the document and attachments but without rendering and without versions.
            // The passed xwiki context contains the XClass definitions (as BaseClass) all the objects that
            // has been previously loaded from XML, so that the class definition appears again in the XML output.
            String xml = doc.toXML(true, false, true, false, context);

            fw.write(xml);
            fw.close();
        } catch (Exception e) {
            throw new MojoExecutionException("Error writing XML for XWikiDocument [" + file + "]", e);
        }
    }

    /**
     * Return the space directory as a File for a given document in a given directory, creating the directories on the
     * fly if the do not exists
     * 
     * @param document the document to get space for
     * @param directory the directory in which the space will be written
     * @return the space as a File
     * @throws MojoExecutionException
     */
    protected File getSpaceDirectory(File directory, File document) throws MojoExecutionException
    {
        File spaceDir = new File(directory, document.getParentFile().getName());
        if (!spaceDir.exists()) {
            spaceDir.mkdirs();
        }
        return spaceDir;
    }
}
