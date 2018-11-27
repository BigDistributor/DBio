package main.java.com.clustering;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.jcraft.jsch.JSchException;

import main.java.com.blockmanager.Block;
import main.java.com.blockmanager.BlocksManager;
import main.java.com.clustering.jsch.SCP;
import main.java.com.clustering.kafka.JobConsumer;
import main.java.com.clustering.scripting.BatchGenerator;
import main.java.com.clustering.scripting.ShellGenerator;
import main.java.com.controllers.items.AppMode;
import main.java.com.gui.items.Colors;
import main.java.com.gui.items.LogPanel;
import main.java.com.gui.items.ProgressBarPanel;
import main.java.com.tools.Config;
import main.java.com.tools.Helper;
import mpicbg.spim.io.IOFunctions;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;

public class WorkflowFunction {
	public List<Block> blocks;
	public static HashMap<Integer, Block> blockMap;
	public ProgressBarPanel progressBarPanel;

	public WorkflowFunction() {
		progressBarPanel = new ProgressBarPanel(0, 100);

	}

	public void sendTask(MyCallBack callBack) {
		Thread task = new Thread(new Runnable() {

			@Override
			public void run() {
				Boolean valid = true;
				IOFunctions.println("Send Task..");
				progressBarPanel.updateBar(0);
				System.out.println("Task in cloud: "+ Config.getJob().getTask().getAll());
				System.out.println("Task in local: "+Config.getLogin().getServer().getPath() + "/task.jar");
				try {
					SCP.send(Config.getLogin(), Config.getJob().getTask().getAll(),
							Config.getLogin().getServer().getPath() + "/task.jar", -1);
				} catch (JSchException e) {
					valid = false;
					// TODO Fix Connection
					e.printStackTrace();
					callBack.onError(e.toString());
				} catch (IOException e) {
					// TODO retry
					valid = false;
					e.printStackTrace();
					callBack.onError(e.toString());
				}
				progressBarPanel.updateBar(100);
				if (valid)
					callBack.onSuccess();
			}
		});
		task.start();
	}

	public void cleanFolder(String dir, MyCallBack callback) {
		try {
			FileUtils.cleanDirectory(new File(dir));
		} catch (IOException e) {
			callback.onError(e.toString());
		}
		callback.onSuccess();
	}

	public void generateInput(MyCallBack callback) {
		Thread task = new Thread(new Runnable() {
			@Override
			public void run() {
				Boolean valid = true;
				IOFunctions.println("Generate input blocks..");
				progressBarPanel.updateBar(0);
				blocks = BlocksManager.generateBlocks(Config.getDataPreview(), callback);
				//TODO create object to send ProcessData
				Config.setTotalInputFiles(blocks.size());
				try {
					blockMap = BlocksManager.saveBlocks(
//						IOFunctions.openAs32Bit(new File(Config.getJob().getInput().getFile().getAll())), 
							Config.getJob().getInput().getLoader().fuse(),
							Config.getDataPreview().getBlocksSizes(),
							blocks,
							new MyCallBack() {

						@Override
						public void onSuccess() {
							// TODO Auto-generated method stub

						}

						@Override
						public void onError(String error) {
							// TODO Auto-generated method stub

						}

						@Override
						public void log(String log) {
							IOFunctions.println(log);

						}
					});
				} catch (IncompatibleTypeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Config.setBlocks(blockMap.size());
				if (valid)
					callback.onSuccess();
			}
		});
		task.run();
	}

	public void sendInput(MyCallBack callBack) {
		Thread task = new Thread(new Runnable() {

			@Override
			public void run() {
				Boolean valid = true;
				IOFunctions.println("Send input files..");
				progressBarPanel.updateBar(0);
				String local = Config.getTempFolderPath();
				ArrayList<String> files = Helper.getFiles(local, Config.getInputPrefix());
				Config.setBlocksFilesNames(files);
				int key = 0;
				for (String file : files) {
					try {
						SCP.send(Config.getLogin(), local + "//" + file,
								Config.getLogin().getServer().getPath() + "//" + file, key);
					} catch (JSchException | IOException e) {
						valid = false;
						callBack.onError(e.toString());
					}
					key++;
					progressBarPanel.updateBar((key * 100) / files.size());
				}
				if (valid)
					callBack.onSuccess();
			}
		});
		task.run();
	}

	public static void generateShell(MyCallBack callback) {
		Thread task = new Thread(new Runnable() {
			@Override
			public void run() {
				IOFunctions.println("Generate Script..");
				ShellGenerator.generateTaskShell(callback);
			}
		});
		task.run();
	}

	public static void generateBatch(int job, MyCallBack callback) {
		Thread task = new Thread(new Runnable() {

			@Override
			public void run() {
				IOFunctions.println("Generate Batch..");
				if (AppMode.LocalInputMode.equals(Config.getJob().getAppMode())) {
					BatchGenerator.GenerateBatchForLocalFiles(job, Config.getTotalInputFiles(), callback);
				} else if (AppMode.ClusterInputMode.equals(Config.getJob().getAppMode())) {
					BatchGenerator.GenerateBatchForClusterFile(callback);
				}
			}
		});
		task.run();
	}

	public static void sendShell(MyCallBack callBack) {
		Thread task = new Thread(new Runnable() {

			@Override
			public void run() {
				Boolean valid = true;
				IOFunctions.println("Send Shell..");
				try {
					
					System.out.println("Local task sh:"+Config.getTempFolderPath() + "//task.sh");
					System.out.println("Cloud task sh:"+Config.getLogin().getServer().getPath() + "task.sh");
					SCP.send(Config.getLogin(), "tools//logProvider.sh",
							Config.getLogin().getServer().getPath() + "logProvider.sh", -1);
					SCP.send(Config.getLogin(), "tools//logProvider.jar",
							Config.getLogin().getServer().getPath() + "logProvider.jar", -1);
					SCP.send(Config.getLogin(), "tools//task.sh",
							Config.getLogin().getServer().getPath() + "task.sh", -1);
				} catch (JSchException e) {
					valid = false;
					callBack.onError(e.toString());
					e.printStackTrace();
					try {
						SCP.connect(Config.getLogin());
					} catch (JSchException e1) {
						IOFunctions.println("Invalide Host");
						e1.printStackTrace();
					}
				} catch (IOException e) {
					callBack.onError(e.toString());
					e.printStackTrace();
				}

				if (valid)
					callBack.onSuccess();
			}
		});
		task.run();
	}

	public static void sendBatch(MyCallBack callBack) {
		Thread task = new Thread(new Runnable() {

			@Override
			public void run() {
				Boolean valid = true;
				IOFunctions.println("Send submit..");
				try {
					SCP.send(Config.getLogin(), Config.getTempFolderPath() + "//submit.cmd",
							Config.getLogin().getServer().getPath() + "submit.cmd", -1);
				} catch (JSchException e) {
					valid = false;
					callBack.onError(e.toString());
					e.printStackTrace();
					try {
						SCP.connect(Config.getLogin());
					} catch (JSchException e1) {
						IOFunctions.println("Invalide Host");
						e1.printStackTrace();
					}
				} catch (IOException e) {
					callBack.onError(e.toString());
					e.printStackTrace();
				}

				if (valid)
					callBack.onSuccess();
			}
		});
		task.run();
	}

	public static void runBatch(MyCallBack callback) {
		Thread task = new Thread(new Runnable() {

			@Override
			public void run() {
				Boolean valid = true;
				IOFunctions.println("Run Submit..");
				try {
					SCP.run(Config.getLogin(), "submit.cmd", callback);
				} catch (JSchException e) {
					try {
						SCP.connect(Config.getLogin());
					} catch (JSchException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					valid = false;
					callback.onError(e.toString());
					e.printStackTrace();
				}

				if (valid)
					callback.onSuccess();
			}
		});
		task.run();
	}

	public void getAllDataBack(MyCallBack callBack) {
		Thread task = new Thread(new Runnable() {

			@Override
			public void run() {
				if (AppMode.ClusterInputMode.equals(Config.getJob().getAppMode())) {
					Boolean valid = true;
					System.out.println("Get Data back..");
					progressBarPanel.updateBar(0);
					try {

						SCP.get(Config.getLogin(), Config.getJob().getInput().getFile().getAll(),
								Config.getTempFolderPath() + "//file.tif", 0);
						IOFunctions.println("file got with success !");
						progressBarPanel.updateBar(1);

					} catch (IOException e) {
						valid = false;
						callBack.onError(e.toString());
						e.printStackTrace();
					} catch (JSchException e) {
						try {
							SCP.connect(Config.getLogin());
						} catch (JSchException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						valid = false;
						callBack.onError(e.toString());
						e.printStackTrace();
					} catch (IndexOutOfBoundsException e) {
						e.printStackTrace();
						// callBack.onSuccess();
					}

					if (valid)
						callBack.onSuccess();
				} else if (AppMode.LocalInputMode.equals(Config.getJob().getAppMode())) {
					Boolean valid = true;
					System.out.println("Get Data back..");
					progressBarPanel.updateBar(0);
					// ArrayList<String> files = Config.getBlocksFilesNames();
					// int key = 0;
					for (int i = 1; i <= Config.getTotalInputFiles(); i++) {
						String file = i + Config.getInputPrefix();
						try {
							SCP.get(Config.getLogin(), Config.getLogin().getServer().getPath()+ "//" + file,
									Config.getTempFolderPath() + "//" + file, i - 1);

							IOFunctions.println("block " + i + " got with success !");
							progressBarPanel.updateBar((i * 100) / Config.getTotalInputFiles());
						} catch (IOException e) {
							valid = false;
							callBack.onError(e.toString());
							e.printStackTrace();
						} catch (JSchException e) {
							try {
								SCP.connect(Config.getLogin());
							} catch (JSchException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							valid = false;
							callBack.onError(e.toString());
							e.printStackTrace();
						} catch (IndexOutOfBoundsException e) {
							e.printStackTrace();
							// callBack.onSuccess();
						}

					}
					if (valid)
						callBack.onSuccess();
				}
			}
		});
		task.run();
	}

	public void getBlockDataBack(int start, int end, MyCallBack callBack) {
		Thread task = new Thread(new Runnable() {

			@Override
			public void run() {
				if (AppMode.ClusterInputMode.equals(Config.getJob().getAppMode())) {
					throw new Error("Cluster blocks get not implimented yet");
				} else if (AppMode.LocalInputMode.equals(Config.getJob().getAppMode())) {
					Boolean valid = true;
					System.out.println("Get Data back..");
					progressBarPanel.updateBar(0);
					// ArrayList<String> files = Config.getBlocksFilesNames();
					// int key = 0;
					for (int i = start; i <= end; i++) {
						String file = i + Config.getInputPrefix();
						try {
							SCP.get(Config.getLogin(), Config.getLogin().getServer().getPath() + "//" + file,
									Config.getTempFolderPath() + "//" + file, i - 1);

							IOFunctions.println("block " + i + " got with success !");
							progressBarPanel.updateBar((i * 100) / Config.getTotalInputFiles());
						} catch (IOException e) {
							valid = false;
							callBack.onError(e.toString());
							e.printStackTrace();
						} catch (JSchException e) {
							try {
								SCP.connect(Config.getLogin());
							} catch (JSchException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							valid = false;
							callBack.onError(e.toString());
							e.printStackTrace();
						} catch (IndexOutOfBoundsException e) {
							e.printStackTrace();
							// callBack.onSuccess();
						}

					}
					if (valid)
						callBack.onSuccess();
				}
			}
		});
		task.run();
	}

	public void combineData(MyCallBack callBack) {
		Thread task = new Thread(new Runnable() {

			@Override
			public void run() {
				IOFunctions.println("Generate result..");
				Config.resultImage = BlocksManager.generateResult(blockMap, Config.getTempFolderPath(), callBack);
				callBack.onSuccess();
			}
		});
		task.run();

	}

	public void startStatusListener() {
		IOFunctions.println("Get Status..");
		JobConsumer consumerThread = new JobConsumer(new MyCallBack() {
			@Override
			public void onSuccess() {
				// TODO Auto-generated method stub
			}

			@Override
			public void onError(String error) {
				// TODO Auto-generated method stub
			}

			@Override
			public void log(String log) {
				System.out.println("Log got: " + log);
				processClusterLog(log);
			}
		});
		consumerThread.start();
	}

	private void assembleBlockToResult(int key, MyCallBack callback) {
		try {
		String string = Config.getTempFolderPath() + "/" + key + ".tif";
		Img<FloatType> tmp = IOFunctions.openAs32Bit(new File(string));
		blockMap.get(key).pasteBlock(Config.resultImage, tmp, callback);
		callback.onSuccess();
		}catch(Exception e ) {
			System.out.println(e.toString());
			System.out.println("Can't past to result");
		}
	}

	private void processClusterLog(String log) {
		String[] parts = log.split(";");
		try {
//			System.out.println("ProcessLog:"+ parts[1] +" - " + Config.getUUID()+" "+(parts[1] == Config.getUUID())+" "+parts[1].equals(Config.getUUID()));
			if (parts[1].equals(Config.getLogin().getId()) ) {
				IOFunctions.println("Log got:" + log);
				int id = Integer.parseInt(parts[2]);

				for (int j = (id - 1) * Config.parallelJobs; j < id * Config.parallelJobs; j++) {
					try {
						Config.getDataPreview().getBlocksPreview().get(j).setStatus(Colors.PROCESSED);

					} catch (IndexOutOfBoundsException e) {
						System.out.println("Error! Invalide elmn");
					}
				}
				getBlockDataBack((id - 1) * Config.parallelJobs + 1, id * Config.parallelJobs, new MyCallBack() {

					@Override
					public void onSuccess() {
						for (int i = (id - 1) * Config.parallelJobs + 1; i <= id * Config.parallelJobs; i++) {
							assembleBlockToResult(i, new MyCallBack() {

								@Override
								public void onSuccess() {
//									ImageJFunctions.show(Config.resultImage).setTitle("Result");

								}

								@Override
								public void onError(String error) {
									// TODO Auto-generated method stub

								}

								@Override
								public void log(String log) {
									// TODO Auto-generated method stub

								}
							});

						}
					}

					@Override
					public void onError(String error) {
						// TODO Auto-generated method stub

					}

					@Override
					public void log(String log) {
						// TODO Auto-generated method stub

					}
				});

			}
		} catch (NumberFormatException e) {
			System.out.println("Error! converting log error");
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Error! Invalide log");
		}
	}
}
