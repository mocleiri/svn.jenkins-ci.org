package hudson.plugins.filesystem_scm;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.Assert;
import org.junit.Test;

public class SimpleAntWildcardFilterTest {
	
	@Test
	public void test1() throws IOException {
		String tmp = System.getProperty("java.io.tmpdir");
		//System.out.println(tmp);
		String sep = System.getProperty("file.separator");
		//System.out.println(sep);
		File myDir = new File(tmp + sep + "abc001234" + sep + "def" + sep + "xyz.000");
		myDir.mkdirs();
		File f1 = new File(myDir, "aa.txt");
		f1.createNewFile();
		File f2 = new File(myDir, "bb.111");
		f2.createNewFile();
		File f3 = new File(myDir.getParentFile(), "ab.tx1");
		f3.createNewFile();
		
		IOFileFilter iof = new SimpleAntWildcardFilter("C:/users/samn/**/a?.tx?");
		Collection<File> coll = (Collection<File>)FileUtils.listFiles(new File(tmp + sep + "abc001234"), iof, HiddenFileFilter.VISIBLE);
		Assert.assertEquals(2, coll.size());
	}	

	@Test
	public void test2() {
		SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("**/CVS/*");
		Assert.assertEquals("(/.*)?/CVS/[^\\/]*$", filter.getPattern().toString());
	}
	
	@Test
	public void test3() {
		SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("org/apache/jakarta/**");
		Assert.assertEquals("/org/apache/jakarta(/.*)?$", filter.getPattern().toString());
	}
	
	@Test
	public void test4() {
		SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("**/*.gif");
		Assert.assertEquals("(/.*)?/[^\\/]*\\.gif$", filter.getPattern().toString());
	}	
	
	@Test
	public void test5() {
		SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("*/.hgignore");
		Assert.assertEquals("/[^\\/]*/\\.hgignore$", filter.getPattern().toString());
	}		
	
	@Test
	public void test6() {
		SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("/views/**/*.cfm");
		Assert.assertEquals("^/views(/.*)?/[^\\/]*\\.cfm$", filter.getPattern().toString());
	}		
	
	@Test
	public void test7() {
		SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("/views/index??.cfm");
		Assert.assertEquals("^/views/index..\\.cfm$", filter.getPattern().toString());
	}	
	
	@Test
	public void test8() {
		SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("WEB-INF/*-context.xml");
		Assert.assertEquals("/WEB-INF/[^\\/]*-context\\.xml$", filter.getPattern().toString());
	}	
	
	@Test
	public void test9() {
		SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("C:/some/path/*-context.xml");
		Assert.assertEquals("^C:/some/path/[^\\/]*-context\\.xml$", filter.getPattern().toString());
	}	
		
	@Test
	public void test10() {
		SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("**/abc/def.txt");
		Assert.assertEquals("(/.*)?/abc/def\\.txt$", filter.getPattern().toString());
	}	
	
	@Test
	public void test11() {
		SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("hello/ab/**");
		Assert.assertEquals("/hello/ab(/.*)?$", filter.getPattern().toString());
	}	

	@Test
	public void test12() {
		SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("**/**/a/b/**/");
		Assert.assertEquals("(/.*)?(/.*)?/a/b(/.*)?$", filter.getPattern().toString());
	}	
}
