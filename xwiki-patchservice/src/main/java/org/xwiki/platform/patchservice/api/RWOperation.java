package org.xwiki.platform.patchservice.api;

import java.io.InputStream;
import java.util.Map;

/**
 * A read-write operation, or one that can be defined using method calls. The
 * 
 * @see Operation
 * @version $Id$
 */
public interface RWOperation extends Operation
{
    /* Operations affeting the content of the document */
    /**
     * Record a text insert operation in the document content.
     * 
     * @param text The text being inserted.
     * @param position The position where the text is inserted.
     * @return True if the action was successfully stored in the object, false otherwise.
     */
    boolean insert(String text, Position position);

    boolean delete(String text, Position position);

    /* Operations affeting the document metadata (name, author, language etc. */
    boolean setProperty(String property, String value);

    /* Operations affeting the XObjectDefinition stored in a document */
    boolean createType(String className, String typeName, String typeType, Map<String, ?> typeConfig);

    boolean modifyType(String className, String typeName, Map<String, ?> typeConfig);

    boolean deleteType(String className, String typeName);

    /* Operations affeting the document's objects */
    boolean addObject(String objectClass);

    boolean deleteObject(String objectClass, int index);

    boolean setObjectProperty(String objectClass, int index, String propertyName, String value);

    boolean insertInProperty(String objectClass, int index, String property, String text,
        Position position);

    boolean deleteFromProperty(String objectClass, int index, String property, String text,
        Position position);

    /* Operations affeting the attachments */
    boolean addAttachment(InputStream is, String filename, String author);
    
    boolean setAttachment(InputStream is, String filename, String author);

    boolean deleteAttachment(String name);

    /* Operation metadata */
    void setType(String type);
}
