package com.xpn.xwiki.wysiwyg.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * This {@link ImageBundle} is used for all the button icons. Using an image bundle allows all of these images to be
 * packed into a single image, which saves a lot of HTTP requests, drastically improving startup time.
 */
public interface Images extends ImageBundle
{
    Images INSTANCE = (Images) GWT.create(Images.class);

    /**
     * @gwt.resource attachment.gif
     */
    AbstractImagePrototype attachment();

    /**
     * @gwt.resource backcolor.gif
     */
    AbstractImagePrototype backColor();

    /**
     * @gwt.resource bold.gif
     */
    AbstractImagePrototype bold();

    /**
     * @gwt.resource charmap.gif
     */
    AbstractImagePrototype charmap();

    /**
     * @gwt.resource copy.gif
     */
    AbstractImagePrototype copy();

    /**
     * @gwt.resource cut.gif
     */
    AbstractImagePrototype cut();

    /**
     * @gwt.resource table_delete_col.gif
     */
    AbstractImagePrototype deleteCol();

    /**
     * @gwt.resource table_delete_row.gif
     */
    AbstractImagePrototype deleteRow();

    /**
     * @gwt.resource forecolor.gif
     */
    AbstractImagePrototype foreColor();

    /**
     * @gwt.resource hr.gif
     */
    AbstractImagePrototype hr();

    /**
     * @gwt.resource image.gif
     */
    AbstractImagePrototype image();

    /**
     * @gwt.resource indent.gif
     */
    AbstractImagePrototype indent();

    /**
     * @gwt.resource table_insert_col_after.gif
     */
    AbstractImagePrototype insertColAfter();

    /**
     * @gwt.resource table_insert_col_before.gif
     */
    AbstractImagePrototype insertColBefore();

    /**
     * @gwt.resource table_insert_row_after.gif
     */
    AbstractImagePrototype insertRowAfter();

    /**
     * @gwt.resource table_insert_row_before.gif
     */
    AbstractImagePrototype insertRowBefore();

    /**
     * @gwt.resource table.gif
     */
    AbstractImagePrototype insertTable();

    /**
     * @gwt.resource italic.gif
     */
    AbstractImagePrototype italic();

    /**
     * @gwt.resource justifycenter.gif
     */
    AbstractImagePrototype justifyCenter();

    /**
     * @gwt.resource justifyfull.gif
     */
    AbstractImagePrototype justifyFull();

    /**
     * @gwt.resource justifyleft.gif
     */
    AbstractImagePrototype justifyLeft();

    /**
     * @gwt.resource justifyright.gif
     */
    AbstractImagePrototype justifyRight();

    /**
     * @gwt.resource link.gif
     */
    AbstractImagePrototype link();

    /**
     * @gwt.resource macro.gif
     */
    AbstractImagePrototype macro();

    /**
     * @gwt.resource ol.gif
     */
    AbstractImagePrototype ol();

    /**
     * @gwt.resource outdent.gif
     */
    AbstractImagePrototype outdent();

    /**
     * @gwt.resource palete.png
     */
    AbstractImagePrototype palete();

    /**
     * @gwt.resource paste.gif
     */
    AbstractImagePrototype paste();

    /**
     * @gwt.resource redo.gif
     */
    AbstractImagePrototype redo();

    /**
     * @gwt.resource removeformat.gif
     */
    AbstractImagePrototype removeFormat();

    /**
     * @gwt.resource spacer.gif
     */
    AbstractImagePrototype spacer();

    /**
     * @gwt.resource strikethrough.gif
     */
    AbstractImagePrototype strikeThrough();

    /**
     * @gwt.resource subscript.gif
     */
    AbstractImagePrototype subscript();

    /**
     * @gwt.resource superscript.gif
     */
    AbstractImagePrototype superscript();

    /**
     * @gwt.resource sync.gif
     */
    AbstractImagePrototype sync();

    /**
     * @gwt.resource ul.gif
     */
    AbstractImagePrototype ul();

    /**
     * @gwt.resource underline.gif
     */
    AbstractImagePrototype underline();

    /**
     * @gwt.resource undo.gif
     */
    AbstractImagePrototype undo();

    /**
     * @gwt.resource unlink.gif
     */
    AbstractImagePrototype unlink();
}
