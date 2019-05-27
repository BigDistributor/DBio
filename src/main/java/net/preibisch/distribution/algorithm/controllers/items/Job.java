package main.java.net.preibisch.distribution.algorithm.controllers.items;

import java.io.File;

import com.google.common.io.Files;

import main.java.net.preibisch.distribution.tools.Tools;

public class Job extends Object {

	public static final String TASK_CLUSTER_NAME = "task.jar";

	private static String id;
	private static AppMode appMode;
	private static File tmpDir;

	private Job(String id, AppMode appMode, File tmpDir) {
		Job.id = id;
		Job.appMode = appMode;
		Job.tmpDir = tmpDir;
	}

	public Job(AppMode mode) {

		String id = Tools.id();

		File tmpDir = createTempDir();

		new Job(id, mode, tmpDir);
	}

	public Job() {
		new Job(AppMode.CLUSTER_INPUT_MODE);
	}

	public static File createTempDir() {
		File tempDir = Files.createTempDir();
		System.out.println("tmp Dir: "+tempDir.getAbsolutePath());
		return tempDir;
	}

	public static String getId() {
		return id;
	}

	public static AppMode getAppMode() {
		return appMode;
	}

	public static File getTmpDir() {
		return tmpDir;
	}

	public static File file(String string) {
		return new File(tmpDir,string);
	}

}
