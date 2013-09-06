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
package org.xwiki.activeinstalls.test.ui;

import org.junit.runners.model.*;
import org.xwiki.test.ui.PageObjectSuite;

import com.github.tlrx.elasticsearch.test.EsSetup;

import static com.github.tlrx.elasticsearch.test.EsSetup.deleteAll;

public class ElasticSearchRunner extends PageObjectSuite
{
    public static EsSetup esSetup;

    public ElasticSearchRunner(Class< ? > suiteClass, RunnerBuilder builder) throws InitializationError
    {
        super(suiteClass, builder);
    }

    @Override
    protected void beforeTests()
    {
        // Start ES *before* XWiki is started (this is important starting XWiki means checking that XWiki is up by
        // requesting one page and that page will trigger the send of the ping to the ES instance!).
        esSetup = new EsSetup();
        esSetup.execute(deleteAll());

        super.beforeTests();
    }

    @Override
    protected void afterTests()
    {
        super.afterTests();

        // Stop ES
        esSetup.terminate();
    }
}
