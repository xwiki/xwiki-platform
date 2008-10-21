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
package com.xpn.xwiki.tool.xar;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.archiver.ArchiveEntry;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 * Gather all resources in a XAR file (which is actually a ZIP file). Also generates a XAR descriptor if none is
 * provided.
 * <p>
 * Note that the generated descriptor currently doesn't handle translations.
 * </p>
 * 
 * @version $Id: $
 * @goal xar
 * @phase package
 * @requiresDependencyResolution runtime
 */
public class XarMojo extends AbstractXarMojo
{
    /**
     * To look up Archiver/UnArchiver implementations.
     * 
     * @parameter expression="${component.org.codehaus.plexus.archiver.manager.ArchiverManager}"
     * @required
     */
    protected ArchiverManager archiverManager;

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoExecutionException
    {
        if (this.project.getResources().size() < 1) {
            this.getLog().warn("No XAR created as no resources were found");
            return;
        }

        try {
            performArchive();
        } catch (Exception e) {
            throw new MojoExecutionException("Error while creating XAR file", e);
        }
    }

    /**
     * Create the XAR by zipping the resource files.
     * 
     * @throws Exception if the zipping failed for some reason
     */
    private void performArchive() throws Exception
    {
        // The target file.
        File xarFile = new File(this.project.getBuild().getDirectory(), this.project.getArtifactId() + ".xar");

        // The source dir points to the target/classes directory where the Maven resources plugin
        // has copied the XML files during the process-resources phase.
        File sourceDir = new File(this.project.getBuild().getOutputDirectory());

        // The XAR is just a plain zip archive, with a special layout of the files, and a special package.xml file.
        ZipArchiver archiver = new ZipArchiver();
        archiver.setDestFile(xarFile);
        archiver.setIncludeEmptyDirs(false);
        archiver.setCompress(true);

        // Unzip dependent XARs on top of this project's XML documents but without overwriting
        // existing files since we want this projet's files to be used if they override a file
        // present in a XAR dependency.
        unpackDependentXars();

        // If no package.xml can be found at the top level of the current project, generate one
        // otherwise, try to use the existing one
        File packageXml = new File(sourceDir, PACKAGE_XML);
        if (packageXml.exists()) {
            addFilesToArchive(archiver, sourceDir, packageXml);
        } else {
            addFilesToArchive(archiver, sourceDir);
        }

        archiver.createArchive();

        this.project.getArtifact().setFile(xarFile);
    }

    /**
     * Create and add package configuration file to the package.
     * 
     * @param packageFile the package when to add configuration file.
     * @param files the files in the package.
     * @throws IOException error when writing the configuration file.
     */
    private void generatePackageXml(File packageFile, Collection<ArchiveEntry> files) throws IOException
    {
        getLog().info("Generating package.xml descriptor at [" + packageFile.getPath() + "]");

        OutputFormat outputFormat = new OutputFormat("", true);
        outputFormat.setEncoding("ISO-8859-1");
        FileWriter fw = new FileWriter(packageFile);
        XMLWriter writer = new XMLWriter(fw, outputFormat);
        writer.write(toXML(files));
        writer.close();
        fw.close();
    }

    /**
     * Generate a DOM4J Document containing the generated XML.
     * 
     * @param files the list of files that we want to include in the generated package XML file.
     * @return the DOM4J Document containing the generated XML
     */
    private Document toXML(Collection<ArchiveEntry> files)
    {
        Document doc = new DOMDocument();

        Element packageElement = new DOMElement("package");
        doc.setRootElement(packageElement);

        Element infoElement = new DOMElement("infos");
        packageElement.add(infoElement);
        addInfoElements(infoElement);

        Element filesElement = new DOMElement(FILES_TAG);
        packageElement.add(filesElement);
        addFileElements(files, filesElement);

        return doc;
    }

    /**
     * Add all the XML elements under the &lt;info&gt; element (name, description, license, author, version and whether
     * it's a backup pack or not).
     * 
     * @param infoElement the info element to which to add to
     */
    private void addInfoElements(Element infoElement)
    {
        Element el = new DOMElement("name");
        el.addText(this.project.getName());
        infoElement.add(el);

        el = new DOMElement("description");
        el.addText(this.project.getDescription());
        infoElement.add(el);

        el = new DOMElement("licence");
        el.addText("");
        infoElement.add(el);

        el = new DOMElement("author");
        el.addText("XWiki.Admin");
        infoElement.add(el);

        el = new DOMElement("version");
        el.addText(this.project.getVersion());
        infoElement.add(el);

        el = new DOMElement("backupPack");
        el.addText("true");
        infoElement.add(el);
    }

    /**
     * Add all the XML elements under the &lt;files&gt; element (the list of files present in the XAR).
     * 
     * @param files the list of files that we want to include in the generated package XML file.
     * @param filesElement the files element to which to add to
     */
    private void addFileElements(Collection<ArchiveEntry> files, Element filesElement)
    {
        for (ArchiveEntry entry : files) {
            // Don't add files in META-INF to the package.xml file
            if (entry.getFile().getPath().indexOf("META-INF") == -1) {
                XWikiDocument xdoc = getDocFromXML(entry.getFile());
                if (xdoc != null) {
                    String fullName = xdoc.getFullName();
                    Element element = new DOMElement(FILE_TAG);
                    element.setText(fullName);
                    element.addAttribute("language", xdoc.getLanguage());
                    element.addAttribute("defaultAction", "0");
                    filesElement.add(element);
                }
            }
        }
    }

    /**
     * Load a XWiki document from its XML representation.
     * 
     * @param file the file to parse.
     * @return the loaded document object or null if the document cannot be parsed
     */
    private XWikiDocument getDocFromXML(File file)
    {
        XWikiDocument doc = null;

        try {
            doc = new XWikiDocument();
            doc.fromXML(file);
        } catch (Exception e) {
            this.getLog().warn(
                "Failed to parse [" + file.getAbsolutePath() + "], skipping it. " + "The error was [" + e.getMessage()
                    + "]", e);
        }

        return doc;
    }

    /**
     * Gets the list of document names from a 'package.xml' document.
     * 
     * @param xmlPath the path to the XML document to parse
     * @return the list of document names contained in the XML document
     * @throws Exception if the XML document is invalid or it contains no document list or it doesn't exist
     */
    private Collection<String> getDocumentNamesFromXML(String xmlPath) throws Exception
    {
        Collection<String> result = new LinkedList<String>();
        SAXReader reader = new SAXReader();
        Document domdoc;
        domdoc = reader.read(new FileReader(xmlPath));

        Element filesElement = domdoc.getRootElement().element(FILES_TAG);

        if (filesElement == null) {
            throw new Exception("The supplied document contains no document list ");
        }
        Collection elements = filesElement.elements(FILE_TAG);
        for (Object item : elements) {
            if (item instanceof Element) {
                Element currentElement = (Element) item;
                String documentName = currentElement.getText();
                result.add(documentName);
            }
        }
        return result;
    }

    /**
     * Adds the files from a specific directory to an archive. It also builds a package.xml file based on that content
     * which is also added to the archive.
     * 
     * @param archiver the archive in which the files will be added
     * @param sourceDir the directory whose contents will be added to the archive
     * @throws Exception if the files cannot be added to the archive
     */
    private void addFilesToArchive(ZipArchiver archiver, File sourceDir) throws Exception
    {
        archiver.addDirectory(sourceDir, getIncludes(), getExcludes());
        File generatedPackageFile = new File(sourceDir, PACKAGE_XML);
        generatePackageXml(generatedPackageFile, archiver.getFiles().values());
        archiver.addFile(generatedPackageFile, PACKAGE_XML);
    }

    /**
     * Adds files from a specific directory to an archive. It uses an existing package.xml to filter the files to be
     * added.
     * 
     * @param archiver the archive in which the files will be added
     * @param sourceDir the directory whose contents will be added to the archive
     * @param packageXml the corresponding package.xml file
     * @throws Exception if the files cannot be added to the archive
     */
    private void addFilesToArchive(ZipArchiver archiver, File sourceDir, File packageXml) throws Exception
    {
        Collection<String> documentNames = null;
        this.getLog().info("Using the existing package.xml descriptor at [" + packageXml.getPath() + "]");
        try {
            documentNames = getDocumentNamesFromXML(sourceDir.getAbsolutePath() + File.separator + PACKAGE_XML);
        } catch (Exception e) {
            // When the provided package.xml is invalid, abort the build.
            this.getLog().error("The existing " + PACKAGE_XML + " is invalid.");
            throw e;
        }
        for (String currentItem : documentNames) {
            try {
                String currentFileName = currentItem.replace('.', File.separatorChar) + ".xml";
                File generatedFile = new File(sourceDir, currentFileName);
                // This will further throw an exception if a file listed in package.xml does not exist.
                getLog().debug("Adding file [" + currentFileName + "]...");
                archiver.addFile(generatedFile, currentFileName);
            } catch (ArchiverException ex) {
                getLog().error("The file [" + currentItem + "] was listed in " + PACKAGE_XML + " but was not found.");
                throw ex;
            }
        }
        archiver.addFile(packageXml, PACKAGE_XML);
    }
}
