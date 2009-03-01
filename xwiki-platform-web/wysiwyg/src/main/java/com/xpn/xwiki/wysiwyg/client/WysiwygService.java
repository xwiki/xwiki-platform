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
package com.xpn.xwiki.wysiwyg.client;

import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.xpn.xwiki.gwt.api.client.XWikiGWTException;
import com.xpn.xwiki.wysiwyg.client.diff.Revision;
import com.xpn.xwiki.wysiwyg.client.plugin.image.ImageConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.macro.MacroDescriptor;
import com.xpn.xwiki.wysiwyg.client.sync.SyncResult;

/**
 * The service interface used on the server.
 * 
 * @version $Id$
 */
public interface WysiwygService extends RemoteService
{
    /**
     * Utility class for accessing the service stub.
     */
    public static final class Singleton
    {
        /**
         * The service stub.
         */
        private static WysiwygServiceAsync instance;

        /**
         * Private constructor because this is a utility class.
         */
        private Singleton()
        {
        }

        /**
         * @return The service stub.
         */
        public static synchronized WysiwygServiceAsync getInstance()
        {
            if (instance == null) {
                String serviceURL;
                try {
                    // Look in the global configuration object.
                    serviceURL = Dictionary.getDictionary("Wysiwyg").get("wysiwygServiceURL");
                } catch (MissingResourceException e) {
                    serviceURL = "/WysiwygService";
                }

                instance = (WysiwygServiceAsync) GWT.create(WysiwygService.class);
                ((ServiceDefTarget) instance).setServiceEntryPoint(serviceURL);

                // We cache the service calls.
                instance = new WysiwygServiceAsyncCacheProxy(instance);
            }
            return instance;
        }
    }

    /**
     * @param html The HTML fragment to be converted.
     * @param syntax The syntax of the result.
     * @return The result of converting the given HTML fragment to the specified syntax.
     */
    String fromHTML(String html, String syntax);

    /**
     * @param source The text to be converted.
     * @param syntax The syntax of the given text.
     * @return The result of converting the given text from the specified syntax to HTML.
     */
    String toHTML(String source, String syntax);

    /**
     * @param dirtyHTML The HTML fragment to be cleaned.
     * @return The result of cleaning the given HTML fragment.
     */
    String cleanHTML(String dirtyHTML);

    /**
     * Parses the given HTML fragment and renders the result in XHTML.
     * 
     * @param html the HTML fragment to be rendered
     * @param syntax the storage syntax
     * @return the XHTML result of rendering the given HTML fragment
     */
    String parseAndRender(String html, String syntax);

    /**
     * Cleans dirty html content produced from an office application like MsWord, MsExcel, OpenOffice Writer etc. This
     * method is primarily utilized by the office importer wysiwyg plugin.
     * 
     * @param htmlPaste dirty html pasted by the user.
     * @param cleanerHint role hint for which cleaner to be used.
     * @param cleaningParams additional parameters to be used when cleaning.
     * @return The cleaned html content.
     */
    String cleanOfficeHTML(String htmlPaste, String cleanerHint, Map<String, String> cleaningParams);

    /**
     * Imports an office document attached to a wiki page into XHTML/1.0. The resulting xhtml content will be returned
     * from this method while if there are non-textual content in the original office document, they will be attached to
     * the wiki page identified by pageName. Note that this method does not alter the content of the wiki page
     * identified by pageName.
     * 
     * @param pageName the wiki page into which the office document is attached.
     * @param attachmentName name of the attached office document.
     * @param cleaningParams additional parameters for the import operation.
     * @return the xhtml result from the office importer.
     * @throws XWikiGWTException if the import operation fails.
     */
    String officeToXHTML(String pageName, String attachmentName, Map<String, String> cleaningParams)
        throws XWikiGWTException;

    /**
     * @param syncedRevision The changes to this editor's content, since the last update.
     * @param pageName The page being edited.
     * @param version The version affected by syncedRevision.
     * @param syncReset resets the sync server for this page.
     * @return The result of synchronizing this editor with others editing the same page.
     * @throws XWikiGWTException when the synchronization fails
     */
    SyncResult syncEditorContent(Revision syncedRevision, String pageName, int version, boolean syncReset)
        throws XWikiGWTException;

    /**
     * Check if the current wiki is part of a multiwiki (i.e. this is a virtual wiki).
     * 
     * @return true if the current wiki is a multiwiki, and false in the other case
     */
    Boolean isMultiWiki();

    /**
     * @return a list containing the names of all wikis.
     */
    List<String> getVirtualWikiNames();

    /**
     * @param wikiName the name of the wiki to search for spaces. If this is <code>null</code>, the current wiki will be
     *            used.
     * @return a list of all spaces names in the specified wiki.
     */
    List<String> getSpaceNames(String wikiName);

    /**
     * @param wikiName the name of the wiki. Pass <code>null</code> if this should use the current wiki.
     * @param spaceName the name of the space
     * @return the list of the page names from a given space and a given wiki.
     */
    List<String> getPageNames(String wikiName, String spaceName);

    /**
     * Creates a page link (url, reference) from the given parameters. None of them are mandatory, if one misses, it is
     * replaced with a default value.
     * 
     * @param wikiName the name of the wiki to which to link
     * @param spaceName the name of the space of the page. If this parameter is missing, it is replaced with the space
     *            of the current document in the context.
     * @param pageName the name of the page to which to link to. If it's missing, it is replaced with "WebHome".
     * @param revision the value for the page revision to which to link to. If this is missing, the link is made to the
     *            latest revision, the default view action for the document.
     * @param anchor the name of the anchor type.
     * @return the data of the link to the document, containing link url and link reference information.
     */
    LinkConfig getPageLink(String wikiName, String spaceName, String pageName, String revision, String anchor);

    /**
     * Returns all the image attachments from the referred page. It can either get all the pictures in a page or in a
     * space or in a wiki, depending on the values of its parameters. A null means a missing parameter on that position.
     * 
     * @param wikiName the name of the wiki to get images from
     * @param spaceName the name of the space to get image attachments from
     * @param pageName the name of the page to get image attachments from
     * @return list of the images
     */
    List<ImageConfig> getImageAttachments(String wikiName, String spaceName, String pageName);

    /**
     * @param macroName a string representing the name of a macro
     * @param syntax the string identifier for the storage syntax
     * @return an object describing the specified macro, or {@code null} if the specified macro is not found
     */
    MacroDescriptor getMacroDescriptor(String macroName, String syntax);
}
