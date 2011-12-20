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
 *
 */
package org.xwiki.security;

import java.util.HashMap;
import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.Utils;

/**
 * Class for plugging in to xwiki.
 * @version $Id$
 */
public class XWikiCachingRightService implements XWikiRightService
{
    /**
     * Specialized map with a chainable put action to avoid exceeding code complexity during initialization.
     */
    private static class ActionMap extends HashMap<String, Right>
    {
        /** Serialization identifier for conformance to Serializable. */
        private static final long serialVersionUID = 1;

        /** Allow filling the map in the initializer without exceeding code complexity.
         * @param action the action name
         * @param right the corresponding right required
         * @return this action map to allow code chaining
         */
        public ActionMap putAction(String action, Right right)
        {
            put(action, right);
            return this;
        }
    }

    /** 
     * Map containing all known actions. 
     * TODO: Remove this mapping here, since each action should know about itself
     */
    private static final ActionMap ACTION_MAP = new ActionMap();
    
    static {
        ACTION_MAP
            .putAction("login", Right.LOGIN)
            .putAction("view", Right.VIEW)
            .putAction("delete", Right.DELETE)
            .putAction("admin", Right.ADMIN)
            .putAction("programing", Right.PROGRAM)
            .putAction("edit", Right.EDIT)
            .putAction("register", Right.REGISTER)
            .putAction("logout", Right.LOGIN)
            .putAction("loginerror", Right.LOGIN)
            .putAction("loginsubmit", Right.LOGIN)
            .putAction("viewrev", Right.VIEW)
            .putAction("get", Right.VIEW)
                // .putAction("downloadrev", "download"); Huh??
            .putAction("downloadrev", Right.VIEW)
            .putAction("plain", Right.VIEW)
            .putAction("raw", Right.VIEW)
            .putAction("attach", Right.VIEW)
            .putAction("charting", Right.VIEW)
            .putAction("skin", Right.VIEW)
            .putAction("download", Right.VIEW)
            .putAction("dot", Right.VIEW)
            .putAction("svg", Right.VIEW)
            .putAction("pdf", Right.VIEW)
            .putAction("deleteversions", Right.ADMIN)
                // .putAction("undelete", "undelete"); Huh??
            .putAction("undelete", Right.EDIT)
            .putAction("reset", Right.DELETE)
            .putAction("commentadd", Right.COMMENT)
            .putAction("redirect", Right.VIEW)
            .putAction("export", Right.VIEW)
            .putAction("import", Right.ADMIN)
            .putAction("jsx", Right.VIEW)
            .putAction("ssx", Right.VIEW)
            .putAction("tex", Right.VIEW)
            .putAction("unknown", Right.VIEW)
            .putAction("save", Right.EDIT)
            .putAction("preview", Right.EDIT)
            .putAction("lock", Right.EDIT)
            .putAction("cancel", Right.EDIT)
            .putAction("delattachment", Right.EDIT)
            .putAction("inline", Right.EDIT)
            .putAction("propadd", Right.EDIT)
            .putAction("propupdate", Right.EDIT)
            .putAction("propdelete", Right.EDIT)
            .putAction("objectadd", Right.EDIT)
            .putAction("objectremove", Right.EDIT)
            .putAction("objectsync", Right.EDIT)
            .putAction("rollback", Right.EDIT)
            .putAction("upload", Right.EDIT)
            .putAction("create", Right.EDIT);
    }

    /**
     * Map an action represented by a string to a right.
     * @param action String representation of action.
     * @return right The corresponding Right instance, or
     * {@code ILLEGAL}.
     */
    public static Right actionToRight(String action)
    {
        Right right = ACTION_MAP.get(action);
        if (right == null) {
            return Right.ILLEGAL;
        }
        return right;
    }

    /** The actual right service compoennt. */
    private final RightService rightService;

    {
        RightServiceConfigurationManager m;
        m = Utils.getComponent(RightServiceConfigurationManager.class);
        rightService = m.getConfiguredRightService();
    }

    @Override
    public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        return rightService.checkAccess(actionToRight(action), doc, context);
    }
 
    @Override
    public boolean hasAccessLevel(String right, String username, String docname, XWikiContext context)
        throws XWikiException
    {
        return rightService.hasAccessLevel(Right.toRight(right), username, docname, context);
    }

    @Override
    public boolean hasProgrammingRights(XWikiContext context)
    {
        return rightService.hasProgrammingRights(context);
    }

    @Override
    public boolean hasProgrammingRights(XWikiDocument doc, XWikiContext context)
    {
        return rightService.hasProgrammingRights(doc, context);
    }

    @Override
    public boolean hasAdminRights(XWikiContext context)
    {
        return rightService.hasAdminRights(context);
    }

    @Override
    public boolean hasWikiAdminRights(XWikiContext context)
    {
        return rightService.hasWikiAdminRights(context);
    }

    @Override
    public List<String> listAllLevels(XWikiContext context)
        throws XWikiException
    {
        return rightService.listAllLevels(context);
    }
}
