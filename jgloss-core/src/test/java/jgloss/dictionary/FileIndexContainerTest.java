package jgloss.dictionary;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import jgloss.dictionary.FileIndexContainer.IndexMetaData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class FileIndexContainerTest {
	private static final int TEST_INDEX_TYPE = 0xabc;
	
	private static final ByteBuffer TEST_INDEX_DATA = initTestIndexData();
	
	private static final int TEST_INDEX_DATA_SIZE = TEST_INDEX_DATA.capacity();
	
	private File indexFile;
	
	
	private static ByteBuffer initTestIndexData() {
		try {
	        return ByteBuffer.wrap("foo".getBytes("ASCII"));
        } catch (UnsupportedEncodingException ex) {
	        throw new AssertionError(ex);
        }
    }

	@Before
	public void createTestIndexFile() throws IOException {
		indexFile = File.createTempFile(FileIndexContainerTest.class.getSimpleName(), FileIndexContainer.EXTENSION);
		assertTrue("failed to delete empty temp index file", indexFile.delete()); // file must not exist for index creation to succeed 
	}

	@After
	public void deleteTestIndexFile() {
		indexFile.delete();
	}
	
	@Test
	public void testCreateNewIndexFile() throws IndexException, FileNotFoundException, IOException {
		createEmptyIndexFile();	
		
		RandomAccessFile file = new RandomAccessFile(indexFile, "r");
		try {
			verifyHeader(file);
		} finally {
			file.close();
		}
	}
	
	@Test
	public void testCanAccessEditInAccessMode() throws IndexException, FileNotFoundException, IOException {
		createEmptyIndexFile();		
		
		FileIndexContainer indexContainer = new FileIndexContainer(indexFile, false);
		assertTrue(indexContainer.canAccess());
		assertFalse(indexContainer.canEdit());
	}
	
	@Test
	public void testCanAccessEditInEditMode() throws IndexException, FileNotFoundException, IOException {
		createEmptyIndexFile();		
		
		FileIndexContainer indexContainer = new FileIndexContainer(indexFile, true);
		assertFalse(indexContainer.canAccess());
		assertTrue(indexContainer.canEdit());
	}
	
	@Test
	public void testGetIndexByteOrder() throws IndexException, FileNotFoundException, IOException {
		createEmptyIndexFile();
		
		FileIndexContainer indexContainer = new FileIndexContainer(indexFile, true);
		assertEquals(ByteOrder.nativeOrder(), indexContainer.getIndexByteOrder());
	}
	
	@Test
	public void testCreateIndex() throws FileNotFoundException, IOException {
		FileIndexContainer indexContainer = createEmptyIndexFile(false);
		try {
			indexContainer.createIndex(TEST_INDEX_TYPE, TEST_INDEX_DATA);
			assertTrue(indexContainer.hasIndex(TEST_INDEX_TYPE));
			assertFalse(indexContainer.hasIndex(TEST_INDEX_TYPE + 1));
		} finally {
			indexContainer.close();
		}
		
		RandomAccessFile file = new RandomAccessFile(indexFile, "r");
		try {
			verifyHeader(file);
			verifyIndex(file);
		} finally {
			file.close();
		}
	}
	
	@Test
	public void testEndEditing() throws FileNotFoundException, IOException {
		FileIndexContainer indexContainer = createEmptyIndexFile(false);
		try {
			indexContainer.createIndex(TEST_INDEX_TYPE, TEST_INDEX_DATA);
			indexContainer.endEditing();
			assertFalse(indexContainer.canEdit());
			assertTrue(indexContainer.canAccess());
			ByteBuffer actualData = indexContainer.getIndexData(TEST_INDEX_TYPE);
			assertEquals(TEST_INDEX_DATA, actualData);
		} finally {
			indexContainer.close();
		}
		
	}
	
	@Test
	public void testHasIndexPreexistingFile() throws FileNotFoundException, IOException {
		FileIndexContainer indexContainer = createAndReopenIndexFile();
		try {
			assertTrue(indexContainer.hasIndex(TEST_INDEX_TYPE));
			assertFalse(indexContainer.hasIndex(TEST_INDEX_TYPE+1));
		} finally {
			indexContainer.close();
		}
	}
	
	@Test
	public void testGetIndexDataPreexistingFile() throws FileNotFoundException, IOException {
		FileIndexContainer indexContainer = createAndReopenIndexFile();
		try {
			ByteBuffer actualData = indexContainer.getIndexData(TEST_INDEX_TYPE);
			assertEquals(TEST_INDEX_DATA, actualData);
		} finally {
			indexContainer.close();
		}
	}

	private void createEmptyIndexFile() throws FileNotFoundException, IOException {
		createEmptyIndexFile(true);
	}
	
	private FileIndexContainer createEmptyIndexFile(boolean close) throws FileNotFoundException, IOException {
	    assertFalse("empty test index file was not deleted", indexFile.exists());
		
		FileIndexContainer container = new FileIndexContainer(indexFile, true);
		
		if (close) {
			container.close();
		}
		
		return container;
    }

	private FileIndexContainer createAndReopenIndexFile() throws FileNotFoundException, IOException {
		FileIndexContainer indexContainer = createEmptyIndexFile(false);
		indexContainer.createIndex(TEST_INDEX_TYPE, TEST_INDEX_DATA);
		indexContainer.close();
		
		return new FileIndexContainer(indexFile, false);
	}

	private void verifyHeader(RandomAccessFile file) throws IOException {
	    assertEquals(FileIndexContainer.MAGIC, file.readInt());
	    assertEquals(FileIndexContainer.VERSION, file.readInt());
	    assertEquals(FileIndexContainer.INDEXCONTAINER_HEADER_LENGTH, file.readInt());
	    int expectedByteOrderCode = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN ? FileIndexContainer.BIG_ENDIAN : FileIndexContainer.LITTLE_ENDIAN; 
	    int byteOrderCode = file.readInt();
	    assertEquals(expectedByteOrderCode, byteOrderCode);
    }

	private void verifyIndex(RandomAccessFile file) throws IOException {
	    assertEquals(FileIndexContainer.INDEXCONTAINER_HEADER_LENGTH, file.getFilePointer());
	    assertEquals(TEST_INDEX_TYPE, file.readInt());
	    assertEquals(TEST_INDEX_DATA_SIZE, file.readInt());
	    assertEquals(IndexMetaData.INDEX_OFFSET, file.readInt());
	    byte[] actualData = new byte[TEST_INDEX_DATA_SIZE];
	    file.readFully(actualData);
	    assertArrayEquals(TEST_INDEX_DATA.array(), actualData);
    }
}
