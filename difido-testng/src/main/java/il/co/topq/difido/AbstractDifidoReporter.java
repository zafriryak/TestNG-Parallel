package il.co.topq.difido;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import il.co.topq.difido.model.Enums.ElementType;
import il.co.topq.difido.model.Enums.Status;
import il.co.topq.difido.model.execution.Execution;
import il.co.topq.difido.model.execution.MachineNode;
import il.co.topq.difido.model.execution.ScenarioNode;
import il.co.topq.difido.model.execution.TestNode;
import il.co.topq.difido.model.test.ReportElement;
import il.co.topq.difido.model.test.TestDetails;

/**
 * @author Ben Mark
 **/

public abstract class AbstractDifidoReporter implements Reporter {

	private ThreadLocal<SimpleDateFormat> testTimeElapsed = new ThreadLocal<>();
	private AtomicLong currentThreadId = new AtomicLong();
	private AtomicInteger startLevelOcurrences = new AtomicInteger(0);
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
	private AtomicBoolean currentTestNameWasSet = new AtomicBoolean(false);
	private ConcurrentHashSet<String> testClassNameSet;
	private ConcurrentMap<String, ScenarioNode> classNodeMap;
	private volatile Execution execution;
	private volatile ScenarioNode currentTestWave;
	private volatile MachineNode currentMachine;
	private volatile ScenarioNode currentClassScenario;
	private ThreadLocal<TestDetails> testDetails = new ThreadLocal<TestDetails>();
	private ThreadLocal<TestNode> currentTest = new ThreadLocal<TestNode>();
	private AtomicInteger index = new AtomicInteger(0);

	private volatile ThreadLocal<String> testClassName = new ThreadLocal<String>();
	private AtomicLong lastWrite = new AtomicLong();
	private final StampedLock onBeforeConfigLock = new StampedLock();
	private final StampedLock onTestInitLock = new StampedLock();
	private final StampedLock onTestStartLock = new StampedLock();
	private final StampedLock onTestSkipLock = new StampedLock();
	private final StampedLock onEndLock = new StampedLock();
	private final StampedLock reporterActionLock = new StampedLock();
	private final StampedLock onExceptionLock = new StampedLock();
	private final StampedLock onFailLock = new StampedLock();
	private final StampedLock onConfigurationFailLock = new StampedLock();
	private final StampedLock onTestFailLock = new StampedLock();
	private final StampedLock onTestCasesValidation = new StampedLock();
	private ConcurrentHashMap<String, ScenarioNode> testWaveMap;
	private ConcurrentHashSet<ScenarioNode> currentMachinechildren;
	private ThreadLocal<ScenarioNode> currentRetryParentTest = new ThreadLocal<>();
	private ConcurrentHashMap<String, ScenarioNode> retryParentTestNodeMap;
	private AtomicBoolean isRetryEnabled = new AtomicBoolean();
	private ThreadLocal<Exception> testCasesXmlValidationException = new ThreadLocal<>();
	private ConcurrentHashSet<String> testCasesValidationClassSet;
	private AtomicLong retryTestDuration = new AtomicLong();

	
	
	
	public AbstractDifidoReporter() {
		testClassNameSet = new ConcurrentHashSet<String>();
		classNodeMap = new ConcurrentHashMap<String, ScenarioNode>();
		currentMachinechildren = new ConcurrentHashSet<>();
		testWaveMap = new ConcurrentHashMap<>();
		retryParentTestNodeMap = new ConcurrentHashMap<>();
		testCasesValidationClassSet = new ConcurrentHashSet<>();
	}

	@Override
	public void onStart(ISuite suite) {
		if (currentMachine == null) {
			if (suite.getHost() == null) {
				currentMachine = new MachineNode(getMachineName());
			} else {
				currentMachine = new MachineNode(suite.getHost());
			}
		}
	}

	@Override
	public void validateTestClassNode(String testWave, String testClass,
			Exception e) {
		InitACustomTestNode(testWave, testClass, null, e, null);
		
	}

	@Override
	public void validateTestMethodNode(String testWave, String testClass,
			String testMethod, Exception e) {
		InitACustomTestNode(testWave, testClass, testMethod, e, null);
	}

	@Override
	public void validateTestNGMethodSignature(String testWave, String testClass,
			String testMethod, Exception e) {
		String iterationNodeName = testMethod + " - wrong method signature";
		InitACustomTestNode(testWave, testClass, testMethod, e,
				iterationNodeName);
	}
	
	@Override
	public void validateTestMandatoryParamsAlert(String testWave, String testClass,
			String testMethod, Exception e) {
		String iterationNodeName = testMethod+" - parameter missing";
		InitACustomTestNode(testWave, testClass, testMethod, e,
				iterationNodeName);
	}

	/**
	 * set scenarioNode aka testWave from beforeConf to create the wave
	 * parentNode
	 **/

	@Override
	public void beforeConfiguration(ITestResult result) {
		long sLock = onBeforeConfigLock.writeLock();
		try {
			initTestIterationNode(result);
		}
		finally {
			onBeforeConfigLock.unlockWrite(sLock);
		}
	}
	

	protected void initTestIterationNode(ITestResult result) {		
		long sLock = onTestInitLock.writeLock();
		try {

			if (result.getMethod().getConstructorOrMethod().getMethod()
					.getAnnotation(BeforeSuite.class) != null)
				return;
			if (result.getMethod().getConstructorOrMethod().getMethod()
					.getAnnotation(BeforeClass.class) != null)
				return;

			testTimeElapsed.set(new SimpleDateFormat("HH:mm:ss.SS"));
			testClassName
					.set(result.getTestClass().getRealClass().getSimpleName());

			addClassNodeToTestWave(result);

			String testName;
			testName = result.getTestName();
			if (!testName.contains("null")) currentTestNameWasSet.set(true);
			currentThreadId.set(Thread.currentThread().getId());
			currentTest.set(new TestNode(index.getAndIncrement(), testName,
					/*testName.replaceAll("[^a-zA-Z0-9]", "")*/  "test_" + generateUid()
							+ "-" + index.get()));

			testDetails.set(new TestDetails(currentTest.get().getUid()));
			currentTest.get().setParent(classNodeMap.get(testClassName.get()));
			currentTest.get().setClassName(testClassName.get());
			Date date = new Date();
			
			currentTest.get().setTimestamp(String.format("%1$tT (%1$tQ)ms", date));
			currentTest.get().setDate(DATE_FORMAT.format(date));

			if (result.getMethod().getDescription() != null) {
				currentTest.get()
						.setDescription(result.getMethod().getDescription());
			}
			updateTestDirectory();
		}
		finally {
			onTestInitLock.unlockWrite(sLock);
		}
	}

	/**
	 * Since this class contains shared-data and exclusive thread data, some of
	 * the fields like currentClassScenario for example, must be set only once,
	 * since it is a shared data, every thread will try to set a copy of this
	 * value it can't be allocated into every thread's stack, therefore it must
	 * remain shared. To overcome the ambiguity of sharedData being initialized
	 * on every thread run.
	 * 
	 * @param result
	 **/
	private void addClassNodeToTestWave(ITestResult result) {
		String className = result.getTestClass().getRealClass().getSimpleName();

		// if this class belongs to a different wave
		if (testClassNameSet.contains(className)
				&& !classNodeMap.get(className).getParent().getName()
						.equals(result.getTestContext().getName())) {
			if (!testWaveMap.containsKey(result.getTestContext().getName()))
				currentTestWave = new ScenarioNode(
						result.getTestContext().getName());

			currentClassScenario = new ScenarioNode(className);
			classNodeMap.put(testClassName.get(), currentClassScenario);
			currentTestWave.addChild(currentClassScenario);

		}
		// if this class doesn't exist in the concurrent class-hashSet
		else if (!testClassNameSet.contains(testClassName.get())) {
			testClassNameSet.add(testClassName.get());
			currentClassScenario = new ScenarioNode(className);
			classNodeMap.putIfAbsent(testClassName.get(), currentClassScenario);
			if (!testWaveMap.containsKey(result.getTestContext().getName())) {
				currentTestWave = new ScenarioNode(
						result.getTestContext().getName());

				currentTestWave.addChild(currentClassScenario);
			} else {
				testWaveMap.get(result.getTestContext().getName())
						.addChild(currentClassScenario);
			}
		}
		// if the concurrent test wave map doesn't contain this specific wave,
		// add and do the following
		if (!testWaveMap.containsKey(result.getTestContext().getName())) {

			currentMachinechildren.add(currentTestWave);
			currentMachine.addChild(currentTestWave);

			testWaveMap.put(result.getTestContext().getName(), currentTestWave);
		}
	}

	@Override
	public void onStart(ITestContext context) {

	}

	@Override
	public void onTestStart(ITestResult result) {
		long sLock = onTestStartLock.writeLock();
		try {
			currentTest.get().setDescription(result.getMethod().getDescription());
			currentTest.get()
					.setName(result.getMethod().getConstructorOrMethod()
							.getMethod().getName() + " - "
							+ result.getTestName() + " (Thread-id = "
							+ Thread.currentThread().getId() + ")");
			
			if (((RetryAnalyzer) result.getMethod()
					.getRetryAnalyzer()) != null) {
				retryTestDuration.set(System.currentTimeMillis());
				if (((RetryAnalyzer) result.getMethod().getRetryAnalyzer())
						.getMaxRetryCount() > 0) {
					if (((RetryAnalyzer) result.getMethod().getRetryAnalyzer())
							.isParentNode()) {

						currentRetryParentTest.set(
								new ScenarioNode(currentTest.get().getName()));
						String methodName = "retry -" + result.getMethod()
								.getConstructorOrMethod().getMethod().getName()
								+ " ";
						currentTest.get()
								.setName(methodName + result.getTestName());
						currentRetryParentTest.get().setParent(
								classNodeMap.get(testClassName.get()));
						currentTest.get().setDuration(0);

						Date date = new Date();
						currentTest.get().setTimestamp(String.format("%1$tT (%1$tQ)ms", date));
						currentTest.get().setDate(DATE_FORMAT.format(date));;
						retryParentTestNodeMap.putIfAbsent(
								currentRetryParentTest.get().getName(),
								currentRetryParentTest.get());
						// if (onFailLock.tryUnlockWrite()) onFailLock.wait();
						isRetryEnabled.set(true);
						onConfigurationOrOnTestFailure(result);
					} else {
						currentTest.get().setParent(retryParentTestNodeMap
								.get(currentTest.get().getName()));
					}
				} else {
					currentTest.get()
							.setParent(classNodeMap.get(testClassName.get()));
				}
			}
		}

		finally {
			onTestStartLock.unlockWrite(sLock);
		}
	}

	@Override
	public void onTestSuccess(ITestResult result) {
		onTestEnd(result);
	}

	@Override
	public void onConfigurationFailure(ITestResult result) {
		long sLock = onConfigurationFailLock.writeLock();
		
		try {
			onConfigurationOrOnTestFailure(result);
		}
		finally {
			onConfigurationFailLock.unlockWrite(sLock);
		}

	}

	/**
	 * TestNG generates 2 threads per every test, one for the BeforeMethod, 2nd
	 * for the test it self
	 * 
	 */

	@Override
	public void onConfigurationSuccess(ITestResult result) {

	}

	@Override
	public void onConfigurationSkip(ITestResult result) {

	}

	@Override
	public void onTestFailure(ITestResult result) {
		long sLock = onTestFailLock.writeLock();
		try {
			onConfigurationOrOnTestFailure(result);
		}
		finally {
			onTestFailLock.unlockWrite(sLock);
		}
	}

	@Override
	public void onTestSkipped(ITestResult result) {
		long sLock = onTestSkipLock.writeLock();
		try {
			if (result.getThrowable() != null && currentTest.get() == null) {
				initTestIterationNode(result);
				onConfigurationOrOnTestFailure(result);
			}
			if (isRetryEnabled.get()) {
				long elapsedTimeMillis = System.currentTimeMillis()
						- retryTestDuration.get();
				retryTestDuration.set(elapsedTimeMillis);
				currentTest.get().setParent(null);
				currentTest.get().setStatus(Status.warning);
				classNodeMap.get(currentTest.get().getClassName())
						.addChild(currentRetryParentTest.get());
				currentTest.set(null);
			}
		}
		finally {
			onTestSkipLock.unlock(sLock);
		}
	}

	@Override
	public void onFinish(ISuite suite) {
		currentTestWave.addScenarioProperty("Suite Name",suite.getName());
		currentTestWave.addScenarioProperty("Date", LocalDateTime.now()
			       .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));//new SimpleDateFormat("dd/mm/yyyy HH:mm:ss"));
		
		addRunProperty("Date", " dsa");
		addMachineToExecution(suite.getHost());
		writeExecution(execution);
	}

	@Override
	public void onFinish(ITestContext context) {

	}

	/**
	 * The testNG retry feature has a bug in the start & end milliseconds,
	 * retryTestDuration is the workaround.
	 * 
	 * @param result
	 */
	private void onTestEnd(ITestResult result) {
		long sLock = onEndLock.writeLock();
		try {
			//if(!validationException.get()){
			classNodeMap.get(currentTest.get().getClassName())
					.addChild(currentTest.get());
			if ((RetryAnalyzer) result.getMethod().getRetryAnalyzer() != null)
				currentTest.get().setDuration(((System.currentTimeMillis()
						- result.getStartMillis())));
			else currentTest.get().setDuration(
					((result.getEndMillis() - result.getStartMillis())));

			currentTestWave
					.setTestWaveDuration(currentTest.get().getDuration());
			currentTestWave.addScenarioProperty("duration",
					Long.toString(currentTestWave.getTestWaveDuration().get()));
			writeTestDetails(testDetails.get());
			isRetryEnabled.set(false);
			retryTestDuration.set(0);
//			}
//			else validationException.set(false);
		}
		finally {
			onEndLock.unlockWrite(sLock);
		}
	}

	protected String generateUid() {
		return String.valueOf(new Random().nextInt(1000))
				+ String.valueOf(System.currentTimeMillis() / 1000);
	}

	/**
	 * If no execution exists. Meaning, we are not appending to an older
	 * execution; A new execution would be created. If the execution is new,
	 * will add a new reported machine instance. If we are appending to an older
	 * execution, and the machine is the same as the machine the execution were
	 * executed on, will append the results to the last machine and will not
	 * create a new one.
	 * 
	 * @param context
	 * 
	 **/
	private void addMachineToExecution(String host) {
		if (null == execution) {
			execution = new Execution();
			execution.addMachine(currentMachine);
			return;
		}
		// We are going to append to existing execution
		MachineNode lastMachine = execution.getLastMachine();
		if (null == lastMachine || null == lastMachine.getName()) {
			// Something is wrong. We don't have machine in the existing
			// execution. We need to add a new one
			execution.addMachine(currentMachine);
			return;
		}
		if (!lastMachine.getName().equals(currentMachine.getName())) {
			// The execution happened on machine different from the current
			// machine, so we will create a new machine
			execution.addMachine(currentMachine);
		} else {
			currentMachine = lastMachine;
		}
	}

	/**
	 * Occurs once by the Main thread
	 * 
	 * @return
	 **/
	private static String getMachineName() {
		String machineName;
		try {
			machineName = InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException e) {
			machineName = "localhost";
		}
		return machineName;
	}

	protected abstract void writeTestDetails(TestDetails testDetails);

	protected abstract void writeExecution(Execution execution);

	/**
	 * @param result
	 * @return
	 **/

	/**
	 * Event that is called when a new scenario is created
	 * 
	 * @param scenario
	 **/
	protected abstract void onScenarioStart(ScenarioNode scenario);

	protected abstract void updateTestDirectory();

	private void onConfigurationOrOnTestFailure(ITestResult result) {
		long sLock = onFailLock.writeLock();
		try {

			if (currentTest.get() != null) {
				if (isRetryEnabled.get() == false) currentTest.get()
						.setName(result.getMethod().getConstructorOrMethod()
								.getMethod().getName() + " - "
								+ result.getTestName() + " (Thread-id = "
								+ Thread.currentThread().getId() + ")");
				if (!classNodeMap.get(currentTest.get().getClassName())
						.getStatus().equals(Status.failure)) {
					classNodeMap.get(currentTest.get().getClassName())
							.getParent().setStatus(Status.failure);
					classNodeMap.get(currentTest.get().getClassName())
							.setStatus(Status.failure);
				}
				currentTest.get().setStatus(Status.failure);
				reportLastTestException(result);
				onTestEnd(result);

			}
		}
		finally {
			onFailLock.unlockWrite(sLock);
		}
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see Design.TestBase3.Diffido.Reporter#onTestSkipped(org.testng.ITestResult)
	 * 
	 *      (getRetryAnalyzer()).getcounter() > 0) indicates that this specific
	 *      test has the retry test option enabled, this test should be
	 *      considered as failed, and also should be marked.
	 **/

	private void reportLastTestException(ITestResult result) {
		long sLock = onExceptionLock.writeLock();
		try {
			if (null == result) { return; }
			// Get the test's last exception
			final Throwable e = result.getThrowable();
			if (null == e) { return; }
			// Log the test's last unhandled exception
			String title = null;
			String message = null;
			try (StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw)) {
				e.printStackTrace(pw);
				title = ("The test ended with the following exception:");
				message = sw.toString();
			}
			catch (IOException e1) {
				title = ("The test ended with unknown exception");
			}
			if (e instanceof AssertionError) {
				log(title, message, Status.failure, ElementType.regular);
			} else {
				log(title, message, Status.failure, ElementType.regular);
			}
			result.getThrowable().printStackTrace();
		}
		finally {
			onExceptionLock.unlockWrite(sLock);
		}
	}

	/**
	 * Event for start of suite
	 * 
	 * @param suite
	 **/

	@SuppressWarnings("unused")
	@Override
	public void log(String title, String message, Status status,
			ElementType type) {
		long sLock = reporterActionLock.writeLock();
		try {
			if (type.equals(ElementType.startLevel)) {
				startLevelOcurrences.getAndIncrement();
			}
			if (currentTest.get() == null) return;
			if (status == Status.failure || status == Status.error
					|| status == Status.warning)
				currentTest.get().setStatus(status);
			if (null == testDetails.get()) return;
			ReportElement element = new ReportElement();
			element = updateTimestampAndTitle(element, title);
			element.setMessage(message);
			element.setStatus(status);
			if (null == type) {
				type = ElementType.regular;
			}
			element.setType(type);
			testDetails.get().addReportElement(element);
			if ((System.currentTimeMillis() - lastWrite.get()) > 100) {
				lastWrite.set(System.currentTimeMillis());
				if (type.equals(ElementType.stopLevel)) {
					startLevelOcurrences.getAndDecrement();
				}
				writeTestDetails(testDetails.get());
			}
		}
		finally {
			reporterActionLock.unlockWrite(sLock);
		}
	}

	private ReportElement updateTimestampAndTitle(ReportElement element,
			String title) {
		//element.setTime(TIME_FORMAT.format(new Date()));
		element.setTime(String.format("%1$tF", new Date()));
		element.setTitle(title);
		return element;
	}

	/**
	 * This method counts all the planned test cases using dataProviders for the
	 * given ITestNGMethod.
	 * 
	 * @param method
	 * @return the number of cases, returns 1 if no dataProvider is found for
	 *         the test.
	 **/

	public void addTestProperty(String name, String value) {
		if (null == testDetails) { return; }
		currentTest.get().addProperty(name, value);
	}

	/**
	 * Add free property to the whole run
	 * 
	 * @param name
	 * @param value
	 **/

	public void addRunProperty(String name, String value) {
		if (null == currentClassScenario) { return; }
		log("Adding run proprty '" + name + "'='" + value + "'", null,
				Status.success, ElementType.regular);
		
	}

	protected TestNode getCurrentTest() {
		return currentTest.get();
	}

	protected TestDetails getTestDetails() {
		return testDetails.get();
	}

	protected Execution getExecution() {
		return execution;
	}

	public void InitACustomTestNode(String testWave, String testClass,
			String testMethod, Exception e, String iterationNodeName) {
		long sLock = onTestCasesValidation.writeLock();
		try {
			if (e != null) {
				if (testCasesValidationClassSet.contains(testClass)) return;
				String iterationName = iterationNodeName;
				if (testMethod == null) {// theres a validation error on a
										 // missing testClass node, no need to
										 // create more than one custom test
										 // node
					iterationName = testClass
							+ " is missing from TestCases xml file";
					testCasesValidationClassSet.add(testClass);
				}
				testCasesXmlValidationException.set(e);
				testClassName.set(testClass);
				if (!testClassNameSet.contains(testClassName.get())) {
					testClassNameSet.add(testClassName.get());
					currentClassScenario = new ScenarioNode(
							testClassName.get());
					classNodeMap.putIfAbsent(testClassName.get(),
							currentClassScenario);
					currentTestWave = new ScenarioNode(testWave);
					currentTestWave.addChild(currentClassScenario);
				} else if (testClassNameSet.contains(testClass) && classNodeMap
						.get(testClass).getParent().getName() != testWave) {
					currentClassScenario = new ScenarioNode(testClass);
					classNodeMap.put(testClassName.get(), currentClassScenario);
					currentTestWave = new ScenarioNode(testWave);
					currentTestWave.addChild(currentClassScenario);
				}

				if (!testWaveMap.containsKey(testWave)) {
					currentMachinechildren.add(currentTestWave);
					currentMachine.addChild(currentTestWave);
					testWaveMap.put(testWave, currentTestWave);
				}

				currentThreadId.set(Thread.currentThread().getId());
				if (iterationName == null) iterationName = testMethod
						+ " TestCases.xml validation Error";
				currentTest.set(
						new TestNode(index.getAndIncrement(), iterationName,
								/*iterationName.replaceAll("[^a-zA-Z0-9]", "")*/
										 "test_" + generateUid() + "-"
										+ index.get()));

				testDetails.set(new TestDetails(currentTest.get().getUid()));
				currentTest.get()
						.setParent(classNodeMap.get(testClassName.get()));
				currentTest.get().setClassName(testClassName.get());
				Date date = new Date();
				currentTest.get().setTimestamp(String.format("%1$tT (%1$tQ)ms", date));
				currentTest.get().setDate(String.format("%1$tF", date));
				updateTestDirectory();
				log("TestCases.xml error", e.getMessage(), Status.failure,
						ElementType.regular);
				classNodeMap.get(currentTest.get().getClassName())
						.addChild(currentTest.get());
				writeTestDetails(testDetails.get());
			}
		}
		finally {
			onTestCasesValidation.unlockWrite(sLock);
			try {
				throw e;
			}
			catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

}
