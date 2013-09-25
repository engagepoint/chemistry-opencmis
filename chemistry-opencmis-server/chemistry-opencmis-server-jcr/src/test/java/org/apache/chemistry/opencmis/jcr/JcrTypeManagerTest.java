package org.apache.chemistry.opencmis.jcr;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.junit.Test;

import static org.junit.Assert.fail;

public class JcrTypeManagerTest extends AbstractJcrSessionTest {

    private final String document = "cmis:document";

    private void attributeRequiredShouldBeFalse(String propertyName, String typeId) {
        TypeDefinition typeDefinitionDoc = getJcrRepository().getTypeDefinition(getSession(), typeId);

        if (typeDefinitionDoc.getPropertyDefinitions().get(propertyName).isRequired()) {
            fail("Attribute Required in property " + propertyName + " should be FALSE");
        }
    }

    @Test
    public void testPropertyBaseTypeId() {
        String property = PropertyIds.BASE_TYPE_ID;
        attributeRequiredShouldBeFalse(property, document);
    }

    @Test
    public void testPropertyCreatedBy() {
        String property = PropertyIds.CREATED_BY;
        attributeRequiredShouldBeFalse(property, document);
    }

    @Test
    public void testPropertyCreationDate() {
        String property = PropertyIds.CREATION_DATE;
        attributeRequiredShouldBeFalse(property, document);
    }

    @Test
    public void testPropertyIsVersionCheckedOut() {
        String property = PropertyIds.IS_VERSION_SERIES_CHECKED_OUT;
        attributeRequiredShouldBeFalse(property, document);
    }

    @Test
    public void testAttributeRequiredInPropertyLastModificationDate() {
        String property = PropertyIds.LAST_MODIFICATION_DATE;
        attributeRequiredShouldBeFalse(property, document);
    }

    @Test
    public void testPropertyLastModifiedBy() {
        String property = PropertyIds.LAST_MODIFIED_BY;
        attributeRequiredShouldBeFalse(property, document);
    }

    @Test
    public void testPropertyObjectId() {
        String property = PropertyIds.OBJECT_ID;
        attributeRequiredShouldBeFalse(property, document);
    }

    @Test
    public void testPropertyVersionSeriesId() {
        String property = PropertyIds.VERSION_SERIES_ID;
        attributeRequiredShouldBeFalse(property, document);
    }
}