/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.branch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests of {@link BranchProjectTracker}
 */
public class BranchProjectTrackerTest extends LocalRepositoryTestCase {

	private static final String BRANCH = "b1";

	private Repository repository;

	@Before
	public void setup() throws Exception {
		closeWelcomePage();
		File repoFile = createProjectAndCommitToRepository();
		assertNotNull(repoFile);
		repository = RepositoryCache.INSTANCE.lookupRepository(repoFile);
		assertNotNull(repository);
		org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore()
				.setValue(getPreferenceKey(Constants.MASTER), "");
		org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore()
				.setValue(getPreferenceKey(BRANCH), "");
	}

	private String getPreferenceKey(String branch) {
		return ProjectTrackerPreferenceHelper.getPreferenceKey(repository,
				branch);
	}

	@Test
	public void twoProjectsWithOnlyOneOnBranch() throws Exception {
		String[] paths = getProjectPaths();
		assertNotNull(paths);
		assertEquals(0, paths.length);
		assertNotNull(Git.wrap(repository).branchCreate().setName(BRANCH)
				.call());
		BranchOperationUI.checkout(repository, BRANCH).start();
		TestUtil.joinJobs(JobFamilies.CHECKOUT);

		paths = getProjectPaths(Constants.MASTER);
		assertNotNull(paths);
		assertEquals(2, paths.length);

		IProject project1 = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);
		IProject project2 = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ2);
		assertTrue(project1.exists());
		assertTrue(project2.exists());
		project1.delete(true, true, new NullProgressMonitor());
		assertNotNull(Git.wrap(repository).commit().setAll(true)
				.setMessage("deleting project").call());
		assertFalse(project1.exists());

		BranchOperationUI.checkout(repository, Constants.MASTER).start();
		TestUtil.joinJobs(JobFamilies.CHECKOUT);

		paths = getProjectPaths(BRANCH);
		assertNotNull(paths);
		assertEquals(1, paths.length);

		assertTrue(project1.exists());
		assertTrue(project2.exists());

		BranchOperationUI.checkout(repository, BRANCH).start();
		TestUtil.joinJobs(JobFamilies.CHECKOUT);

		assertTrue(project1.exists());
		assertFalse(project1.isOpen());
		assertTrue(project2.exists());
		assertTrue(project2.isOpen());

		BranchOperationUI.checkout(repository, Constants.MASTER).start();
		TestUtil.joinJobs(JobFamilies.CHECKOUT);

		assertTrue(project1.exists());
		assertTrue(project1.isOpen());
		assertTrue(project2.exists());
		assertTrue(project2.isOpen());
	}

	private String[] getProjectPaths() {
		try {
			String branch = repository.getBranch();
			return getProjectPaths(branch);
		} catch (IOException e) {
			return new String[0];
		}
	}

	private String[] getProjectPaths(String branch) {
		List<String> value = ProjectTrackerPreferenceHelper
				.restoreFromPreferences(repository, branch);
		return value.toArray(new String[0]);
	}
}
