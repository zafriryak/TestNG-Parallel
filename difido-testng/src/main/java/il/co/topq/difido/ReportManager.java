package il.co.topq.difido;

import il.co.topq.difido.model.Enums.ElementType;
import il.co.topq.difido.model.Enums.Status;
import org.testng.*;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.StampedLock;

public class ReportManager implements ReportDispatcher {

	private static ReportManager instance;
	private final List<Reporter> reporters;
	private ThreadLocal<Status> testStatus = new ThreadLocal<>();
	/*
	 * (non-Javadoc)
	 *
	 * @see il.co.topq.difido.ReportDispatcher#startLevel(java.lang.String)
	 */
	private StampedLock logLock = new StampedLock();
	/*
	 * (non-Javadoc)
	 *
	 * @see il.co.topq.difido.ReportDispatcher#endLevel()
	 */
	private StampedLock endLevelLock = new StampedLock();

	public ReportManager() {
		reporters = new LinkedList<Reporter>();
		reporters.add(new LocalDifidoReporter());
	}

	@SuppressWarnings("unchecked")
	public static ReportDispatcher getInstance() {
		if (null == instance) {
			instance = new ReportManager();
		}
		return instance;
	}

	public void onTestStart(ITestResult result) {
		for (Reporter reporter : reporters) {
			reporter.onTestStart(result);
		}
	}

	@Override
	public void logHtml(String title, Status status) {
		log(title, null, status, ElementType.html);
	}

	@Override
	public void logHtml(String title, String message, Status status) {
		log(title, message, status, ElementType.html);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see il.co.topq.difido.ReportDispatcher#log(java.lang.String)
	 */

	public void log(String title, String message, Status status, ElementType type) {
		updateStatus(status);
		for (Reporter reporter : reporters) {
			reporter.log(title, message, status, type);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see il.co.topq.difido.ReportDispatcher#log(java.lang.String,
	 * il.co.topq.difido.model.Enums.Status)
	 */

	private void updateStatus(Status status) {
		Status currentStatus = testStatus.get();
		if ((null == currentStatus) || (currentStatus.ordinal() < status.ordinal())) {
			testStatus.set(status);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see il.co.topq.difido.ReportDispatcher#log(java.lang.String,
	 * java.lang.String)
	 */

	@Override
	public void log(String title) {
		log(title, null, Status.success, ElementType.regular);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see il.co.topq.difido.ReportDispatcher#log(java.lang.String,
	 * java.lang.String, il.co.topq.difido.model.Enums.Status)
	 */

	@Override
	public void log(String title, Status status) {
		log(title, null, status, ElementType.regular);
	}

	@Override
	public void log(String title, String message) {
		log(title, message, Status.success, ElementType.regular);

	}

	@Override
	public synchronized void log(String title, String message, Status status) {
		log(title, message, status, ElementType.regular);
	}

	@Override
	public void startLevel(String description) {
		long sLock = logLock.writeLock();
		try {
			log(description, null, Status.success, ElementType.startLevel);
		} finally {
			logLock.unlockWrite(sLock);
		}
	}

	@Override
	public void endLevel() {
		long sLock = endLevelLock.writeLock();
		try {
			log(null, null, Status.success, ElementType.stopLevel);
		} finally {
			endLevelLock.unlockWrite(sLock);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see il.co.topq.difido.ReportDispatcher#step(java.lang.String)
	 */

	@Override
	public void step(String description) {
		log(description, null, Status.success, ElementType.step);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see il.co.topq.difido.ReportDispatcher#addFile(java.io.File,
	 * java.lang.String)
	 */
	@Override
	public void addFile(File file, String description) {
		for (Reporter reporter : reporters) {
			reporter.addFile(file);
			if (null == description) {
				reporter.log(file.getName(), file.getName(), Status.success, ElementType.lnk);
			} else {
				reporter.log(description, file.getName(), Status.success, ElementType.lnk);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see il.co.topq.difido.ReportDispatcher#addImage(java.io.File,
	 * java.lang.String)
	 */

	@Override
	public void addImage(File file, String description) {
		for (Reporter reporter : reporters) {
			reporter.addFile(file);
			reporter.log(description, file.getName(), Status.success, ElementType.img);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see il.co.topq.difido.ReportDispatcher#addLink(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void addLink(String link, String description) {
		for (Reporter reporter : reporters) {
			reporter.log(description, link, Status.success, ElementType.lnk);
		}
	}

	public void beforeConfiguration(ITestResult result) {
		for (Reporter reporter : reporters) {
			reporter.beforeConfiguration(result);
		}
	}

	public void onConfigurationSuccess(ITestResult result) {
		for (Reporter reporter : reporters) {
			reporter.onConfigurationSuccess(result);
		}
	}

	public void onConfigurationFailure(ITestResult result) {
		for (Reporter reporter : reporters) {
			reporter.onConfigurationFailure(result);
		}
	}

	public void onConfigurationSkip(ITestResult result) {
		for (Reporter reporter : reporters) {
			reporter.onConfigurationSkip(result);
		}
	}

	public void onTestSuccess(ITestResult result) {
		for (Reporter reporter : reporters) {
			reporter.onTestSuccess(result);
		}
		if (testStatus.get() != null) {
			if (testStatus.get().equals(Status.failure)) {
				result.setStatus(ITestResult.FAILURE);
				if (result.getMethod().getRetryAnalyzer() != null) {
					System.out.println("retry occured");
					Assert.fail();
				}
			}
		}
	}

	public void onTestFailure(ITestResult result) {
		for (Reporter reporter : reporters) {
			reporter.onTestFailure(result);
		}
	}

	public void onTestSkipped(ITestResult result) {
		for (Reporter reporter : reporters) {
			reporter.onTestSkipped(result);
		}
	}

	public void onStart(ITestContext context) {
		for (Reporter reporter : reporters) {
			reporter.onStart(context);
		}
	}

	public void onFinish(ITestContext context) {
		for (Reporter reporter : reporters) {
			reporter.onFinish(context);
		}
	}

	public void onStart(ISuite suite) {
		for (Reporter reporter : reporters) {
			reporter.onStart(suite);
		}
	}

	public void onFinish(ISuite suite) {
		for (Reporter reporter : reporters) {
			reporter.onFinish(suite);
		}
	}

	@Override
	public void addTestProperty(String name, String value) {
		for (Reporter reporter : reporters) {
			reporter.addTestProperty(name, value);
		}
	}

	@Override
	public void addRunProperty(String name, String value) {
		for (Reporter reporter : reporters) {
			reporter.addRunProperty(name, value);
		}

	}

	@Override
	public String getCurrentTestFolder() {
		for (Reporter reporter : reporters) {
			return reporter.getCurrentTestFolder();
		}
		return null;
	}

	@Override
	public Status getCurrentTestStatus() {
		for (Reporter reporter : reporters) {

		}
		return testStatus.get();

	}

	@Override
	public void beforeInvocation(IInvokedMethod method, ITestResult testResult, ITestContext context) {
		//empty implementation, I think there's nothing to do here.
	}

	@Override
	public void afterInvocation(IInvokedMethod method, ITestResult testResult, ITestContext context) {
		failTestNG(testResult);
	}

	@Override
	public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
		//empty implementation, I think there's nothing to do here.
	}

	private void failTestNG(ITestResult testResult) {

		Status status = testStatus.get();
		if (null != status) {
			switch (status) {
			case warning:
				break;
			case error:
				Assert.fail("Test status is error");
			case failure:
				testResult.setStatus(ITestResult.FAILURE);
			case in_progress:
			case success:
				break;
			}
		}
	}

	@Override
	public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
		failTestNG(testResult);
	}

	public void validateTestClassNode(String testWave, String testClass, Exception e) {
		for (Reporter reporter : reporters) {
			reporter.validateTestClassNode(testWave, testClass, e);
		}

	}

	public void validateTestMethodNode(String testWave, String testClass, String testMethod, Exception e) {
		for (Reporter reporter : reporters) {
			reporter.validateTestMethodNode(testWave, testClass, testMethod, e);
		}

	}

	public void validateTestNGMethodSignature(String testWave, String testClass, String testMethod, Exception e) {
		for (Reporter reporter : reporters) {
			reporter.validateTestNGMethodSignature(testWave, testClass, testMethod, e);
		}

	}

	public void validateTestMandatoryParamsAlert(String testWave, String testClass, String testMethod, Exception e) {
		for (Reporter reporter : reporters) {
			reporter.validateTestMandatoryParamsAlert(testWave, testClass, testMethod, e);
		}

	}

}
