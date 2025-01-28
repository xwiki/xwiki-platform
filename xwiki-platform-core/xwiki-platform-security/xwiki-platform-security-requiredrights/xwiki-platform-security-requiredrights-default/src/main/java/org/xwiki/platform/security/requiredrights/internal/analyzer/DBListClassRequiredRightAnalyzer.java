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
package org.xwiki.platform.security.requiredrights.internal.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.display.BlockSupplierProvider;
import org.xwiki.query.Query;
import org.xwiki.query.QueryBuilder;
import org.xwiki.query.QueryException;
import org.xwiki.query.hql.internal.HQLStatementValidator;
import org.xwiki.velocity.internal.util.VelocityDetector;

import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * Required rights analyzer for {@link DBListClass}.
 *
 * @since 15.10.16
 * @since 16.4.7
 * @since 16.10.2
 * @version $Id$
 */
@Component
@Singleton
public class DBListClassRequiredRightAnalyzer implements RequiredRightAnalyzer<DBListClass>
{
    @Inject
    private VelocityDetector velocityDetector;

    @Inject
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @Inject
    @Named("stringCode")
    private BlockSupplierProvider<String> stringCodeBlockSupplierProvider;

    @Inject
    private QueryBuilder<DBListClass> dbListClassQueryBuilder;

    @Inject
    private HibernateStore hibernate;

    @Inject
    private HQLStatementValidator hqlStatementValidator;

    @Inject
    private RequiredRightAnalyzer<PropertyClass> propertyClassRequiredRightAnalyzer;

    @Override
    public List<RequiredRightAnalysisResult> analyze(DBListClass dbListClass) throws RequiredRightsException
    {
        List<RequiredRightAnalysisResult> results =
            new ArrayList<>(this.propertyClassRequiredRightAnalyzer.analyze(dbListClass));

        // Empty HQL is considered safe.
        if (dbListClass != null && StringUtils.isNotBlank(dbListClass.getSql())) {
            String sql = dbListClass.getSql();
            // For Velocity code, we have no idea what the actual query is, but it's the same as Velocity, either
            // script (for Velocity) or programming (for Velocity and/or the query).
            if (this.velocityDetector.containsVelocityScript(sql)) {
                results.add(new RequiredRightAnalysisResult(dbListClass.getReference(),
                    this.translationMessageSupplierProvider.get("security.requiredrights.class.dbListVelocity"),
                    this.stringCodeBlockSupplierProvider.get(sql),
                    RequiredRight.SCRIPT_AND_MAYBE_PROGRAM));
            } else {
                // For a plain query, do a more thorough analysis by checking the actual query.
                try {
                    Query query = this.dbListClassQueryBuilder.build(dbListClass);
                    results.addAll(validateQuery(dbListClass, query));
                } catch (QueryException e) {
                    results.add(new RequiredRightAnalysisResult(dbListClass.getReference(),
                        this.translationMessageSupplierProvider.get(
                            "security.requiredrights.class.errorParsingQuery", ExceptionUtils.getRootCauseMessage(e)),
                        this.stringCodeBlockSupplierProvider.get(sql),
                        List.of(RequiredRight.MAYBE_PROGRAM))
                    );
                }
            }
        }

        return results;
    }

    private List<RequiredRightAnalysisResult> validateQuery(DBListClass dbListClass, Query query) throws QueryException
    {
        List<RequiredRightAnalysisResult> results = new ArrayList<>();

        if (query.isNamed()) {
            Optional<String> hqlQuery = getNamedQuery(query);
            // Assume unknown named queries are unsafe.
            if (hqlQuery.isEmpty() || !this.hqlStatementValidator.isSafe(hqlQuery.get())) {
                results.add(new RequiredRightAnalysisResult(dbListClass.getReference(),
                    this.translationMessageSupplierProvider.get(
                        "security.requiredrights.class.unsafeNamedQuery"),
                    this.stringCodeBlockSupplierProvider.get(query.getStatement()),
                    List.of(RequiredRight.PROGRAM)
                ));
            }
        } else if (!this.hqlStatementValidator.isSafe(query.getStatement())) {
            results.add(new RequiredRightAnalysisResult(dbListClass.getReference(),
                this.translationMessageSupplierProvider.get(
                    "security.requiredrights.class.unsafeQuery"),
                this.stringCodeBlockSupplierProvider.get(query.getStatement()),
                List.of(RequiredRight.PROGRAM)
            ));
        }

        return results;
    }

    private Optional<String> getNamedQuery(Query query)
    {
        return Optional.ofNullable(this.hibernate.getConfiguration().getNamedQueries().get(query.getStatement()))
            .map(NamedQueryDefinition::getQuery);
    }
}
