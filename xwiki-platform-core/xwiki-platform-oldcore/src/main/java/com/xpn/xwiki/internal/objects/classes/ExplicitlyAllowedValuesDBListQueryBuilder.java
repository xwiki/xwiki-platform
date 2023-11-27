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
package com.xpn.xwiki.internal.objects.classes;

import java.io.StringWriter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryBuilder;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.objects.classes.DBListClass;

/**
 * Builds a secure query from the HQL statement specified by a Database List property.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Named("explicitlyAllowedValues")
@Singleton
public class ExplicitlyAllowedValuesDBListQueryBuilder implements QueryBuilder<DBListClass>
{
    @Inject
    private Logger logger;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private AuthorExecutor authorExecutor;

    @Inject
    private Provider<VelocityManager> velocityManagerProvider;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    @Named("secure")
    private QueryManager secureQueryManager;

    @Override
    public Query build(DBListClass dbListClass) throws QueryException
    {
        String statement = dbListClass.getSql();
        DocumentReference authorReference = dbListClass.getOwnerDocument().getAuthorReference();
        DocumentReference documentReference = dbListClass.getOwnerDocument().getDocumentReference();
        if (this.authorizationManager.hasAccess(Right.SCRIPT, authorReference, documentReference)) {
            String namespace = this.entityReferenceSerializer.serialize(dbListClass.getReference());
            try {
                statement = this.authorExecutor.call(() -> evaluateVelocityCode(dbListClass.getSql(), namespace),
                    authorReference, documentReference);
            } catch (Exception e) {
                this.logger.warn(
                    "Failed to evaluate the Velocity code from the query [{}]."
                        + " Root cause is [{}]. Continuing with the raw query.",
                    statement, ExceptionUtils.getRootCauseMessage(e));
            }
        }

        Query query = this.secureQueryManager.createQuery(statement, Query.HQL);
        query.setWiki(documentReference.getWikiReference().getName());
        return query;
    }

    private String evaluateVelocityCode(String code, String namespace) throws Exception
    {
        VelocityManager velocityManager = this.velocityManagerProvider.get();
        VelocityContext velocityContext = velocityManager.getVelocityContext();
        VelocityEngine velocityEngine = velocityManager.getVelocityEngine();

        StringWriter writer = new StringWriter();
        velocityEngine.evaluate(velocityContext, writer, namespace, code);
        return writer.toString();
    }
}
