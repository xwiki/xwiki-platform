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
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.authorization.SecurityEntryReader;
import org.xwiki.security.authorization.SecurityRule;

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
        DocumentReference documentReference;
        DocumentReference classReference;
        WikiReference wikiReference;

        switch (entity.getType()) {
            case WIKI:
                wikiReference = new WikiReference(entity);
                SpaceReference wikiSpace = new SpaceReference(XWikiConstants.WIKI_SPACE, wikiReference);
                documentReference = new DocumentReference(XWikiConstants.WIKI_DOC, wikiSpace);
                classReference = new DocumentReference(XWikiConstants.GLOBAL_CLASSNAME, wikiSpace);
                break;
            case SPACE:
                wikiReference = new WikiReference(entity.extractReference(EntityType.WIKI));
                documentReference = new DocumentReference(XWikiConstants.SPACE_DOC, new SpaceReference(entity));
                classReference = new DocumentReference(XWikiConstants.GLOBAL_CLASSNAME,
                    new SpaceReference(XWikiConstants.WIKI_SPACE, wikiReference));
                break;
            default:
                EntityReference relatedDocument = entity.extractReference(EntityType.DOCUMENT);
                if (relatedDocument != null) {
                    wikiReference = new WikiReference(relatedDocument.extractReference(EntityType.WIKI));
                    documentReference = new DocumentReference(relatedDocument);
                    classReference = new DocumentReference(XWikiConstants.LOCAL_CLASSNAME,
                        new SpaceReference(XWikiConstants.WIKI_SPACE, wikiReference));
                } else {
                    this.logger.debug("Rights on entities of type {} is not supported by this reader!",
                                      entity.getType());
                    throw new EntityTypeNotSupportedException(entity.getType(), this);
                }
        }

        XWikiContext context = getXWikiContext();
        List<BaseObject> baseObjects;
        List<SecurityRule> securityRules = new ArrayList<SecurityRule>();

        try {
            XWikiDocument doc = context.getWiki().getDocument(documentReference, context);
            if (doc == null) {
                return new InternalSecurityRuleEntry(entity, Collections.<SecurityRule>emptyList());
            }
            baseObjects = doc.getXObjects(classReference);
        } catch (XWikiException e) {
            throw new AuthorizationException(documentReference, "Couldn't read right objects", e);
        }

        if (baseObjects != null) {
            for (BaseObject obj : baseObjects) {
                if (obj != null) {
                    // The users and groups listed by the rights object, inherit
                    // the wiki from the document, unless explicitly given.
                    securityRules.add(new XWikiSecurityRule(obj, resolver, wikiReference));
                }
            }
        }

        return new InternalSecurityRuleEntry(entity, securityRules);
    }
}

