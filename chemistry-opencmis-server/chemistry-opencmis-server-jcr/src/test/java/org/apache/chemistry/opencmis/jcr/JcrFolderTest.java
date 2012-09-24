package org.apache.chemistry.opencmis.jcr;

import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The test covers org.apache.chemistry.opencmis.jcr.JcrRepository class.
 * 
 * @author sergey.nikolaew@engagepoint.com
 *
 */
public class JcrFolderTest extends AbstractJcrSessionTest {
	private static Logger log = LoggerFactory.getLogger(JcrFolderTest.class);
	

	public void testRootFolder(){
		ObjectData objData = getRootFolder();
		log.info("Root folder ID: " + objData.getId());
		Assert.assertNotNull(objData);
	}
	

	public void testJcrTypeProperty(){
		getRootFolder().getProperties();
		PropertiesImpl properties = new PropertiesImpl();
        
        properties.addProperty(new PropertyIdImpl(PropertyIds.OBJECT_TYPE_ID, "cmis:folder"));
        properties.addProperty(new PropertyStringImpl(PropertyIds.NAME, "TestFolder"));
		String testFolderId = getJcrRepository().createFolder(getSession(), properties, getRootFolder().getId());
		ObjectData testFolder = getJcrRepository().getObject(getSession(), testFolderId, null, null, null, false);
		Assert.assertNotNull(testFolder.getProperties().getProperties()
				.get(PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS).getValues());
	}
	
	
	
}
