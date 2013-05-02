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
package org.xwiki.extension.distribution.internal;

import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.Utils;

import com.xpn.xwiki.web.XWikiAction;

/**
 * Action used to apply various distribution related actions. We create a special action to make sure to execute the
 * template with the current user rights.
 * 
 * @version $Id$
 * @since 4.2M3
 */
public class DistributionAction extends XWikiAction
{
    /**
     * The identifier of the distribution action.
     * 
     * @todo need an enumerated class for actions.
     */
    public static final String DISTRIBUTION_ACTION = "distribution";

    /**
     * The reference of the superadmin user document.
     */
    private static final DocumentReference SUPERADMIN_REFERENCE = new DocumentReference("xwiki", XWiki.SYSTEM_SPACE,
        XWikiRightService.SUPERADMIN_USER);

    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        context.put("action", DISTRIBUTION_ACTION);

        // Disallow template override with xpage parameter.
        if (!DISTRIBUTION_ACTION.equals(Utils.getPage(context.getRequest(), DISTRIBUTION_ACTION))) {
            throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                String.format("Template may not be overriden with 'xpage' in [%s] action.", DISTRIBUTION_ACTION));
        }

        // Make sure the user has the right to access the distribution action
        DistributionManager distributionManager = Utils.getComponent(DistributionManager.class);
        if (!distributionManager.canDisplayDistributionWizard()) {
            if (context.getUserReference() == null) {
                context.getWiki().getAuthService().showLogin(context);
                return false;
            } else {
                throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                    String.format("Current user need proper rights to access action [%s].", DISTRIBUTION_ACTION));
            }
        }

        // Make sure to have programming rights
        // TODO: find something nicer
        XWikiDocument document =
            new XWikiDocument(new DocumentReference(context.getDatabase(), XWiki.SYSTEM_SPACE, "Distribution"));
        document.setContentAuthorReference(SUPERADMIN_REFERENCE);
        document.setAuthorReference(SUPERADMIN_REFERENCE);
        document.setCreatorReference(SUPERADMIN_REFERENCE);
        context.setDoc(document);

        return true;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        return DISTRIBUTION_ACTION;
    }
}
