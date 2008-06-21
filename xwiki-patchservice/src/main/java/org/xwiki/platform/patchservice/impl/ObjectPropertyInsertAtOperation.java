/*
 * See the NOTICE file distributed with thisimport java.util.Formatter;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.Position;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package org.xwiki.platform.patchservice.impl;

import java.util.Formatter;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.Position;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public class ObjectPropertyInsertAtOperation extends AbstractOperationImpl implements RWOperation
{
    private String className;

    private int number = -1;

    private String propertyName;

    private Position position;

    private String addedContent;

    static {
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_OBJECT_PROPERTY_INSERT_AT,
            ObjectPropertyInsertAtOperation.class);
    }

    public ObjectPropertyInsertAtOperation()
    {
        this.setType(Operation.TYPE_OBJECT_PROPERTY_INSERT_AT);
    }

    /**
     * {@inheritDoc}
     */
    public void apply(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        BaseObject obj = doc.getObject(this.className, this.number);
        if (obj == null) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                new Formatter().format("Invalid object type/number: %s[%d]",
                    new Object[] {this.className, new Integer(this.number)}).toString());
        }
        try {
            String value = obj.getLargeStringValue(this.propertyName);
            if (!this.position.checkPosition(value)) {
                throw new Exception();
            }
            value =
                this.position.getTextBeforePosition(value) + this.addedContent
                    + this.position.getTextAfterPosition(value);
            obj.set(this.propertyName, value, context);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                new Formatter().format("Invalid inserted text: [%s] for object property [%s]",
                    new Object[] {this.addedContent, this.propertyName}).toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean insertInProperty(String objectClass, int index, String propertyName, String value, Position position)
    {
        this.className = objectClass;
        this.number = index;
        this.propertyName = propertyName;
        this.addedContent = value;
        this.position = position;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        this.className = getObjectClassname(e);
        this.number = getObjectNumber(e);
        this.propertyName = getPropertyName(getObjectNode(e));
        this.addedContent = getTextValue(e);
        this.position = loadPositionNode(e);
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = createOperationNode(doc);
        Element objectNode = createObjectNode(this.className, this.number, doc);
        objectNode.appendChild(createPropertyNode(this.propertyName, doc));
        xmlNode.appendChild(objectNode);
        xmlNode.appendChild(createTextNode(this.addedContent, doc));
        xmlNode.appendChild(this.position.toXml(doc));
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other)
    {
        try {
            ObjectPropertyInsertAtOperation that = (ObjectPropertyInsertAtOperation) other;
            return that.getType().equals(this.getType()) && that.className.equals(this.className)
                && (that.number == this.number) && that.propertyName.equals(this.propertyName)
                && that.addedContent.equals(this.addedContent) && that.position.equals(this.position);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(29, 31).append(this.className).append(this.number).append(this.propertyName).append(
            this.addedContent).append(this.position).toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getType() + ": [" + this.className + "[" + this.number + "]#" + this.propertyName + "]@" + this.position
            + " = [" + this.addedContent + "]";
    }
}
