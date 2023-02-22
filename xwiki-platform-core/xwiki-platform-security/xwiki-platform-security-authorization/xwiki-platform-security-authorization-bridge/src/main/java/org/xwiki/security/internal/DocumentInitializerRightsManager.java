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
package org.xwiki.security.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static java.util.stream.Collectors.joining;
import static org.xwiki.security.authorization.Right.DELETE;
import static org.xwiki.security.authorization.Right.EDIT;
import static org.xwiki.security.authorization.Right.VIEW;
import static org.xwiki.security.internal.XWikiConstants.ALLOW_FIELD_NAME;
import static org.xwiki.security.internal.XWikiConstants.GROUPS_FIELD_NAME;
import static org.xwiki.security.internal.XWikiConstants.LEVELS_FIELD_NAME;
import static org.xwiki.security.internal.XWikiConstants.LOCAL_CLASS_REFERENCE;

/**
 * Service provided to {@link MandatoryDocumentInitializer}s to initialize documents with the correct rights.
 *
 * @version $Id$
 * @since 14.4.8
 * @since 14.10.5
 * @since 15.1RC1
 */
@Component(roles = DocumentInitializerRightsManager.class)
@Singleton
public class DocumentInitializerRightsManager
{
    private static final String XWIKI_ADMIN_GROUP_DOCUMENT_REFERENCE = "XWiki.XWikiAdminGroup";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    /**
     * Restrict the rights of the provided document so that it can only be viewed, edited and deleted by the
     * {@code XWiki.XWikiAdminGroup} group. Note that this restriction is only applied if no rights are already applied
     * on the current document.
     *
     * @param document the document to updated rights on
     * @return {@code true} if the document has been modified, {@code false} otherwise (including if the document hasn't
     *     been modified because of an error during the modification)
     */
    public boolean restrictToAdmin(XWikiDocument document)
    {
        boolean updated = false;
        List<Right> rights = List.of(VIEW, EDIT, DELETE);

        if (fixBadlyInitializedRights(document, rights)) {
            updated = true;
        }

        // If some rights have already been set on the document, we consider that it has already been protected 
        // manually.
        if (document.getXObjects(LOCAL_CLASS_REFERENCE).isEmpty()) {
            updated = updated || initializeRights(document, rights);
        }

        return updated;
    }

    private boolean initializeRights(XWikiDocument document, List<Right> rights)
    {
        boolean updated = false;

        try {
            BaseObject object = document.newXObject(LOCAL_CLASS_REFERENCE, this.xcontextProvider.get());
            setRights(object, rights);
            updated = true;
        } catch (XWikiException e) {
            this.logger.error(String.format("Error adding a [%s] object to the document [%s]", LOCAL_CLASS_REFERENCE,
                document.getDocumentReference()));
        }

        return updated;
    }

    /**
     * Because of XWIKI-20519, the levels of the rights of {@code XWiki.XWikiAdminGroup} can be badly initialized, and
     * needs to be fixed. This is the case when upgrading from instances running exactly version 14.10.5 or 15.1-rc-1.
     *
     * @param document the document to fix
     * @param rights the rights to set to the {@code XWiki.XWikiAdminGroup} group
     */
    private boolean fixBadlyInitializedRights(XWikiDocument document, List<Right> rights)
    {
        boolean updated = false;
        if (document.getXObjects(LOCAL_CLASS_REFERENCE).size() == 1) {
            BaseObject object = document.getXObject(LOCAL_CLASS_REFERENCE);
            if (StringUtils.isEmpty(object.getStringValue(LEVELS_FIELD_NAME))
                && StringUtils.isEmpty(object.getLargeStringValue(GROUPS_FIELD_NAME)))
            {
                setRights(object, rights);
                updated = true;
            }
        }
        return updated;
    }

    private void setRights(BaseObject object, List<Right> rights)
    {
        object.setLargeStringValue(GROUPS_FIELD_NAME, XWIKI_ADMIN_GROUP_DOCUMENT_REFERENCE);
        object.setStringValue(LEVELS_FIELD_NAME, rights.stream().map(Right::getName).collect(joining(",")));
        object.setIntValue(ALLOW_FIELD_NAME, 1);
    }
}
