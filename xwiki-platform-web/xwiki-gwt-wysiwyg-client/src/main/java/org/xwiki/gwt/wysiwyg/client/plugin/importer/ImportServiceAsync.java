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

import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Service interface used on the client by the importer plug-in. It should have all the methods from
 * {@link ImportService} with an additional {@link AsyncCallback} parameter. This is specific to GWT's architecture.
 * 
 * @version $Id$
 */
public interface ImportServiceAsync
{
    /**
     * Makes a request to the server to clean the given HTML fragment which comes from an office application.
     * 
     * @param htmlPaste dirty HTML pasted by the user
     * @param cleanerHint role hint for which cleaner to be used
     * @param cleaningParams additional parameters to be used when cleaning
     * @param async the call-back to be used for notifying the caller after receiving the response from the server
     */
    void cleanOfficeHTML(String htmlPaste, String cleanerHint, Map<String, String> cleaningParams,
        AsyncCallback<String> async);

    /**
     * Imports the given office attachment into XHTML 1.0.
     * 
     * @param attachment office attachment to be imported into XHTML 1.0
     * @param cleaningParams additional parameters for the import operation
     * @param async the call-back to be used for notifying the caller after receiving the response from the server
     */
    void officeToXHTML(Attachment attachment, Map<String, String> cleaningParams, AsyncCallback<String> async);
}
