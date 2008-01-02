package org.xwiki.platform.patchservice.api;

import java.io.InputStream;
import java.util.Date;
import java.util.Map;

/**
 * A read-write operation, or one that can be defined using method calls. The 
 * @see Operation
 */
public interface RWOperation extends Operation
{
    /* Operations affeting the content of the document */
    /**
     * Record a text insert operation in the document content.
     * @param text The text being inserted.
     * @param position The position where the text is inserted.
     * @return True if the action was successfully stored in the object, false otherwise.
     */
    boolean insert(String text, Position position);

    boolean delete(String text, Position position);

    /* Operations affeting the document metadata (name, author, language etc. */
    boolean setProperty(String property, String value);

    /* Operations affeting the XObjectDefinition stored in a document */
    boolean createType(String propertyType, Map properties);

    boolean modifyType(String propertyName, Map properties);

    boolean deleteType(String propertyName);

    /* Operations affeting the document's objects */
    boolean addObject(String objectClass);

    boolean deleteObject(String objectClass, int index);

    boolean setObjectProperty(String objectClass, String index, String propertyName, String value);

    boolean insertInProperty(String property, String text, Position position);

    boolean deleteFromProperty(String property, String text, Position position);

    /* Operations affeting the attachments */
    boolean setAttachment(InputStream is);

    boolean deleteAttachment(String name);

    /* Operation metadata */
    void setTime(Date time);

    void setType(String type);
}
