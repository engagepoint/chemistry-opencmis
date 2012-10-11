package org.apache.chemistry.opencmis.jcr;

import java.math.BigInteger;

import junit.framework.Assert;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ChangeEventInfo;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.enums.CapabilityChanges;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.junit.Before;
import org.junit.Test;

/**
 * @author sergey.nikolaew@engagepoint.com
 * @date Oct 1, 2012
 */
public class JcrObservationJournalTest extends AbstractJcrSessionTest {
	
	private static final BigInteger PAGE_SIZE = new BigInteger("10");
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		
		Assert.assertEquals(CapabilityChanges.OBJECTIDSONLY, getJcrRepository()
				.getRepositoryInfo(getSession()).getCapabilities()
				.getChangesCapability());
		
		fillObservationJournal();
	}
	
	@Test
	public void testObservationJournal() throws Exception {
		((JackrabbitSession)getSession()).getUserManager();
		ObjectList objList = getJcrRepository().getContentChanges(getSession(),
				null, false, null, false, false, PAGE_SIZE, null);
		//The repository supports journaled observation 
		Assert.assertNotNull(objList);
		
		//event list isn't empty
		Assert.assertEquals(true, !objList.getObjects().isEmpty());
		
		ChangeEventInfo latestChangeEvent = objList.getObjects()
				.get(objList.getNumItems().intValue() - 1).getChangeEventInfo();
		long previousChangeLogToken = latestChangeEvent.getChangeTime().getTimeInMillis();
		objList = getJcrRepository().getContentChanges(getSession(),
				String.valueOf(previousChangeLogToken), false, null, false, false, PAGE_SIZE, null);
		
		latestChangeEvent = objList.getObjects()
				.get(objList.getNumItems().intValue() - 1).getChangeEventInfo();
		
		long latestChangeLogToken = latestChangeEvent.getChangeTime().getTimeInMillis();
		
		//Ensure the latest change log token is the really latest one.
		Assert.assertEquals(true, latestChangeLogToken > previousChangeLogToken);
		
		//In journal must remain some events. 
		Assert.assertEquals(Boolean.TRUE, objList.hasMoreItems());
	}
	
	private void fillObservationJournal() {
		PropertiesImpl properties = new PropertiesImpl();
        properties.addProperty(new PropertyStringImpl(PropertyIds.NAME, "OBSERVATION_TEST_FOLDER"));
        properties.addProperty(new PropertyIdImpl(PropertyIds.OBJECT_TYPE_ID, "cmis:folder"));

        String testFolderId = getJcrRepository().createFolder(getSession(), properties, getRootFolder().getId());
        properties = new PropertiesImpl();
        properties.addProperty(new PropertyStringImpl(PropertyIds.NAME, "NEW_NAME_TEST_FOLDER"));
        getJcrRepository().getJcrNode(getSession(), testFolderId)
        	.updateProperties(properties);
        getJcrRepository().deleteObject(getSession(), testFolderId, true);
	}

}
