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
package org.xwiki.gwt.user.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;

/**
 * This {@link Constants} interface is used to localize user interface strings.
 * 
 * @version $Id$
 */
public interface Strings extends Constants
{
    /**
     * An instance of this string bundle that can be used anywhere in the code to obtain i18n strings.
     */
    Strings INSTANCE = (Strings) GWT.create(Strings.class);

    /**
     * @return the tool tip used by the dialog box close icon
     */
    String close();

    /**
     * @return an error message saying that the dialog box failed to load
     */
    String dialogFailedToLoad();

    /**
     * @return the label used on the wizard step previous button
     */
    String wizardPrevious();

    /**
     * @return the label used on the wizard step next button
     */
    String wizardNext();

    /**
     * @return the label used on the wizard step finish button
     */
    String wizardFinish();

    /**
     * NOTE: We added this message because the {@code statusText} response property is not accessible when the status
     * code is 0 (unknown).
     * 
     * @return the status text for an HTTP response with status code 0 (unknown); such a response it usually generated
     *         when the HTTP request is aborted
     */
    String httpStatusTextRequestAborted();
}
