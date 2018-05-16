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
package org.xwiki.security.authorization.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.EntityTypeNotSupportedException;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RightSet;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityEntryReader;
import org.xwiki.security.authorization.SecurityRule;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.internal.XWikiConstants;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * The default implementation of the security rules reader, which reads rules from documents in a wiki.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class DefaultSecurityEntryReader implements SecurityEntryReader
{
    /** A security rules to deny everyone the edit right by allowing edit to no one. */
    private static final SecurityRule DENY_EDIT = new AllowEditToNoOneRule();

    /** Right set allowed for main wiki owner. */
    private static final Set<Right> MAINWIKIOWNER_RIGHTS = new RightSet(Right.PROGRAM);

    /** Right set allowed for wiki owner. */
    private static final Set<Right> OWNER_RIGHTS = new RightSet(Right.ADMIN);

    /** Right set allowed for document creators. */
    private static final Set<Right> CREATOR_RIGHTS = new RightSet(Right.CREATOR);

    /** Resolver for user and group names. */
    @Inject
    @Named("user")
    private DocumentReferenceResolver<String> resolver;

    /** Execution object. */
    @Inject
    private Execution execution;

    /**
     * @return the current {@code XWikiContext}
     */
    private XWikiContext getXWikiContext()
    {
        return ((XWikiContext) execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY));
    }

    /**
     * Internal implementation of the SecurityRuleEntry.
     */
    private final class InternalSecurityRuleEntry extends AbstractSecurityRuleEntry
    {
        /** Reference of the related entity. */
        private final SecurityReference reference;

        /** The list of objects. */
        private final Collection<SecurityRule> rules;

        /**
         * @param reference reference of the related entity
         * @param rules collection of security rules applied on the entity.
         */
        private InternalSecurityRuleEntry(SecurityReference reference, Collection<SecurityRule> rules)
        {
            this.reference = reference;
            this.rules = Collections.unmodifiableCollection(rules);
        }

        /**
         * @return the reference of the related entity
         */
        @Override
        public SecurityReference getReference()
        {
            return reference;
        }

        /**
         * @return all rules available for this entity
         */
        @Override
        public Collection<SecurityRule> getRules()
        {
            return rules;
        }
    }

    /**
     * Load the rules from wiki documents.
     *
     * @param entity Any entity reference that is either a WIKI or a SPACE, or an entity containing a DOCUMENT entity.
     * @return the access rules that could be loaded into the cache.
     * @throws org.xwiki.security.authorization.AuthorizationException if an issue arise while reading these rules from
     *             the wiki.
     */
    @Override
    public SecurityRuleEntry read(SecurityReference entity) throws AuthorizationException
    {
        if (entity == null) {
            return null;
        }

        if (entity.getOriginalReference() == null) {
            // Public users (not logged in) are not stored anywhere and does not have their own rules
            // More generally, any reference without a valid original reference should not be considered.
            return new InternalSecurityRuleEntry(entity, Collections.<SecurityRule>emptyList());
        }

        DocumentReference documentReference;
        DocumentReference classReference;
        WikiReference wikiReference;

        switch (entity.getType()) {
            case WIKI:
                wikiReference = new WikiReference(entity);
                SpaceReference wikiSpace = new SpaceReference(XWikiConstants.XWIKI_SPACE, wikiReference);
                documentReference = new DocumentReference(XWikiConstants.WIKI_DOC, wikiSpace);
                classReference = new DocumentReference(XWikiConstants.GLOBAL_CLASSNAME, wikiSpace);
                break;
            case SPACE:
                wikiReference = new WikiReference(entity.extractReference(EntityType.WIKI));
                documentReference = new DocumentReference(XWikiConstants.SPACE_DOC, new SpaceReference(entity));
                classReference = new DocumentReference(XWikiConstants.GLOBAL_CLASSNAME,
                    new SpaceReference(XWikiConstants.XWIKI_SPACE, wikiReference));
                break;
            case DOCUMENT:
                wikiReference = new WikiReference(entity.extractReference(EntityType.WIKI));
                documentReference = new DocumentReference(entity);
                classReference = new DocumentReference(XWikiConstants.LOCAL_CLASSNAME,
                    new SpaceReference(XWikiConstants.XWIKI_SPACE, wikiReference));
                break;
            default:
                throw new EntityTypeNotSupportedException(entity.getType(), this);
        }

        // Get standard rules
        Collection<SecurityRule> rules = getSecurityRules(documentReference, classReference, wikiReference);

        // Add extras
        for (SecurityEntryReaderExtra extra : this.extras) {
            rules.addAll(extra.read(entity));
        }

        return new InternalSecurityRuleEntry(entity, rules);
    }

    /**
     * Get the document.
     * 
     * @param documentReference reference to the document to be loaded.
     * @return a list of matching base objects, or null if none where found.
     * @throws AuthorizationException if an unexpected error occurs during retrieval.
     */
    private XWikiDocument getDocument(DocumentReference documentReference) throws AuthorizationException
    {
        XWikiContext context = getXWikiContext();

        try {
            XWikiDocument doc = context.getWiki().getDocument(documentReference, context);
            if (doc == null || doc.isNew()) {
                return null;
            }
            return doc;
        } catch (XWikiException e) {
            throw new AuthorizationException(documentReference,
                "Could not retrieve the document to check security access", e);
        }
    }

    /**
     * @param wikiReference the wiki to look for owner
     * @return a reference to the owner of the wiki
     * @throws AuthorizationException if the owner could not be retrieved.
     */
    private DocumentReference getWikiOwner(WikiReference wikiReference) throws AuthorizationException
    {
        XWikiContext context = getXWikiContext();
        String wikiOwner;
        try {
            wikiOwner = context.getWiki().getWikiOwner(wikiReference.getName(), context);
        } catch (XWikiException e) {
            throw new AuthorizationException(wikiReference, "Could not retrieve the owner of this wiki", e);
        }

        if (wikiOwner == null) {
            return null;
        }

        return resolver.resolve(wikiOwner, wikiReference);
    }

    /**
     * Read right objects from an XWikiDocument and return them as XWikiSecurityRule.
     * 
     * @param documentReference reference to document to read
     * @param classReference reference to the right class to read
     * @param wikiReference reference to the wiki of the document
     * @return a collection of rules read from the document
     * @throws AuthorizationException on error reading object from the document
     */
    private Collection<SecurityRule> getSecurityRules(DocumentReference documentReference,
        DocumentReference classReference, WikiReference wikiReference) throws AuthorizationException
    {
        boolean isGlobalRightsReference = isGlobalRightsReference(documentReference);
        boolean isGlobalRightRequested = classReference.getName().equals(XWikiConstants.GLOBAL_CLASSNAME);
        XWikiDocument doc = getDocument(documentReference);

        // Get implied rules (creator, owner, global rights restriction)
        List<SecurityRule> securityRules =
            getImpliedRules(documentReference, doc, isGlobalRightsReference, isGlobalRightRequested);

        if (doc == null) {
            return securityRules;
        }

        // Convert existing rules on the entity
        List<BaseObject> baseObjects = doc.getXObjects(classReference);
        if (baseObjects != null) {
            for (BaseObject obj : baseObjects) {
                if (obj != null) {
                    SecurityRule rule;
                    try {
                        // Thanks to the resolver, the users and groups listed by the rights object, inherit
                        // the wiki from the document, unless explicitly given.
                        rule = XWikiSecurityRule.createNewRule(obj, resolver, wikiReference,
                            isGlobalRightsReference && !isGlobalRightRequested);
                    } catch (IllegalArgumentException e) {
                        // Do not add badly formed security rules.
                        continue;
                    }
                    securityRules.add(rule);
                }
            }
        }

        return securityRules;
    }

    /**
     * Get rules implied by wiki owners, document creators, and global rights documents.
     * 
     * @param documentReference reference to the document requested.
     * @param document the document requested.
     * @param isGlobalRightsReference true when the document is a document which host global rights.
     * @param isGlobalRightRequested true when the request concern global rights.
     * @return a list of implied security rules, or an empty list of there none.
     * @throws AuthorizationException if anything goes wrong.
     */
    private List<SecurityRule> getImpliedRules(DocumentReference documentReference, XWikiDocument document,
        boolean isGlobalRightsReference, boolean isGlobalRightRequested) throws AuthorizationException
    {
        List<SecurityRule> rules = new ArrayList<>();

        if (isGlobalRightsReference) {
            if (isGlobalRightRequested) {
                addImpliedGlobalRule(documentReference, rules);
            } else {
                // Deny local edit right on documents hosting global rights for anyone but admins.
                rules.add(DENY_EDIT);
            }
        }

        if (!isGlobalRightRequested && document != null) {
            DocumentReference creator = document.getCreatorReference();

            // Allow local rights to document creator (unless it is a public creator)
            if (creator != null && !XWikiConstants.GUEST_USER.equals(creator.getName())) {
                rules.add(new XWikiSecurityRule(CREATOR_RIGHTS, RuleState.ALLOW, Collections.singleton(creator), null));
            }
        }

        return rules;
    }

    private void addImpliedGlobalRule(DocumentReference documentReference, List<SecurityRule> rules)
        throws AuthorizationException
    {
        WikiReference documentWiki = documentReference.getWikiReference();
        DocumentReference owner = getWikiOwner(documentWiki);
        if (owner != null) {
            XWikiContext context = getXWikiContext();

            // Allow global rights to wiki owner
            if (context.isMainWiki(documentWiki.getName())) {
                rules.add(
                    new XWikiSecurityRule(MAINWIKIOWNER_RIGHTS, RuleState.ALLOW, Collections.singleton(owner), null));
            } else {
                rules.add(new XWikiSecurityRule(OWNER_RIGHTS, RuleState.ALLOW, Collections.singleton(owner), null));
            }
        }
    }

    /**
     * Check if the entity reference refers to a document that may contain global rights objects. In other words
     * '*:XWiki.XWikiPreferences' or '*:*.WebPreferences'.
     *
     * @param documentReference the document reference to check.
     * @return true if the document is scanned for global rights objects during authorization.
     */
    private boolean isGlobalRightsReference(DocumentReference documentReference)
    {
        return (XWikiConstants.SPACE_DOC.equals(documentReference.getName())
            || (XWikiConstants.WIKI_DOC.equals(documentReference.getName())
                && XWikiConstants.XWIKI_SPACE.equals(documentReference.getParent().getName())));
    }
}
