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
package org.xwiki.gwt.wysiwyg.client.plugin.importer;

import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The service interface used on the server.
 * 
 * @version $Id$
 */
@Role
@RemoteServiceRelativePath("ImportService.gwtrpc")
public interface ImportService extends RemoteService
{
    /**
     * Cleans dirty HTML content produced from an office application like MsWord, MsExcel, OpenOffice Writer etc. This
     * method is primarily utilized by the office importer WYSIWYG plug-in.
     * 
     * @param htmlPaste dirty HTML pasted by the user
     * @param cleanerHint role hint for which cleaner to be used
     * @param cleaningParams additional parameters to be used when cleaning
     * @return the cleaned HTML content
     */
    String cleanOfficeHTML(String htmlPaste, String cleanerHint, Map<String, String> cleaningParams);

    /**
     * Imports the given office attachment into XHTML 1.0. This method returns the resulting XHTML content while if
     * there are non-textual content in the office attachment, they will be attached to the owner wiki page. Note that
     * this operation does not alter the content of the wiki page.
     * 
     * @param attachment office attachment to be imported into XHTML 1.0.
     * @param cleaningParams additional parameters for the import operation.
     * @return the XHTML result from the office importer.
     */
    String officeToXHTML(Attachment attachment, Map<String, String> cleaningParams);
}
