package org.apache.chemistry.opencmis.jcr;


import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class JcrDocumentTest extends AbstractJcrSessionTest {

    private void checkPropertyForNotNull(String property) {
        JcrRepository repository = getJcrRepository();
        ObjectData root = getRootFolder();

        PropertiesImpl properties = new PropertiesImpl();
        properties.addProperty(new PropertyIdImpl(PropertyIds.OBJECT_TYPE_ID, "cmis:document"));
        properties.addProperty(new PropertyStringImpl(PropertyIds.NAME, "TestDocument"));

        String documentId = repository.createDocument(getSession(), properties, root.getId(), null, null);
        ObjectData testDocument = repository.getObject(getSession(), documentId, null, null, null, false);

        assertNotNull(testDocument.getProperties().getProperties().get(property).getValues());
    }

    @Test
    public void propertyVSCOByShouldNotBeNull() {
        String propertyName = PropertyIds.VERSION_SERIES_CHECKED_OUT_BY;
        checkPropertyForNotNull(propertyName);
    }

    @Test
    public void propertyVSCOIdShouldNotBeNull() {
        String propertyName = PropertyIds.VERSION_SERIES_CHECKED_OUT_ID;
        checkPropertyForNotNull(propertyName);
    }

}
