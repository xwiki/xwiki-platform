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
import java.io.FileWriter;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * An abstract Mojo that knows how to load a XWikiDocument from XML and to write XML from a
 * XWikiDocument
 * 
 * @version $Id: $
 */
public abstract class AbstractDocumentMojo extends AbstractMojo
{
    
    /**
     * The maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The document to perform the update on
     * 
     * @parameter expression="${basedir}/src/main/resources/XWiki/XWikiPreferences"
     * @required
     */
    protected File sourceDocument;
    
    /**
     * The target directory to write the document back to
     * 
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    protected File outputDirectory;    
    
    /**
     * Loads a XWikiDocument from a XML file
     * 
     * @param file the xml file to load
     * @return the XWiki document loaded from XML
     * @throws MojoExecutionException
     */
    protected XWikiDocument loadFromXML(File file) throws MojoExecutionException
    {
        XWikiDocument doc = new XWikiDocument();
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
            doc.fromXML(fis);
            fis.close();
            return doc;
        } catch (Exception e) {
            throw new MojoExecutionException("Error loading XWikiDocument [" + file + "]", e);
        }
    }

    /**
     * Write a XWiki document to a XML file
     * 
     * @param doc the document to write XML for
     * @param file the file to write the document to
     * @throws MojoExecutionException
     */
    protected void writeToXML(XWikiDocument doc, File file) throws MojoExecutionException
    {
        try {
            FileWriter fw = new FileWriter(file);
            // write to XML the document, its object and attachments 
            // but without rendering and without versions
            // A null context does the trick if the document
            // has been properly loaded.
            fw.write(doc.toXML(true, false, true, false, null));
            fw.close();
        } catch (Exception e) {
            throw new MojoExecutionException("Error writing XML for XWikiDocument [" + file + "]",
                e);
        }
    }

    /**
     * Return the space directory as a File for a given document in a given
     * directory, creating the directories on the fly if the do not exists
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
