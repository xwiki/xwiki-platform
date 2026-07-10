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
package org.xwiki.job.store.internal;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.xwiki.job.store.internal.hibernate.JobStatusHibernateExecutor;
import org.xwiki.job.store.internal.hibernate.JobStatusHibernateStore;
import org.xwiki.store.hibernate.internal.HibernateCfgXmlLoader;
import org.xwiki.store.hibernate.internal.datasource.DBCPHibernateDataSourceProvider;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.xstream.internal.SafeXStream;
import org.xwiki.xstream.internal.XStreamUtils;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Meta-annotation that registers the shared Hibernate components needed by job status integration tests.
 * <p>
 * Used on {@link AbstractJobStatusHibernateTest} and inherited by all concrete test classes.
 *
 * @version $Id$
 */
@ComponentList({
    DatabaseLoggerTail.class,
    HibernateCfgXmlLoader.class,
    JobStatusHibernateStore.class,
    JobStatusHibernateExecutor.class,
    DBCPHibernateDataSourceProvider.class,
    SafeXStream.class,
    XStreamUtils.class,
    TestEnvironment.class
})
@Inherited
@Retention(RUNTIME)
@Target(TYPE)
@interface JobStatusHibernateComponentList
{
}
