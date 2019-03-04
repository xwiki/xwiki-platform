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
package org.xwiki.extension.xar.question;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 * @since 4.0M2
 */
public class ConflictQuestion
{
    private static final String PREFIX_REQUEST_CONFLICT_DEFAULTANSWER = "extension.xar.packager.conflict.always.";

    /**
     * @see ConflictType#MERGE_SUCCESS
     * @since 9.2RC1
     */
    public static final String REQUEST_CONFLICT_DEFAULTANSWER_MERGE_SUCCESS = toKey(ConflictType.MERGE_SUCCESS);

    /**
     * @see ConflictType#MERGE_FAILURE
     * @since 9.2RC1
     */
    public static final String REQUEST_CONFLICT_DEFAULTANSWER_MERGE_FAILURE = toKey(ConflictType.MERGE_FAILURE);

    /**
     * @see ConflictType#CURRENT_EXIST
     * @since 9.2RC1
     */
    public static final String REQUEST_CONFLICT_DEFAULTANSWER_CURRENT_EXIST = toKey(ConflictType.CURRENT_EXIST);

    /**
     * @see ConflictType#CURRENT_DELETED
     * @since 9.2RC1
     */
    public static final String REQUEST_CONFLICT_DEFAULTANSWER_CURRENT_DELETED = toKey(ConflictType.CURRENT_DELETED);

    public static String toKey(ConflictType type)
    {
        return PREFIX_REQUEST_CONFLICT_DEFAULTANSWER + type;
    }

    /**
     * The type of conflict.
     * 
     * @version $Id$
     * @since 9.2RC1
     */
    public enum ConflictType
    {
        /**
         * 3 ways merge succeed (at least low level from 3 ways merge point of view).
         */
        MERGE_SUCCESS(GlobalAction.ASK, GlobalAction.CURRENT, GlobalAction.MERGED, GlobalAction.NEXT, GlobalAction.PREVIOUS),

        /**
         * 3 ways merge failed.
         */
        MERGE_FAILURE(GlobalAction.ASK, GlobalAction.CURRENT, GlobalAction.MERGED, GlobalAction.NEXT, GlobalAction.PREVIOUS),

        /**
         * Already existing different document.
         */
        CURRENT_EXIST(GlobalAction.ASK, GlobalAction.CURRENT, GlobalAction.NEXT),

        /**
         * Already deleted current document.
         */
        CURRENT_DELETED(GlobalAction.ASK, GlobalAction.CURRENT, GlobalAction.PREVIOUS, GlobalAction.NEXT);

        private final List<GlobalAction> actions;

        private ConflictType(GlobalAction... actions)
        {
            this.actions = Collections.unmodifiableList(Arrays.asList(actions));
        }

        public List<GlobalAction> getActions()
        {
            return this.actions;
        }
    };

    public enum GlobalAction
    {
        /**
         * The document in the database.
         */
        CURRENT,

        /**
         * The document from the previous version of the extension.
         */
        PREVIOUS,

        /**
         * The document from the new version of the extension.
         */
        NEXT,

        /**
         * A proposal created by the 3 ways merge.
         */
        MERGED,

        /**
         * A custom version.
         */
        CUSTOM,

        /**
         * Used to force asking whatever is the default. Should not be used as an answer to the question but to
         * configure default answers in the request for example.
         */
        ASK
    };

    // Question datas

    private final ConflictType type;

    private final XWikiDocument currentDocument;

    private final XWikiDocument previousDocument;

    private final XWikiDocument nextDocument;

    private final XWikiDocument mergedDocument;

    // Answer datas

    private GlobalAction globalAction;

    private XWikiDocument customDocument;

    private boolean always;

    /**
     * @deprecated since 9.2RC1, use
     */
    @Deprecated
    public ConflictQuestion(XWikiDocument currentDocument, XWikiDocument previousDocument, XWikiDocument nextDocument,
        XWikiDocument mergedDocument)
    {
        this(currentDocument, previousDocument, nextDocument, mergedDocument, null);
    }

    /**
     * @since 9.2RC1
     */
    public ConflictQuestion(XWikiDocument currentDocument, XWikiDocument previousDocument, XWikiDocument nextDocument,
        XWikiDocument mergedDocument, ConflictType type)
    {
        this.currentDocument = currentDocument;
        this.previousDocument = previousDocument;
        this.nextDocument = nextDocument;
        this.mergedDocument = mergedDocument;
        this.type = type;

        if (this.type == ConflictType.CURRENT_EXIST) {
            // Use next version by default if it's not a merge
            setGlobalAction(GlobalAction.NEXT);
        } else if (this.type == ConflictType.CURRENT_DELETED) {
            // Don't restore delete document by default
            setGlobalAction(GlobalAction.CURRENT);
        } else {
            setGlobalAction(GlobalAction.MERGED);
        }
    }

    /**
     * @return the type of conflict
     * @since 9.2RC1
     */
    public ConflictType getType()
    {
        return this.type;
    }

    public XWikiDocument getCurrentDocument()
    {
        return this.currentDocument;
    }

    public XWikiDocument getPreviousDocument()
    {
        return this.previousDocument;
    }

    public XWikiDocument getNextDocument()
    {
        return this.nextDocument;
    }

    public XWikiDocument getMergedDocument()
    {
        return this.mergedDocument;
    }

    // Answer

    public GlobalAction getGlobalAction()
    {
        return this.globalAction;
    }

    public void setGlobalAction(GlobalAction globalAction)
    {
        this.globalAction = globalAction;
    }

    /**
     * @since 4.1
     */
    public XWikiDocument getCustomDocument()
    {
        return this.customDocument;
    }

    /**
     * @since 4.1
     */
    public void setCustomDocument(XWikiDocument customDocument)
    {
        this.customDocument = customDocument;
        this.globalAction = GlobalAction.CUSTOM;
    }

    /**
     * @return if true apply the same answer to following conflict. Does nothing if the answer is
     *         {@link GlobalAction#CUSTOM}.
     * @since 5.1RC1
     */
    public boolean isAlways()
    {
        return this.always;
    }

    /**
     * @param always if true apply the same answer to following conflict. Does nothing if the answer is
     *            {@link GlobalAction#CUSTOM}.
     * @since 5.1RC1
     */
    public void setAlways(boolean always)
    {
        this.always = always;
    }
}
