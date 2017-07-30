package il.co.topq.difido;

import il.co.topq.difido.model.Enums;
import il.co.topq.difido.model.execution.Execution;
import il.co.topq.difido.model.execution.ScenarioNode;
import il.co.topq.difido.model.test.TestDetails;
import org.apache.commons.io.FileUtils;
import org.testng.ISuite;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LocalDifidoReporter extends AbstractDifidoReporter {

	private File reportDir;

	private ThreadLocal<File> currentTestFolder = new ThreadLocal<>();

	@Override
	protected void writeTestDetails(TestDetails testDetails) {
		long threadId = Thread.currentThread().getId();
		PersistenceUtils.writeTest(testDetails,
								   new File(reportDir + File.separator + "current"),
								   currentTestFolder.get());
	}

	@Override
	protected void writeExecution(Execution execution) {
		PersistenceUtils.writeExecution(execution, new File(reportDir + File.separator + "current"));
	}

	@Override
	public void onStart(ISuite suite) {
		super.onStart(suite);
		reportDir = new File(new File(suite.getOutputDirectory()).getParent(), "difido");
		if (!reportDir.exists()) {
			reportDir.mkdirs();
		}
		final File currentLogFolder = new File(reportDir, "current");
		if (currentLogFolder.exists()) {
			try {
				FileUtils.deleteDirectory(currentLogFolder);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		final File templateFolder = new File(reportDir, "template");
		if (!templateFolder.exists() || !(new File(templateFolder, "index.html").exists())) {
			PersistenceUtils.copyResources(templateFolder);
		}
		final File testDetailsHtmlFile = new File(currentLogFolder, PersistenceUtils.TEST_DETAILS_HTML_FILE);
		final File indexFile = new File(currentLogFolder, "index.html");
		if (!testDetailsHtmlFile.exists() && !indexFile.exists()) {
			try {
				FileUtils.copyDirectory(templateFolder, currentLogFolder);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void addFile(File file) {
		if (file == null || !file.exists() || !file.isFile()) {
			return;
		}
		try {
			Files.copy(Paths.get(file.getAbsolutePath()),
					   Paths.get(currentTestFolder.get() + File.separator + file.getName()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void updateTestDirectory() {
		currentTestFolder.set(new File(reportDir,
									   "current" + File.separator + "tests" + File.separator + "test_"
											   + getCurrentTest().getUid()));
	}

	@Override
	protected boolean shouldEscapeHtml(Enums.ElementType type) {
		switch (type) {
		case regular:
		case step:
		case startLevel:
		case stopLevel:
			return true;
		case lnk:
		case img:
		case html:
		default://by default, do nothing.
			return false;
		}
	}

	@Override
	public String getName() {
		return "DifidoLocalReporter";
	}

	@Override
	public String getCurrentTestFolder() {
		return currentTestFolder.get().getName();
	}

	@Override
	protected void onScenarioStart(ScenarioNode scenario) {
		// Unused
	}

}