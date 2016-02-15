package org.apache.chemistry.opencmis.jcr;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyHtmlDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyHtmlImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyUriDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyUriImpl;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.UUID;

/**
 * @author Vyacheslav Polulyakh
 * @since 2/11/2016.
 */
public class PropertyHelperTest {

    @Test
    public void shouldCheckBooleanPropertyTest() {

        PropertyBooleanDefinitionImpl propertyBooleanDefinition = new PropertyBooleanDefinitionImpl();
        propertyBooleanDefinition.setIsRequired(Boolean.TRUE);
        propertyBooleanDefinition.setPropertyType(PropertyType.BOOLEAN);

        assertFalse(
                PropertyHelper.isPropertyEmpty(
                        new PropertyBooleanImpl("requiredBooleanProperty", Boolean.TRUE),
                        propertyBooleanDefinition));
        assertTrue(
                PropertyHelper.isPropertyEmpty(
                        new PropertyBooleanImpl("requiredBooleanProperty", (Boolean) null),
                        propertyBooleanDefinition));
        assertFalse(
                PropertyHelper.isPropertyEmpty(
                        new PropertyBooleanImpl("requiredMultipleBooleanProperty", Arrays.asList(Boolean.FALSE, Boolean.TRUE)),
                        propertyBooleanDefinition));
        assertTrue(
                PropertyHelper.isPropertyEmpty(
                        new PropertyBooleanImpl("requiredMultipleBooleanProperty", Arrays.asList(Boolean.FALSE, null)),
                        propertyBooleanDefinition));
    }

    @Test
    public void shouldCheckDatePropertyTest() {

        PropertyDateTimeDefinitionImpl propertyDateTimeDefinition = new PropertyDateTimeDefinitionImpl();
        propertyDateTimeDefinition.setIsRequired(Boolean.TRUE);
        propertyDateTimeDefinition.setPropertyType(PropertyType.DATETIME);

        assertFalse(
                PropertyHelper.isPropertyEmpty(
                        new PropertyDateTimeImpl("requiredDateProperty", new GregorianCalendar()),
                        propertyDateTimeDefinition));
        assertTrue(
                PropertyHelper.isPropertyEmpty(
                        new PropertyDateTimeImpl("requiredDateProperty", (GregorianCalendar) null),
                        propertyDateTimeDefinition));
        assertFalse(
                PropertyHelper.isPropertyEmpty(
                        new PropertyDateTimeImpl("requiredMultipleDateProperty", Arrays.asList(new GregorianCalendar(), new GregorianCalendar())),
                        propertyDateTimeDefinition));
        assertTrue(
                PropertyHelper.isPropertyEmpty(
                        new PropertyDateTimeImpl("requiredMultipleDateProperty", Arrays.asList(new GregorianCalendar(), null)),
                        propertyDateTimeDefinition));
    }

    @Test
    public void shouldCheckDecimalPropertyTest() {

        PropertyDecimalDefinitionImpl propertyDecimalDefinition = new PropertyDecimalDefinitionImpl();
        propertyDecimalDefinition.setIsRequired(Boolean.TRUE);
        propertyDecimalDefinition.setPropertyType(PropertyType.DECIMAL);

        assertFalse(
                PropertyHelper.isPropertyEmpty(
                        new PropertyDecimalImpl("requiredDecimalProperty", BigDecimal.ZERO),
                        propertyDecimalDefinition));
        assertTrue(
                PropertyHelper.isPropertyEmpty(
                        new PropertyDecimalImpl("requiredDecimalProperty", (BigDecimal) null),
                        propertyDecimalDefinition));
        assertFalse(
                PropertyHelper.isPropertyEmpty(
                        new PropertyDecimalImpl("requiredMultipleDecimalProperty", Arrays.asList(BigDecimal.ONE, BigDecimal.TEN)),
                        propertyDecimalDefinition));
        assertTrue(
                PropertyHelper.isPropertyEmpty(
                        new PropertyDecimalImpl("requiredMultipleDecimalProperty", Arrays.asList(BigDecimal.ZERO, null)),
                        propertyDecimalDefinition));
    }

    @Test
    public void shouldCheckIntegerPropertyTest() {

        PropertyIntegerDefinitionImpl propertyIntegerDefinition = new PropertyIntegerDefinitionImpl();
        propertyIntegerDefinition.setIsRequired(Boolean.TRUE);
        propertyIntegerDefinition.setPropertyType(PropertyType.INTEGER);

        assertFalse(
                PropertyHelper.isPropertyEmpty(
                        new PropertyIntegerImpl("requiredIntegerProperty", BigInteger.ZERO),
                        propertyIntegerDefinition));
        assertTrue(
                PropertyHelper.isPropertyEmpty(
                        new PropertyIntegerImpl("requiredIntegerProperty", (BigInteger) null),
                        propertyIntegerDefinition));
        assertFalse(
                PropertyHelper.isPropertyEmpty(
                        new PropertyIntegerImpl("requiredMultipleIntegerProperty", Arrays.asList(BigInteger.ONE, BigInteger.TEN)),
                        propertyIntegerDefinition));
        assertTrue(
                PropertyHelper.isPropertyEmpty(
                        new PropertyIntegerImpl("requiredMultipleIntegerProperty", Arrays.asList(BigInteger.ZERO, null)),
                        propertyIntegerDefinition));
    }

    @Test
    public void shouldCheckHtmlPropertyTest() {

        PropertyHtmlDefinitionImpl propertyHtmlDefinition = new PropertyHtmlDefinitionImpl();
        propertyHtmlDefinition.setIsRequired(Boolean.TRUE);
        propertyHtmlDefinition.setPropertyType(PropertyType.HTML);

        assertFalse(
                PropertyHelper.isPropertyEmpty(
                        new PropertyHtmlImpl("requiredHtmlProperty", "<html></html>"),
                        propertyHtmlDefinition));
        assertTrue(
                PropertyHelper.isPropertyEmpty(
                        new PropertyHtmlImpl("requiredHtmlProperty", (String) null),
                        propertyHtmlDefinition));
        assertFalse(
                PropertyHelper.isPropertyEmpty(
                        new PropertyHtmlImpl("requiredMultipleHtmlProperty", Arrays.asList("<a></a>", "<b></b>")),
                        propertyHtmlDefinition));
        assertTrue(
                PropertyHelper.isPropertyEmpty(
                        new PropertyHtmlImpl("requiredMultipleHtmlProperty", Arrays.asList("<i></i>", null)),
                        propertyHtmlDefinition));
    }

    @Test
    public void shouldCheckIdPropertyTest() {

        PropertyIdDefinitionImpl propertyIdDefinition = new PropertyIdDefinitionImpl();
        propertyIdDefinition.setIsRequired(Boolean.TRUE);
        propertyIdDefinition.setPropertyType(PropertyType.ID);

        assertFalse(
                PropertyHelper.isPropertyEmpty(
                        new PropertyIdImpl("requiredIdProperty", UUID.randomUUID().toString()),
                        propertyIdDefinition));
        assertTrue(
                PropertyHelper.isPropertyEmpty(
                        new PropertyIdImpl("requiredIdProperty", (String) null),
                        propertyIdDefinition));
        assertFalse(
                PropertyHelper.isPropertyEmpty(
                        new PropertyIdImpl("requiredMultipleIdProperty", Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString())),
                        propertyIdDefinition));
        assertTrue(
                PropertyHelper.isPropertyEmpty(
                        new PropertyIdImpl("requiredMultipleIdProperty", Arrays.asList(UUID.randomUUID().toString(), null)),
                        propertyIdDefinition));
    }

    @Test
    public void shouldCheckStringPropertyTest() {

        PropertyStringDefinitionImpl propertyStringDefinition = new PropertyStringDefinitionImpl();
        propertyStringDefinition.setIsRequired(Boolean.TRUE);
        propertyStringDefinition.setPropertyType(PropertyType.STRING);

        assertFalse(
                PropertyHelper.isPropertyEmpty(
                        new PropertyStringImpl("requiredStringProperty", "text"),
                        propertyStringDefinition));
        assertTrue(
                PropertyHelper.isPropertyEmpty(
                        new PropertyStringImpl("requiredStringProperty", (String) null),
                        propertyStringDefinition));
        assertFalse(
                PropertyHelper.isPropertyEmpty(
                        new PropertyStringImpl("requiredMultipleStringProperty", Arrays.asList("text1", "text2")),
                        propertyStringDefinition));
        assertTrue(
                PropertyHelper.isPropertyEmpty(
                        new PropertyStringImpl("requiredMultipleStringProperty", Arrays.asList("text3", null)),
                        propertyStringDefinition));
    }

    @Test
    public void shouldCheckUriPropertyTest() {

        PropertyUriDefinitionImpl propertyUriDefinition = new PropertyUriDefinitionImpl();
        propertyUriDefinition.setIsRequired(Boolean.TRUE);
        propertyUriDefinition.setPropertyType(PropertyType.URI);

        assertFalse(
                PropertyHelper.isPropertyEmpty(
                        new PropertyUriImpl("requiredUriProperty", "http://java.sun.com/j2se/1.3/"),
                        propertyUriDefinition));
        assertTrue(
                PropertyHelper.isPropertyEmpty(
                        new PropertyUriImpl("requiredUriProperty", (String) null),
                        propertyUriDefinition));
        assertFalse(
                PropertyHelper.isPropertyEmpty(
                        new PropertyUriImpl("requiredMultipleUriProperty", Arrays.asList("docs/guide/collections/designfaq.html#28", "../../../demo/jfc/SwingSet2/src/SwingSet2.java")),
                        propertyUriDefinition));
        assertTrue(
                PropertyHelper.isPropertyEmpty(
                        new PropertyUriImpl("requiredMultipleUriProperty", Arrays.asList("file:///~/calendar", null)),
                        propertyUriDefinition));
    }
}
