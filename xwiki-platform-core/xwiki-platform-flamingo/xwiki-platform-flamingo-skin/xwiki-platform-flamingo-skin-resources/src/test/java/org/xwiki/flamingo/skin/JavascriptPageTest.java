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
package org.xwiki.flamingo.skin;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.PageTest;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.debug.DebugConfiguration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the {@code javascript.vm} template of the Flamingo skin.
 *
 * @version $Id$
 */
class JavascriptPageTest extends PageTest
{
    private static final Path TEMPLATE = Path.of("src/main/resources/flamingo/javascript.vm");

    // Required by $xwiki.getSkinFile.
    @MockComponent
    private DebugConfiguration debugConfiguration;

    @Test
    void docSyntax() throws Exception
    {
        String syntaxType = "my\"syntax";
        XWikiDocument document =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Page"), this.context).clone();
        document.setSyntax(new Syntax(new SyntaxType(syntaxType, syntaxType), "1"));
        this.context.setDoc(document);

        TemplateManager templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        StringWriter writer = new StringWriter();
        templateManager.render(templateManager.createStringTemplate("javascript.vm",
            Files.readString(TEMPLATE, StandardCharsets.UTF_8), (DocumentReference) null, (DocumentReference) null),
            writer);

        String result = writer.toString();
        assertTrue(result.contains("XWiki.docsyntax = \"my\\\"syntax\\/1\";"), result);
    }
}
