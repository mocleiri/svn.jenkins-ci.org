package hudson.plugins.jira;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.FreeStyleBuild;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;

import com.google.common.collect.Sets;

/**
 * Test case for the JIRA {@link Updater}.
 * 
 * @author kutzi
 */
@SuppressWarnings("unchecked")
public class UpdaterTest {
	
	private static class MockEntry extends Entry {

		private final String msg;

		public MockEntry(String msg) {
			this.msg = msg;
		}
		
		@Override
		public Collection<String> getAffectedPaths() {
			return null;
		}

		@Override
		public User getAuthor() {
			return null;
		}

		@Override
		public String getMsg() {
			return this.msg;
		}
	}
	
	@Test
	public void testFindIssues() {
		FreeStyleBuild build = mock(FreeStyleBuild.class);
		ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
		
		when(changeLogSet.iterator()).thenReturn(Collections.EMPTY_LIST.iterator());
		when(build.getChangeSet()).thenReturn(changeLogSet);
		
		Set<String> ids = new HashSet<String>();
		Updater.findIssues(build, ids);
		Assert.assertTrue(ids.isEmpty());
		

		Set<? extends Entry> entries = Sets.newHashSet(new MockEntry("Fixed JIRA-4711"));
		when(changeLogSet.iterator()).thenReturn(entries.iterator());
		
		ids = new HashSet<String>();
		Updater.findIssues(build, ids);
		Assert.assertEquals(1, ids.size());
		Assert.assertEquals("JIRA-4711", ids.iterator().next());
		
		// now test multiple ids
		entries = Sets.newHashSet(
				new MockEntry("Fixed BL-4711"),
				new MockEntry("TR-123: foo"),
				new MockEntry("[ABC-42] hallo"),
				new MockEntry("#123: this one must not match"),
				new MockEntry("ABC-: this one must also not match"));
		when(changeLogSet.iterator()).thenReturn(entries.iterator());
		
		ids = new TreeSet<String>();
		Updater.findIssues(build, ids);
		Assert.assertEquals(3, ids.size());
		Set<String> expected = Sets.newTreeSet(Sets.newHashSet(
				"BL-4711", "TR-123", "ABC-42"));
		Assert.assertEquals(expected, ids);
	}
	
	@Test
	@Bug(729)
	public void testDigitsInProjectNameAllowed() {
		FreeStyleBuild build = mock(FreeStyleBuild.class);
		ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
		when(build.getChangeSet()).thenReturn(changeLogSet);
		
		Set<? extends Entry> entries = Sets.newHashSet(new MockEntry("Fixed JI123-4711"));
		when(changeLogSet.iterator()).thenReturn(entries.iterator());
		
		Set<String> ids = new HashSet<String>();
		Updater.findIssues(build, ids);
		Assert.assertEquals(1, ids.size());
		Assert.assertEquals("JI123-4711", ids.iterator().next());
	}
	
	@Test
	@Bug(4092)
	public void testUnderscoreInProjectNameAllowed() {
		FreeStyleBuild build = mock(FreeStyleBuild.class);
		ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
		when(build.getChangeSet()).thenReturn(changeLogSet);
		
		Set<? extends Entry> entries = Sets.newHashSet(new MockEntry("Fixed FOO_BAR-4711"));
		when(changeLogSet.iterator()).thenReturn(entries.iterator());
		
		Set<String> ids = new HashSet<String>();
		Updater.findIssues(build, ids);
		Assert.assertEquals(1, ids.size());
		Assert.assertEquals("FOO_BAR-4711", ids.iterator().next());
	}
	
	@Test
	@Bug(4132)
	public void testLowercaseProjectNameAllowed() {
		FreeStyleBuild build = mock(FreeStyleBuild.class);
		ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
		when(build.getChangeSet()).thenReturn(changeLogSet);
		
		Set<? extends Entry> entries = Sets.newHashSet(new MockEntry("Fixed foo_bar-4711"));
		when(changeLogSet.iterator()).thenReturn(entries.iterator());
		
		Set<String> ids = new HashSet<String>();
		Updater.findIssues(build, ids);
		Assert.assertEquals(1, ids.size());
		Assert.assertEquals("FOO_BAR-4711", ids.iterator().next());
		
		entries = Sets.newHashSet(new MockEntry("Fixed FoO_bAr-4711"));
		when(changeLogSet.iterator()).thenReturn(entries.iterator());
		
		ids = new HashSet<String>();
		Updater.findIssues(build, ids);
		Assert.assertEquals(1, ids.size());
		Assert.assertEquals("FOO_BAR-4711", ids.iterator().next());
	}
}
