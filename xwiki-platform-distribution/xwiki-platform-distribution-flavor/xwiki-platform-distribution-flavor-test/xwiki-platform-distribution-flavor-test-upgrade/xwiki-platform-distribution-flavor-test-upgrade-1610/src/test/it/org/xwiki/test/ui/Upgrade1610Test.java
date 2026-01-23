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
package org.xwiki.test.ui;

import org.xwiki.rendering.syntax.Syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Execute upgrade tests.
 * 
 * @version $Id$
 */
public class Upgrade1610Test extends UpgradeTest
{
    @Override
    protected void postUpdateValidate() throws Exception
    {
        // Make sure the Solr migration went as planned
        assertSolrMigration();
    }

    private void assertSolrMigration() throws Exception
    {
        // We expect the default Sandbox home page which id was generated based on the default locale to be removed
        String result = getUtil().executeWikiPlain("""
{{velocity}}
  #if ($services.component.getInstance('org.xwiki.search.solr.internal.api.SolrInstance').get('xwiki:Sandbox.WebHome_en'))true#{else}false#end

  #if ($services.component.getInstance('org.xwiki.search.solr.internal.api.SolrInstance').get('xwiki:Sandbox.WebHome_'))true#{else}false#end
{{/velocity}}
            """, Syntax.XWIKI_2_1);

        assertEquals("false\ntrue", result.trim());
    }
}
