package org.apache.chemistry.opencmis.jcr;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JcrTypeManagerTest extends AbstractJcrSessionTest {

    @Test
    public void addDocumentPropertyDefinitionsTest() {

        JcrRepository repository = getJcrRepository();
        String propertyName = PropertyIds.VERSION_SERIES_CHECKED_OUT_BY;
        TypeDefinition typeDefinition = repository.getTypeDefinition(getSession(), "cmis:document");

        for (PropertyDefinition<?> propertyDefinition : typeDefinition.getPropertyDefinitions().values()) {
            if (propertyDefinition.getId().equals(propertyName)) {
                assertEquals(PropertyType.STRING, propertyDefinition.getPropertyType());
                return;
            }
        }

        fail("The type of property VERSION_SERIES_CHECKED_OUT_BY should be STRING");
    }
}