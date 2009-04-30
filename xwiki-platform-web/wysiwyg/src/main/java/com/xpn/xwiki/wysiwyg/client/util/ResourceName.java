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
package com.xpn.xwiki.wysiwyg.client.util;

/**
 * Stores a resource reference: the wiki, space, page and attached file name, if it's the case. It allows parsing from
 * and serializing to a string, in a {@code org.xwiki.bridge.DocumentName} form, i.e. {@code wiki:Space.Page@attachment}
 * . Note: As opposed to {@code org.xwiki.bridge.DocumentName}, this class is designed to also parse and process
 * <em>relative</em> references, such as {@code attachment.png}, or {@code Page@filename@attachment.png}, in which case
 * the missing values will be set to empty. Thus, relative references to resources can be stored and processed (@see
 * {@link #resolveRelativeTo(ResourceName)}). However, the assumption that if a reference is relative then its specified
 * values are continuous (cannot have {@code wiki} specified, {@code space} missing and {@code page} specified). <br />
 * FIXME: this logic should NOT be duplicated on the client, but since it's needed in frequent enough situations
 * (resolving relative links, resolving an attachment location: current page or another page), it's not worth making a
 * call to the server just to have such a string parsed or serialized. <br />
 * TODO: should do most of the parsing on the server and send already modeled data to the client so that no parsing
 * whatsoever is needed on the client any longer.
 * 
 * @version $Id$
 */
public class ResourceName
{
    /**
     * Separator between wiki name and space name.
     */
    private static final String WIKI_SEPARATOR = ":";

    /**
     * Separator between space name and page name.
     */
    private static final String SPACE_SEPARATOR = ".";

    /**
     * Separator between page name and file name.
     */
    private static final String PAGE_SEPARATOR = "@";

    /**
     * The default wiki, for the comparison of two resources.
     */
    private static final String DEFAULT_WIKI = "xwiki";

    /**
     * The wiki of the resource.
     */
    private String wiki;

    /**
     * The space of the resource.
     */
    private String space;

    /**
     * The page of the resource.
     */
    private String page;

    /**
     * The name of the attachment referred in this resource description.
     */
    private String file;

    /**
     * Default constructor.
     */
    public ResourceName()
    {
    }

    /**
     * Builds a resource from the passed wiki, space, page and file.
     * 
     * @param wiki the wiki to build the resource for
     * @param space the space to build the resource for
     * @param page the page to build the resource for
     * @param file the file to build the resource for
     */
    public ResourceName(String wiki, String space, String page, String file)
    {
        this.wiki = wiki;
        this.space = space;
        this.page = page;
        this.file = file;
    }

    /**
     * @return the wiki
     */
    public String getWiki()
    {
        return wiki;
    }

    /**
     * @param wiki the wiki to set
     */
    public void setWiki(String wiki)
    {
        this.wiki = wiki;
    }

    /**
     * @return the space
     */
    public String getSpace()
    {
        return space;
    }

    /**
     * @param space the space to set
     */
    public void setSpace(String space)
    {
        this.space = space;
    }

    /**
     * @return the page
     */
    public String getPage()
    {
        return page;
    }

    /**
     * @param page the page to set
     */
    public void setPage(String page)
    {
        this.page = page;
    }

    /**
     * @return the file
     */
    public String getFile()
    {
        return file;
    }

    /**
     * @param file the file to set
     */
    public void setFile(String file)
    {
        this.file = file;
    }

    /**
     * Clears this resource name, i.e. sets everything on null.
     */
    public void clear()
    {
        wiki = null;
        space = null;
        page = null;
        file = null;
    }

    /**
     * Parses the passed string as a string reference to this resource (i.e. in the form {@code
     * wiki:Space.Page@attachment}) and stores it in the current object. <br />
     * 
     * @param reference the string form of a resource reference, {@code wiki:Space.Page@attachment}
     * @see #resolveRelativeTo(ResourceName)
     */
    public void fromString(String reference)
    {
        clear();
        if (StringUtils.isEmpty(reference)) {
            // nothing
            return;
        } else {
            // follow the parsing order from the xhtml parser on the server
            String strippedRef = reference;
            if (reference.startsWith("image:") || reference.startsWith("attach:")) {
                strippedRef = reference.substring(reference.indexOf(':') + 1);
                // parse the file name
                strippedRef = parseFileName(strippedRef);
            }
            parseReference(strippedRef);
        }
    }

    /**
     * Helper function to do the parsing from actual reference, after it has been stripped from the file part (protocol
     * and file).
     * 
     * @param reference document name to parse
     */
    private void parseReference(String reference)
    {
        // TODO: this code needs cleanup: as it is, it's duplicated from the DefaultDocumentNameFactory and it might do
        // too much for the current purpose

        // Step 1: Extract the wiki name

        // We allow the wiki separator in wiki names and thus we look for the last wiki sep in the reference.
        // TODO: Note that this was done to have the same behavior of XWikiDocument.setFullName() but it would
        // seem better to me to allow the wiki sep in space names rather than in wiki names (since wiki
        // names are constrained by database schema names).
        int spaceSeparatorPosition;
        int wikiSeparatorPosition = reference.lastIndexOf(WIKI_SEPARATOR);
        if (wikiSeparatorPosition != -1) {
            wiki = reference.substring(0, wikiSeparatorPosition);
            if (wiki.length() == 0) {
                wiki = null;
            }

            // We allow space sep in space names and thus we look for the last space sep in the reference.
            // TODO: Note that this was done to have the same behavior of XWikiDocument.setFullName() but it would
            // seem better to me to allow space sep in pages names rather than in space names (since users
            // want more liberty in page names and usually create pages in existing spaces).
            spaceSeparatorPosition = reference.lastIndexOf(SPACE_SEPARATOR);
            if (spaceSeparatorPosition <= wikiSeparatorPosition) {
                spaceSeparatorPosition = -1;
            }
        } else {
            // No wiki separator, set wiki to nothing
            wiki = null;

            // We allow space sep in space names and thus we look for the last space sep in the reference.
            // TODO: Note that this was done to have the same behavior of XWikiDocument.setFullName() but it would
            // seem better to me to allow space sep in pages names rather than in space names (since users
            // want more liberty in page names and usually create pages in existing spaces).
            spaceSeparatorPosition = reference.lastIndexOf(SPACE_SEPARATOR);
        }

        // Step 2: Extract the space and page names

        if (spaceSeparatorPosition != -1) {
            space = reference.substring(wikiSeparatorPosition + WIKI_SEPARATOR.length(), spaceSeparatorPosition);
            if (space.length() == 0) {
                space = null;
            }

            // Make sure the space separator is not the last char of the reference
            if (spaceSeparatorPosition + SPACE_SEPARATOR.length() < reference.length()) {
                page = reference.substring(spaceSeparatorPosition + SPACE_SEPARATOR.length());
            } else {
                page = null;
            }
        } else {
            // No space separator the whole substring is thus the page.
            space = null;

            // Make sure the wiki separator is not the last char of the reference
            if (wikiSeparatorPosition == -1 || wikiSeparatorPosition + WIKI_SEPARATOR.length() < reference.length()) {
                page = reference.substring(wikiSeparatorPosition + WIKI_SEPARATOR.length());
            } else {
                page = null;
            }
        }
    }

    /**
     * Helper function to parse a file name from a reference, setting the {@code file} value and returns the remainder
     * reference. To be used only when the reference is known to point to a file.
     * 
     * @param reference the reference to parse the filename from
     * @return the remainder of the reference after the file name has been parsed out
     */
    private String parseFileName(String reference)
    {
        int pageSeparatorPosition = reference.indexOf(PAGE_SEPARATOR);
        String remainder = "";
        if (pageSeparatorPosition < 0) {
            // the whole reference is the file name, because we're parsing files
            file = reference;
            // there is no page
            remainder = "";
        } else {
            // set according to separator position
            file = reference.substring(pageSeparatorPosition + 1);
            remainder = reference.substring(0, pageSeparatorPosition);
        }
        return remainder;
    }

    /**
     * Resolves the current resource relative to the passed resource.
     * 
     * @param resource the resource to resolve relative to
     * @return a new resource, which represents this resource resolved relative to the passed resource
     */
    public ResourceName resolveRelativeTo(ResourceName resource)
    {
        ResourceName resolved = new ResourceName(wiki, space, page, file);
        // take missing values from the reference resource
        if (StringUtils.isEmpty(wiki)) {
            resolved.setWiki(resource.getWiki());
        }
        if (StringUtils.isEmpty(space)) {
            resolved.setSpace(resource.getSpace());
        }
        if (StringUtils.isEmpty(page)) {
            resolved.setPage(resource.getPage());
        }
        if (StringUtils.isEmpty(file)) {
            resolved.setFile(resource.getFile());
        }

        return resolved;
    }

    /**
     * Creates the String representation of the resource, in the form {@code wiki:Space.Page@file}.
     * 
     * @return the string representation of this resource
     * @see #fromString(String)
     */
    public String toString()
    {
        StringBuffer reference = new StringBuffer();
        if (!StringUtils.isEmpty(file)) {
            reference.append(file);
        }

        if (!StringUtils.isEmpty(page)) {
            if (reference.length() > 0) {
                reference.insert(0, PAGE_SEPARATOR);
            }
            reference.insert(0, page);
        }

        if (!StringUtils.isEmpty(space)) {
            if (reference.length() > 0) {
                reference.insert(0, SPACE_SEPARATOR);
            }
            reference.insert(0, space);
        }

        if (!StringUtils.isEmpty(wiki)) {
            if (reference.length() > 0) {
                reference.insert(0, WIKI_SEPARATOR);
            }
            reference.insert(0, wiki);
        }

        return reference.toString();
    }

    /**
     * Compares this resource with the passed resource to check if the values for the components match up to the page
     * component, inclusive. If there are components missing in the current resource, they will be considered matching
     * so that relative resources are matched only on the specified components.
     * 
     * @param resource the resource to compare this resource to
     * @return {@code true} if this resource matches up to the page component (inclusive) (if the resource referred by
     *         this {@link ResourceName} is in the same page with the passed resource), or {@code false} otherwise
     */
    public boolean matchesUpToPage(ResourceName resource)
    {
        if (!StringUtils.isEmpty(wiki)) {
            // either they're equal, either this is "xwiki" and the other one is empty
            if (!(wiki.equals(resource.getWiki()) || (StringUtils.isEmpty(resource.getWiki()) && wiki
                .equals(DEFAULT_WIKI)))) {
                return false;
            }
        }
        if (!StringUtils.isEmpty(space) && !space.equals(resource.getSpace())) {
            return false;
        }

        if (!StringUtils.isEmpty(page) && !page.equals(resource.getPage())) {
            return false;
        }

        return true;
    }
}
