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
package org.xwiki.extension.script.internal;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.extension.Extension;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.ExtensionValidator;
import org.xwiki.job.Request;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Default right needed to install/uninstall an extension in XWiki.
 * 
 * @version $Id$
 * @since 4.2M2
 */
// The rationale for being in this module is that checking right is useless if you don't also provide public script
// service but if there is other things to put in a new xwiki-platform-extension-xwiki we might want to move it.
public class XWikiExtensionValidator implements ExtensionValidator
{
    private static final String PROPERTY_USERREFERENCE = "user.reference";

    private static final String PROPERTY_CALLERREFERENCE = "caller.reference";

    private static final String PROPERTY_CHECKRIGHTS = "checkrights";

    /**
     * Needed for checking programming rights.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    private boolean hasAccessLevel(String right, String document, Request request) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        boolean hasAccess = true;

        String caller = getRequestUserString(PROPERTY_CALLERREFERENCE, request);
        if (caller != null) {
            hasAccess =
                xcontext.getWiki().getRightService().hasAccessLevel(right, caller, "XWiki.XWikiPreferences", xcontext);
        }

        if (hasAccess) {
            String user = getRequestUserString(PROPERTY_USERREFERENCE, request);
            if (user != null) {
                hasAccess =
                    xcontext.getWiki().getRightService()
                        .hasAccessLevel(right, user, "XWiki.XWikiPreferences", xcontext);
            }
        }

        return hasAccess;
    }

    private DocumentReference getRequestUserReference(String property, Request request)
    {
        Object obj = request.getProperty(property);

        if (obj instanceof DocumentReference) {
            return (DocumentReference) obj;
        }

        return null;
    }

    private String getRequestUserString(String property, Request request)
    {
        String str = null;

        if (request.containsProperty(property)) {
            DocumentReference reference = getRequestUserReference(property, request);

            if (reference != null) {
                str = this.serializer.serialize(reference);
            } else {
                str = XWikiRightService.GUEST_USER_FULLNAME;
            }
        }

        return str;
    }

    @Override
    public void checkInstall(Extension extension, String namespace, Request request) throws InstallException
    {
        if (request.getProperty(PROPERTY_CHECKRIGHTS) == Boolean.TRUE) {
            try {
                if (!hasAccessLevel("programming", namespace + "XWiki.XWikiPreferences", request)) {
                    if (namespace == null) {
                        throw new InstallException(String.format(
                            "Programming right is required to install extension [%s]", extension.getId()));
                    } else {
                        throw new InstallException(String.format(
                            "Programming right is required to install extension [%s] on namespace [%s]",
                            extension.getId(), namespace));
                    }
                }
            } catch (XWikiException e) {
                throw new InstallException("Failed to check rights", e);
            }
        }
    }

    @Override
    public void checkUninstall(InstalledExtension extension, String namespace, Request request)
        throws UninstallException
    {
        if (request.getProperty(PROPERTY_CHECKRIGHTS) == Boolean.TRUE) {
            try {
                if (!hasAccessLevel("programming", namespace + "XWiki.XWikiPreferences", request)) {
                    if (namespace == null) {
                        throw new UninstallException(String.format(
                            "Programming right is required to uninstall extension [%s]", extension.getId()));
                    } else {
                        throw new UninstallException(String.format(
                            "Programming right is required to uninstall extension [%s] from namespace [%s]",
                            extension.getId(), namespace));
                    }
                }
            } catch (XWikiException e) {
                throw new UninstallException("Failed to check rights", e);
            }
        }
    }
}
