package org.xwiki.rendering.macro.include;

public class IncludeMacroParameters
{
    /**
     * @version $Id$
     */
    public enum Context
    {
        /**
         * Macro executed in its own context.
         */
        NEW,

        /**
         * Macro executed in the context of the current page.
         */
        CURRENT;
    };

    private String document;

    private Context context;

    /**
     * @return the name of the document to include.
     */
    public String getDocument()
    {
        return this.document;
    }

    public void setDocument(String document)
    {
        this.document = document;
    }

    /**
     * @return defines whether the included page is executed in its separated execution context or whether it's executed
     *         in the contex of the current page.
     */
    public Context getContext()
    {
        return this.context;
    }

    public void setContext(Context context)
    {
        this.context = context;
    }
}
