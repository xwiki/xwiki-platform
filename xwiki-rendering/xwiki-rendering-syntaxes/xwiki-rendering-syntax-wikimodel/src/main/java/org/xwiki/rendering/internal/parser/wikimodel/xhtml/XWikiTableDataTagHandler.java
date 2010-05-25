/*******************************************************************************
 * Copyright (c) 2005,2006 Cognium Systems SA and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cognium Systems SA - initial API and implementation
 *******************************************************************************/
package org.xwiki.rendering.internal.parser.wikimodel.xhtml;

import org.wikimodel.wem.WikiParameter;
import org.wikimodel.wem.WikiParameters;
import org.wikimodel.wem.xhtml.handler.TableDataTagHandler;
import org.wikimodel.wem.xhtml.impl.XhtmlHandler.TagStack.TagContext;

/**
 * Override the default implementation of hte WikiModel XHTML parser for handling HTML table cells. We need to do this
 * in order to handle clean auto generated scope attributes.
 * 
 * @version $Id$
 * @since 2.4M1
 */
public class XWikiTableDataTagHandler extends TableDataTagHandler
{
    /**
     * Name of the th scope attribute.
     */
    private static final String TH_SCOPE = "scope";

    /**
     * Column value of the th scope attribute.
     */
    private static final String TH_SCOPE_COL = "col";

    /**
     * Row value of the th scope attribute.
     */
    private static final String TH_SCOPE_ROW = "row";

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.xhtml.handler.TableDataTagHandler#begin(org.wikimodel.wem.xhtml.impl.XhtmlHandler.TagStack.TagContext)
     */
    @Override
    protected void begin(TagContext context)
    {
        WikiParameters parameters = context.getParams();

        // clean useless scope attributes
        WikiParameter scopeParameter = parameters.getParameter(TH_SCOPE);

        if (scopeParameter != null) {
            if (context.getScannerContext().getTableRowCounter() == 0) {
                if (scopeParameter.getValue().equals(TH_SCOPE_COL)) {
                    parameters = parameters.remove(TH_SCOPE);
                }
            } else if (context.getScannerContext().getTableCellCounter() == 0) {
                if (scopeParameter.getValue().equals(TH_SCOPE_ROW)) {
                    parameters = parameters.remove(TH_SCOPE);
                }
            } else {
                if (scopeParameter.getValue().equals(TH_SCOPE_COL)) {
                    parameters = parameters.remove(TH_SCOPE);
                }
            }
        }

        context.getScannerContext().beginTableCell(true, parameters);
    }

}
