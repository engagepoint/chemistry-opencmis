/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.chemistry.opencmis.commons.impl.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.impl.XMLConstants;
import org.apache.chemistry.opencmis.commons.impl.XMLUtils;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public abstract class AbstractXMLConverterTest {

    protected final static String TEST_NAMESPACE = "http://chemistry.apache.org/test/schema";
    protected final static String TEST_PREFIX = "test";

    private final static Logger LOG = LoggerFactory.getLogger(AbstractXMLConverterTest.class);

    private final static long SEED = 1234567890;

    protected final static String TEST_SCHEMA = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\" targetNamespace=\""
            + TEST_NAMESPACE
            + "\" xmlns:test=\""
            + TEST_NAMESPACE
            + "\" xmlns:cmis=\""
            + XMLConstants.NAMESPACE_CMIS
            + "\" version=\"1.0\">"
            + "<xs:import namespace=\""
            + XMLConstants.NAMESPACE_CMIS
            + "\"/>"
            + "<xs:complexType name=\"testType\">"
            + "<xs:sequence>"
            + "<xs:element name=\"repositoryInfo\" type=\"cmis:cmisRepositoryInfoType\" minOccurs=\"0\" maxOccurs=\"1\" />"
            + "<xs:element name=\"type\" type=\"cmis:cmisTypeDefinitionType\" minOccurs=\"0\" maxOccurs=\"1\" />"
            + "<xs:element name=\"object\" type=\"cmis:cmisObjectType\" minOccurs=\"0\" maxOccurs=\"1\" />"
            + "</xs:sequence>" //
            + "</xs:complexType>" //
            + "<xs:element name=\"test\" type=\"test:testType\"/>" //
            + "</xs:schema>";

    protected Schema schema10;
    protected Schema schema11;
    protected Random rnd;

    /**
     * Sets up the schema.
     */
    @Before
    public void init() throws SAXException, IOException {
        SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

        InputStream schema10stream = AbstractXMLConverterTest.class.getResourceAsStream("/schema/cmis10/CMIS-core.xsd");
        if (schema10stream != null) {
            StreamSource core10 = new StreamSource(schema10stream);
            StreamSource test10 = new StreamSource(new ByteArrayInputStream(TEST_SCHEMA.getBytes("UTF-8")));
            schema10 = sf.newSchema(new Source[] { core10, test10 });
        }

        InputStream schema11stream = AbstractXMLConverterTest.class.getResourceAsStream("/schema/cmis11/CMIS-core.xsd");
        if (schema11stream != null) {
            StreamSource core11 = new StreamSource(schema11stream);
            StreamSource test11 = new StreamSource(new ByteArrayInputStream(TEST_SCHEMA.getBytes("UTF-8")));
            schema11 = sf.newSchema(new Source[] { core11, test11 });
        }

        rnd = new Random(SEED);
    }

    /**
     * Writes root tag of the test XML.
     */
    protected void writeRootTag(XMLStreamWriter writer) throws XMLStreamException {
        writer.setPrefix(TEST_PREFIX, TEST_NAMESPACE);
        writer.writeStartElement(TEST_NAMESPACE, TEST_PREFIX);
        writer.writeNamespace(XMLUtils.PREFIX_XSI, XMLConstants.NAMESPACE_XSI);
        writer.writeNamespace(XMLUtils.PREFIX_CMIS, XMLConstants.NAMESPACE_CMIS);
        writer.writeNamespace(TEST_PREFIX, TEST_NAMESPACE);
    }

    /**
     * Creates a test XML writer.
     */
    protected XMLStreamWriter createWriter(OutputStream out) throws XMLStreamException {
        XMLStreamWriter writer = XMLUtils.createWriter(out);

        XMLUtils.startXmlDocument(writer);
        writeRootTag(writer);

        return writer;
    }

    /**
     * Closes the test XML writer.
     */
    protected void closeWriter(XMLStreamWriter writer) throws XMLStreamException {
        XMLUtils.endXmlDocument(writer);
        writer.close();
    }

    /**
     * Creates a parser and moves it to the tag that should be tested.
     */
    protected XMLStreamReader createParser(byte[] xmlDocument) throws XMLStreamException {
        XMLStreamReader parser = XMLUtils.createParser(new ByteArrayInputStream(xmlDocument));
        moveToTestTag(parser);

        return parser;
    }

    /**
     * Closes the parser.
     */
    protected void closeParser(XMLStreamReader parser) throws XMLStreamException {
        parser.close();
    }

    /**
     * Moves the parser to tag that should be tested.
     */
    protected void moveToTestTag(XMLStreamReader parser) throws XMLStreamException {
        while (XMLUtils.findNextStartElemenet(parser)) {
            if (parser.getName().getLocalPart().equals("test")) {
                XMLUtils.next(parser);
                XMLUtils.findNextStartElemenet(parser);
                break;
            }
        }
    }

    /**
     * Validates the given XML.
     */
    protected void validate(byte[] xmlDocument, CmisVersion cmisVersion) {
        Validator validator = null;
        if (cmisVersion == CmisVersion.CMIS_1_0) {
            if (schema10 != null) {
                validator = schema10.newValidator();
            } else {
                LOG.warn("CMIS 1.0 schema not loaded. Cannot validate XML.");
                return;
            }
        } else {
            if (schema11 != null) {
                validator = schema11.newValidator();
            } else {
                LOG.warn("CMIS 1.1 schema not loaded. Cannot validate XML.");
                return;
            }
        }

        Source source = new StreamSource(new ByteArrayInputStream(xmlDocument));

        try {
            validator.validate(source);
        } catch (Exception e) {
            try {
                LOG.error("Schema validation failed:\n" + format(xmlDocument));
                System.out.println(format(xmlDocument));
            } catch (TransformerException e1) {
            }
            fail("Schema " + cmisVersion.value() + " validation failed: " + e);
        }
    }

    /**
     * Formats an XML document.
     */
    protected String format(byte[] xmlDocument) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setAttribute("indent-number", new Integer(2));

        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        StreamResult result = new StreamResult(new StringWriter());
        Source source = new StreamSource(new ByteArrayInputStream(xmlDocument));
        transformer.transform(source, result);

        return result.getWriter().toString();
    }

    protected String randomString() {
        StringBuilder sb = new StringBuilder();

        int length = rnd.nextInt(21) + 3;
        for (int i = 0; i < length; i++) {
            sb.append((char) (rnd.nextInt(94) + 32));
        }

        return sb.toString();
    }

    protected String randomUri() {
        StringBuilder sb = new StringBuilder("urn:test:");

        int length = rnd.nextInt(21) + 3;
        for (int i = 0; i < length; i++) {
            sb.append((char) (rnd.nextInt(26) + 97));
        }

        return sb.toString();
    }

    protected Boolean randomBoolean() {
        return Boolean.valueOf(rnd.nextBoolean());
    }

    protected BigInteger randomInteger() {
        return BigInteger.valueOf(rnd.nextInt());
    }

    protected BigDecimal randomDecimal() {
        return BigDecimal.valueOf(rnd.nextDouble() * rnd.nextInt());
    }

    protected GregorianCalendar randomDateTime() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeZone(TimeZone.getTimeZone("GMT" + (rnd.nextBoolean() ? "+" : "-") + (rnd.nextInt(23) - 12) + ":00"));
        cal.set(rnd.nextInt(9998) + 1, rnd.nextInt(12), rnd.nextInt(31) + 1, rnd.nextInt(23), rnd.nextInt(60),
                rnd.nextInt(60));
        cal.set(Calendar.MILLISECOND, 0);

        return cal;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Enum<?>> T randomEnum(Class<T> enumClass) {
        T[] values = null;
        try {
            values = (T[]) enumClass.getMethod("values", new Class<?>[0]).invoke(null, new Object[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return values[rnd.nextInt(values.length)];
    }

    /**
     * Compares two data objects.
     */
    protected void assertDataObjectsEquals(String name, Object expected, Object actual, Set<String> ignoreMethods) {

        LOG.debug(name + ": " + expected + " / " + actual);

        if ((expected == null) && (actual == null)) {
            return;
        }

        if ((expected == null) || (actual == null)) {
            fail("Data object is null! name: " + name + " / expected: " + expected + " / actual: " + actual);
        }

        // handle simple types
        if ((expected instanceof String) || (expected instanceof Boolean) || (expected instanceof BigInteger)
                || (expected instanceof BigDecimal) || (expected instanceof Enum<?>)) {
            assertEquals(expected, actual);

            return;
        } else if (expected instanceof GregorianCalendar) {
            assertEquals(((GregorianCalendar) expected).getTimeInMillis(),
                    ((GregorianCalendar) actual).getTimeInMillis());

            return;
        } else if (expected instanceof List<?>) {
            List<?> expectedList = (List<?>) expected;
            List<?> actualList = (List<?>) actual;

            assertEquals(expectedList.size(), actualList.size());

            for (int i = 0; i < expectedList.size(); i++) {
                assertDataObjectsEquals(name + "[" + i + "]", expectedList.get(i), actualList.get(i), ignoreMethods);
            }

            return;
        } else if (expected instanceof Map<?, ?>) {
            Map<?, ?> expectedMap = (Map<?, ?>) expected;
            Map<?, ?> actualMap = (Map<?, ?>) actual;

            assertEquals(expectedMap.size(), actualMap.size());

            for (Map.Entry<?, ?> entry : expectedMap.entrySet()) {
                assertTrue(actualMap.containsKey(entry.getKey()));
                assertDataObjectsEquals(name + "[" + entry.getKey() + "]", entry.getValue(),
                        actualMap.get(entry.getKey()), ignoreMethods);
            }

            return;
        }

        for (Method m : expected.getClass().getMethods()) {
            if (!m.getName().startsWith("get") && !m.getName().startsWith("is") && !m.getName().startsWith("supports")) {
                continue;
            }

            if (ignoreMethods != null && ignoreMethods.contains(m.getName())) {
                continue;
            }

            if (m.getName().equals("getClass")) {
                continue;
            }

            if (m.getParameterTypes().length != 0) {
                continue;
            }

            try {
                Object expectedValue = m.invoke(expected, new Object[0]);
                Object actualValue = m.invoke(actual, new Object[0]);

                assertDataObjectsEquals(name + "." + m.getName(), expectedValue, actualValue, ignoreMethods);
            } catch (Exception e) {
                fail(e.toString());
            }
        }
    }
}
