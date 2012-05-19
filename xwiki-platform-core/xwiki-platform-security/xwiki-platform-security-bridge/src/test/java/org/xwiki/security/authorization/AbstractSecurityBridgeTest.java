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
package org.xwiki.security.authorization;

import org.xwiki.test.annotation.MockingRequirement;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;
import org.jmock.lib.action.CustomAction;
import org.jmock.api.Invocation;

import org.junit.Before;
import org.junit.BeforeClass;

import java.util.Map;
import java.util.HashMap;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;
import org.xwiki.model.reference.EntityReferenceSerializer;

public abstract class AbstractSecurityBridgeTest extends AbstractMockingComponentTestCase
{

    private final static String TEST_WIKI_DEFINITIONS_DIRECTORY = "testwikis";

    private Map<String, XWiki> wikis;

    private String mainWiki;

    private XWikiContext context;

    private String currentDatabase;

    @Before
    public void setupLegacy() throws Exception
    {
        Utils.setComponentManager(getComponentManager());
        getMockery().setImposteriser(ClassImposteriser.INSTANCE);
        context = getMockery().mock(XWikiContext.class);
        new Expectations() {{
            allowing(context).setDatabase(with(any(String.class)));
            will(new CustomAction("Set the current database") {
                    @Override
                    public Object invoke(Invocation invocation) {
                        currentDatabase = (String) invocation.getParameter(0);
                        return null;
                    }
                });
            allowing(context).getDatabase(); will(returnValue(currentDatabase));
        }};
    }


    protected Map<String, XWiki> getWikis() {
        return wikis;
    }

    protected String getMainWiki() {
        return mainWiki;
    }

}
