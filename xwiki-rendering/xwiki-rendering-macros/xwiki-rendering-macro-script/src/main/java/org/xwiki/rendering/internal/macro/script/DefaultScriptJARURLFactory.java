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
package org.xwiki.rendering.internal.macro.script;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.xwiki.bridge.AttachmentName;
import org.xwiki.bridge.AttachmentNameFactory;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentName;
import org.xwiki.bridge.DocumentNameFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.macro.script.ScriptJARURLFactory;

/**
 * Default implementation supporting the syntax defined in {@link #createJARURLs(String)}. 
 *  
 * @version $Id$
 * @since 2.0RC1
 */
@Component
public class DefaultScriptJARURLFactory implements ScriptJARURLFactory
{
    /**
     * Character to separate document name from JAR name, see {@link #createClassLoader(String)}.
     * Note that we cannot rely completely on {@link AttachmentNameFactory} since we need to handle one 
     * special case: when the format {@code attach:wiki:space.page} is used to signify that all jar
     * attachments from the specified page must be added.
     */
    private static final String FILENAME_SEPARATOR = "@";

    /**
     * Since we want to allow the format {@code attach:wiki:space.page} to add all jars located on that document, 
     * we need to be able to differentiate if a reference is specifying a document name or a jar. We check this
     * by verifying the suffix.
     */
    private static final String JAR_SUFFIX = "jar";
    
    /**
     * Create attachment name from a string reference.
     */
    @Requirement
    private AttachmentNameFactory attachmentNameFactory;
    
    /**
     * Used to get the current document name and document URLs.
     */
    @Requirement
    private DocumentAccessBridge documentAccessBridge;
    
    /**
     * Used to get the current document name from a string reference.
     */
    @Requirement("current")
    private DocumentNameFactory documentNameFactory;
    
    /**
     * {@inheritDoc}
     * 
     * Parse a string pointing to JARs locations, either a known URL protocol such as "http" or a special "attach" 
     * protocol to generate JAR URLs from JARs attached to wiki pages.
     * <ul>
     *   <li>{@code attach:wiki:space.page@somefile.jar}</li>
     *   <li>{@code attach:wiki:space.page}: all jars located on the passed page</li>
     * </ul>
     * 
     * @see ScriptJARURLFactory#createURLs(String)
     */
    public List<URL> createJARURLs(String scriptJars) throws Exception
    {
        List<URL> urls = new ArrayList<URL>();
        
        if (!StringUtils.isEmpty(scriptJars)) {
            List<String> urlsAsString = new ArrayList<String>();
            StringTokenizer tokenizer = new StringTokenizer(scriptJars, ",");
            while (tokenizer.hasMoreElements()) {
                String token = tokenizer.nextToken();

                if (token.startsWith("attach:")) {
                    urlsAsString.addAll(parseAttachmentSyntax(token.substring(7)));
                } else {
                    // Assume we have a URL and use it as is
                    urlsAsString.add(token);
                }
            }

            for (String urlAsString : urlsAsString) {
                urls.add(new URL(urlAsString));
            }
        }
        
        return urls;
    }
    
    /**
     * @param attachmentReference the reference to parse
     * @return the list of URLs (as strings) to add to the returned Class Loader
     * @throws Exception in case of an error retrieving the full list of attachments of the referenced document
     */
    private List<String> parseAttachmentSyntax(String attachmentReference) throws Exception
    {
        List<String> urls = new ArrayList<String>();
        
        int pos = attachmentReference.lastIndexOf(FILENAME_SEPARATOR);
        // If there's a "@" symbol specified use an attachment parser.
        if (pos > -1) {
            AttachmentName attachmentName = this.attachmentNameFactory.createAttachmentName(attachmentReference);
            urls.add(this.documentAccessBridge.getAttachmentURL(attachmentName, true));
        } else {
            // If the reference ends with "jar" then it's a filename reference, otherwise it's a document name
            if (attachmentReference.toLowerCase().endsWith(JAR_SUFFIX)) {
                // TODO: Do we need to check if current document name is null?
                AttachmentName attachmentName = new AttachmentName(
                    this.documentAccessBridge.getCurrentDocumentName(), attachmentReference);
                urls.add(this.documentAccessBridge.getAttachmentURL(attachmentName, true));
            } else {
                // Add all jars attached to the specified document
                DocumentName documentName = this.documentNameFactory.createDocumentName(attachmentReference);
                // Only add attachments ending with the jar suffix
                for (String attachmentURL : this.documentAccessBridge.getAttachmentURLs(documentName, true)) {
                    if (attachmentURL.endsWith(JAR_SUFFIX)) {
                        urls.add(attachmentURL);
                    }
                }
            }
        }

        return urls;
    }
}
