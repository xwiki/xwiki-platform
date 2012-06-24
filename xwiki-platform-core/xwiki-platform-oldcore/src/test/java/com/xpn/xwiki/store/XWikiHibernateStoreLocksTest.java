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
package com.xpn.xwiki.store;

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.store.migration.DataMigrationManager;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWiki;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Query;
import org.hibernate.Transaction;
import org.jmock.Expectations;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.legacy.ClassImposteriser;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;


/**
 * Make sure the user's locks are released when they logout.
 *
 * @version $Id$
 * @since 4.1M1
 */
public class XWikiHibernateStoreLocksTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private XWikiHibernateStore xhs;

    private ObservationManager observationManager;

    private final EventListener[] listener = new EventListener[1];

    @Override
    public void configure() throws Exception
    {
        // Needed because XHS has initializers which depend on Utils.
        Utils.setComponentManager(this.getComponentManager());

        final ObservationManager om =
            this.getComponentManager().getInstance(ObservationManager.class);
        this.observationManager = om;
        this.getMockery().checking(new Expectations() {{
            oneOf(om).addListener(with(new BaseMatcher<EventListener>() {
                public void describeTo(final Description d)
                {
                    d.appendText("See if the listener is a deleteLocksOnLogoutListener.");
                }

                public boolean matches(final Object o)
                {
                    return ((EventListener) o).getName().equals("deleteLocksOnLogoutListener");
                }
            }));
                will(new CustomAction("grab the EventListener so it can be called") {
                    public Object invoke(org.jmock.api.Invocation invocation) throws Exception
                    {
                        listener[0] = (EventListener) invocation.getParameter(0);
                        return null;
                    }
                });
        }});

        final HibernateSessionFactory xhsf =
            this.getComponentManager().getInstance(HibernateSessionFactory.class);
        final SessionFactory hsf = this.getMockery().mock(SessionFactory.class, "hsf");
        final Session session = this.getMockery().mock(org.hibernate.classic.Session.class);
        this.getMockery().checking(new Expectations() {{
            oneOf(xhsf).getSessionFactory(); will(returnValue(hsf));
            oneOf(hsf).openSession(); will(returnValue(session));
        }});

        final Query mockQuery = this.getMockery().mock(Query.class);
        final Transaction mockTransaction = this.getMockery().mock(Transaction.class);
        this.getMockery().checking(new Expectations() {{
            exactly(2).of(session).setFlushMode(FlushMode.COMMIT);
            oneOf(session).createQuery("delete from XWikiLock as lock where lock.userName=:userName");
                will(returnValue(mockQuery));
            oneOf(mockQuery).setString("userName", "XWiki.LoggerOutter");
            oneOf(mockQuery).executeUpdate();
            oneOf(session).beginTransaction();
                will(returnValue(mockTransaction));
            oneOf(mockTransaction).commit();
            oneOf(session).close();
        }});

        // setDatabase() is called for each transaction and that calls checkDatabase().
        final DataMigrationManager dmm =
            this.getComponentManager().getInstance(DataMigrationManager.class, "hibernate");
        this.getMockery().checking(new Expectations() {{
            oneOf(dmm).checkDatabase();
        }});


        // initialize() gets the xcontext from the execution then uses that
        // to get the path to the hibernate.cfg.xml
        this.getMockery().setImposteriser(ClassImposteriser.INSTANCE);
        final Execution exec = this.getComponentManager().getInstance(Execution.class);
        final ExecutionContext execCtx = this.getMockery().mock(ExecutionContext.class);
        final XWikiContext xc = new XWikiContext();
        xc.setWiki(this.getMockery().mock(XWiki.class));
        this.getMockery().checking(new Expectations() {{
            oneOf(exec).getContext(); will(returnValue(execCtx));
            oneOf(execCtx).getProperty("xwikicontext"); will(returnValue(xc));
            oneOf(xc.getWiki()).Param(with("xwiki.store.hibernate.path"), with(any(String.class)));
                will(returnValue("unimportant"));
        }});
    }

    @Test
    public void testLocksAreReleasedOnLogout()
    {
        Assert.assertNotNull(this.listener[0]);
        final XWikiContext xc = new XWikiContext();
        xc.setUserReference(new DocumentReference("xwiki", "XWiki", "LoggerOutter"));
        this.listener[0].onEvent(new ActionExecutingEvent("logout"), null, xc);
    }
}
