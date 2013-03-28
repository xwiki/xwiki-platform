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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.EntityTypeNotSupportedException;
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
    private static final SecurityRule ALLOW_EDIT = new AllowEditToNoOneRule();

    /** Logger. **/
    @Inject
    private Logger logger;
    
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
    private XWikiContext getXWikiContext() {
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
         * @return all rules available for this entity
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
     * @throws org.xwiki.security.authorization.AuthorizationException if an issue arise while reading these rules
     *         from the wiki.
     */
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
            default:
                EntityReference relatedDocument = entity.extractReference(EntityType.DOCUMENT);
                if (relatedDocument != null) {
                    wikiReference = new WikiReference(relatedDocument.extractReference(EntityType.WIKI));
                    documentReference = new DocumentReference(relatedDocument);
                    classReference = new DocumentReference(XWikiConstants.LOCAL_CLASSNAME,
                        new SpaceReference(XWikiConstants.XWIKI_SPACE, wikiReference));
                } else {
                    this.logger.debug("Rights on entities of type {} is not supported by this reader!",
                                      entity.getType());
                    throw new EntityTypeNotSupportedException(entity.getType(), this);
                }
        }

        return new InternalSecurityRuleEntry(entity,
            getSecurityRules(documentReference, classReference, wikiReference));
    }

    /**
     * Load Right object from the document.
     * @param documentReference reference to the document to be loaded.
     * @param classReference reference to the class of object to load.
     * @return a list of matching base objects, or null if none where found.
     * @throws AuthorizationException if an unexpected error occurs during retrieval.
     */
    private List<BaseObject> getXObjects(DocumentReference documentReference, DocumentReference classReference)
        throws AuthorizationException
    {
        XWikiContext context = getXWikiContext();

        try {
            XWikiDocument doc = context.getWiki().getDocument(documentReference, context);
            if (doc == null || doc.isNew()) {
                return null;
            }
            return doc.getXObjects(classReference);
        } catch (XWikiException e) {
            throw new AuthorizationException(documentReference, "Couldn't read right objects", e);
        }
    }

    /**
     * Read right objects from an XWikiDocument and return them as XWikiSecurityRule.
     * @param documentReference reference to document to read
     * @param classReference reference to the right class to read
     * @param wikiReference reference to the wiki of the document
     * @return a collection of rules read from the document
     * @throws AuthorizationException on error reading object from the document
     */
    private Collection<SecurityRule> getSecurityRules(DocumentReference documentReference,
        DocumentReference classReference, WikiReference wikiReference) throws AuthorizationException
    {

        List<BaseObject> baseObjects;
        List<SecurityRule> securityRules = new ArrayList<SecurityRule>();

        boolean isGlobalRightsDocument = isGlobalRightsReference(documentReference)
            && !classReference.getName().equals(XWikiConstants.GLOBAL_CLASSNAME);

        if (isGlobalRightsDocument) {
            securityRules.add(ALLOW_EDIT);
        }

        baseObjects = getXObjects(documentReference, classReference);
        if (baseObjects == null) {
            return isGlobalRightsDocument ? securityRules : Collections.<SecurityRule>emptyList();
        }

        if (baseObjects != null) {
            for (BaseObject obj : baseObjects) {
                if (obj != null) {
                    SecurityRule rule;
                    try {
                        // Thanks to the resolver, the users and groups listed by the rights object, inherit
                        // the wiki from the document, unless explicitly given.
                        rule = XWikiSecurityRule.createNewRule(obj, resolver, wikiReference, isGlobalRightsDocument);
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
     * Check if the entity reference refers to a document that may contain global rights objects.  In other words
     * '*:XWiki.XWikiPreferences' or '*:*.WebPreferences'.
     *
     * @param documentReference the document reference to check.
     * @return true if the document is scanned for global rights objects during authorization.
     */
    private boolean isGlobalRightsReference(DocumentReference documentReference) {
        return documentReference.getType() == EntityType.DOCUMENT
            && (XWikiConstants.SPACE_DOC.equals(documentReference.getName())
            || (XWikiConstants.WIKI_DOC.equals(documentReference.getName())
            && XWikiConstants.XWIKI_SPACE.equals(documentReference.getParent().getName())));
    }
}

