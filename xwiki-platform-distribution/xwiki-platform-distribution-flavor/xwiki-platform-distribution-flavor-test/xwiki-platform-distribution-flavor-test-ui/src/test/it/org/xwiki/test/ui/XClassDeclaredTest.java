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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.Assert.assertEquals;

/**
 * Check that all mandatory document initializers are properly declared in the demo package build and that the pages
 * are not created at first start only. See also XWIKI-22368.
 *
 * @version $Id$
 * @since 16.7.0RC1
 */
public class XClassDeclaredTest extends AbstractTest
{
    private static final String GROOVY_CODE =
    """
        {{groovy}}
        import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
        import java.time.temporal.ChronoUnit;
        import java.text.SimpleDateFormat;
        
        // define the pages that shouldn't be created during the build.
        def allowedExceptions = ["XWiki.XWikiServerXwiki"];
        
        // Get creation date at build time.
        def adminRef = services.model.resolveDocument("XWiki.Admin");
        def adminDoc = xwiki.getDocument(adminRef);
        def adminCreationDate = adminDoc.getCreationDate();
        def adminCreationDateDay = adminDoc.getCreationDate().toInstant().truncatedTo(ChronoUnit.DAYS);
        
        def allowedExceptionsRef = [];
        for (exception in allowedExceptions) {
          allowedExceptionsRef.add(services.model.resolveDocument(exception));
        }
        
        def componentManager = services.component.getContextComponentManager();
        def documentInitializersList = componentManager.getInstanceList(MandatoryDocumentInitializer.class);
        
        def dateFormat = new SimpleDateFormat("dd/MM/YYYY");
        def foundErrors = [];
        for (initializer in documentInitializersList) {
          def ref = initializer.getDocumentReference();
          if (!allowedExceptionsRef.contains(ref)) {
            def classDoc = xwiki.getDocument(ref);
            def creationDate = classDoc.getCreationDate();
            def creationDateDay = classDoc.getCreationDate().toInstant().truncatedTo(ChronoUnit.DAYS);
            if (!creationDateDay.equals(adminCreationDateDay)) {
              foundErrors.add(ref.toString() + " ("+dateFormat.format(creationDateDay.toDate())+")");
            }
          }
        }
        
        if (foundErrors.size() > 0) {
            def adminDateFormat = dateFormat.format(adminCreationDateDay.toDate());
            println foundErrors.size() + " page found created after the build ("+adminDateFormat+"):\\n";
            for (error in foundErrors) {
              println "     * " + error;
            }
        }
        {{/groovy}}        
    """;

    @Test
    public void testXClassAreDeclaredInPackage()
    {
        getUtil().loginAsAdmin();
        ViewPage page = getUtil().createPage(new DocumentReference("xwiki", "Test", "TestXClass"), GROOVY_CODE);
        assertEquals("", page.getContent());
    }
}
