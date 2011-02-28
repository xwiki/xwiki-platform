package org.xwiki.extension.xar.internal.handler.packager.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

public class DocumentReferenceHandler extends AbstractHandler
{
    private EntityReference pageReference;
    private EntityReference spaceReference;

    public DocumentReferenceHandler(ComponentManager componentManager)
    {
        super(componentManager);

        setCurrentBean(this);

        this.spaceReference = new EntityReference("space", EntityType.SPACE);
        this.pageReference = new EntityReference("page", EntityType.DOCUMENT, this.spaceReference);
    }

    public EntityReference getDocumentReference()
    {
        return this.pageReference;
    }

    @Override
    public void startElementInternal(String uri, String localName, String qName, Attributes attributes)
        throws SAXException
    {
        if (qName.equals("name") || qName.equals("web")) {
            super.startElementInternal(uri, localName, qName, attributes);
        }
    }

    @Override
    public void endElementInternal(String uri, String localName, String qName) throws SAXException
    {
        if (qName.equals("name")) {
            this.pageReference.setName(this.value.toString());
        } else if (qName.equals("web")) {
            this.spaceReference.setName(this.value.toString());
        } else {
            super.endElementInternal(uri, localName, qName);
        }
    }
}
