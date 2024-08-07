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

import org.junit.Test;
import org.xwiki.rendering.syntax.Syntax;

import static org.junit.Assert.assertEquals;

/**
 * Check that all mandatory document initializers are properly declared in the demo package build and that the pages
 * are not created at first start only. See also XWIKI-22368.
 *
 * @version $Id$
 * @since 16.7.0RC1
 */
public class MandatoryDocInitializedTest extends AbstractTest
{
    private static final String GROOVY_CODE =
    """
        {{groovy}}
        import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
        import java.text.SimpleDateFormat;
        import java.lang.management.ManagementFactory;

        // define the pages that should be created at startup
        def startupPages = ["XWiki.XWikiServerXwiki"];
        
        // Pages that are completely ignored from the test
        // SheetClass is initialized in a legacy package: we got it tested from the CI but it's expected that it's
        // initialized late.
        def ignoredPages = ["XWiki.SheetClass"];

        def dateFormat = new SimpleDateFormat("dd/MM/YYYY HH:mm:ss");
        def rb = ManagementFactory.getRuntimeMXBean();
        def startedSince = new Date(rb.getStartTime());
        
        def startupPageRef = [];
        for (startupPage in startupPages) {
          startupPageRef.add(services.model.resolveDocument(startupPage));
        }
        
        def ignoredPagesRef = [];
        for (ignoredPage in ignoredPages) {
          ignoredPagesRef.add(services.model.resolveDocument(ignoredPage));
        }

        def componentManager = services.component.getContextComponentManager();
        def documentInitializersList = componentManager.getInstanceList(MandatoryDocumentInitializer.class);
        
        def foundErrorsCreatedAfter = [];
        def foundErrorsCreatedBefore = [];
        for (initializer in documentInitializersList) {
          def ref = initializer.getDocumentReference();
          // the provided ref might be a local document ref, so we serialize and resolve again to obtain same 
          // canonical ref
          if (!ignoredPagesRef.contains(services.model.resolveDocument(services.model.serialize(ref)))) {
            def classDoc = xwiki.getDocument(ref);
            def creationDate = classDoc.getCreationDate();
            def shouldBeBefore = (!startupPageRef.contains(ref));
            if (shouldBeBefore && creationDate.after(startedSince)) {
              foundErrorsCreatedAfter.add(ref.toString() + " ("+dateFormat.format(creationDate)+")");
            } else if (!shouldBeBefore && creationDate.before(startedSince)) {
              foundErrorsCreatedBefore.add(ref.toString() + " ("+dateFormat.format(creationDate)+")");
            }
          }
        }

        if (foundErrorsCreatedAfter.size() > 0) {
          println "Instance started at " + dateFormat.format(startedSince);
          println foundErrorsCreatedAfter.size() + " page found created after:\\n";
          for (error in foundErrorsCreatedAfter) {
            println "     * " + error;
          }
        }
        if (foundErrorsCreatedBefore.size() > 0) {
          println "Instance started at " + dateFormat.format(startedSince);
          println foundErrorsCreatedBefore.size() + " page found created before while they should be created at startup:\\n";
          for (error in foundErrorsCreatedBefore) {
            println "     * " + error;
          }
        }
        {{/groovy}}
    """;

    @Test
    public void docsAreInitialized() throws Exception
    {
        getUtil().loginAsAdmin();
        String result = getUtil().executeWikiPlain(GROOVY_CODE, Syntax.XWIKI_2_1);

        // Using equals on purpose to see the output in case of failure.
        assertEquals("", result);
    }
}
