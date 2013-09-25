package org.apache.chemistry.opencmis.jcr;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.junit.Test;

import static org.junit.Assert.fail;

public class JcrTypeManagerTest extends AbstractJcrSessionTest {

    private final String document = "cmis:document";

    private void attributeQueryableShouldBeTrue(String propertyName, String typeId) {
        TypeDefinition typeDefinitionDoc = getJcrRepository().getTypeDefinition(getSession(), typeId);

        if (!typeDefinitionDoc.getPropertyDefinitions().get(propertyName).isQueryable()) {
            fail("Attribute Queryable in property " + propertyName + " should be TRUE");
        }
    }

    private void attributeOrderableShouldBeTrue(String propertyName, String typeId) {
        TypeDefinition typeDefinitionDoc = getJcrRepository().getTypeDefinition(getSession(), typeId);

        if (!typeDefinitionDoc.getPropertyDefinitions().get(propertyName).isOrderable()) {
            fail("Attribute Orderable in property " + propertyName + " should be TRUE");
        }
    }

    @Test
    public void testPropertyCreatedBy() {
        String property = PropertyIds.CREATED_BY;
        attributeQueryableShouldBeTrue(property, document);
        attributeOrderableShouldBeTrue(property, document);
    }

    @Test
    public void testPropertyCreationDate() {
        String property = PropertyIds.CREATION_DATE;
        attributeQueryableShouldBeTrue(property, document);
        attributeOrderableShouldBeTrue(property, document);
    }

    @Test
    public void testPropertyLastModificationDate() {
        String property = PropertyIds.LAST_MODIFICATION_DATE;
        attributeQueryableShouldBeTrue(property, document);
        attributeOrderableShouldBeTrue(property, document);
    }

    @Test
    public void testPropertyLastModifiedBy() {
        String property = PropertyIds.LAST_MODIFIED_BY;
        attributeQueryableShouldBeTrue(property, document);
        attributeOrderableShouldBeTrue(property, document);
    }

}