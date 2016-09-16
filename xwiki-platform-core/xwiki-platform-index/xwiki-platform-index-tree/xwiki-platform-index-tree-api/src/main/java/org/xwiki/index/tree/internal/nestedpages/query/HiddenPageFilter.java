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
package org.xwiki.index.tree.internal.nestedpages.query;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;

/**
 * Filters hidden pages. This filter works with the named <strong>native SQL</strong> queries declared in the
 * {@code queries.hbm.xml} mapping file.
 * 
 * @version $Id$
 * @since 8.3RC1, 7.4.5
 */
@Component
@Named("hiddenPage/nestedPages")
@Singleton
public class HiddenPageFilter extends AbstractNestedPageFilter
{
    @Inject
    private HibernateSessionFactory sessionFactory;

    @Override
    protected String filterNestedPagesStatement(String statement)
    {
        // The constraint is different depending on whether we filter a native SQL query or an HQL query.
        String constraint =
            statement.indexOf("XWS_REFERENCE") < 0 ? "hidden <> true " : getHiddenConstraint("XWS_HIDDEN");
        return insertWhereConstraint(statement, constraint);
    }

    @Override
    protected String filterTerminalPagesStatement(String statement)
    {
        return statement + " and " + getHiddenConstraint("doc.XWD_HIDDEN");
    }

    private String getHiddenConstraint(String field)
    {
        // I don't know exactly why "field = false" is not enough since the hidden field is marked as non-null so it
        // should have only two values (true or false). I remember something related to Oracle but I don't know for
        // sure. Let's keep this for now, but the downside of using the <> (not-equal) operator instead of = (equal) is
        // that the database cannot use the index so the query is slower (which can be significant when we have
        // thousands of documents and spaces in the database).
        //
        // Note that we can't use the boolean literal here because Oracle doesn't support it (ORA-00904: "TRUE": invalid
        // identifier) and we can't use "1" (integer) also because PostgreSQL doesn't support automatic integer to
        // boolean conversion (PSQLException: ERROR: operator does not exist: boolean <> integer). We're forced to get
        // the boolean value from the SQL Dialect currently in use.
        return String.format("%s <> %s ", field, toBooleanValueString(true));
    }

    private String toBooleanValueString(boolean value)
    {
        Dialect dialect = ((SessionFactoryImplementor) this.sessionFactory.getSessionFactory()).getDialect();
        return dialect.toBooleanValueString(value);
    }
}
