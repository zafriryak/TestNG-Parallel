package il.co.topq.difido;

import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;

/**
 * Default implementation of IHookable.
 * Once a test implements this interface, any actual test methods will not be called, but rather will be sent as the "callback" parameter.
 * This enables:
 * 1. Failing test from setUp\teardown
 * 2. Avoid the mess of trying to identify the actual method in the configuration methods (@BeforeMethod, @AfterMethod)
 *
 * @author wertha1
 * @implNote - This behavior is colliding with @BeforeMethod \ @AfterMethod. Using
 * @see org.testng.IHookable
 */
public interface IHookableTest extends IHookable {
	@Override
	public default void run(IHookCallBack callBack, ITestResult testResult) {
		ReportDispatcher report = ReportManager.getInstance();
		runSetup(callBack, report);
		callBack.runTestMethod(testResult);
		runTeardown(report);

	}

	default void runTeardown(ReportDispatcher report) {
		try {
			report.startLevel("Teardown");
			tearDown();
		} finally {
			report.endLevel();
		}
	}

	default void runSetup(IHookCallBack callBack, ReportDispatcher report) {
		try {
			report.startLevel("Setup");
			setUp(callBack.getParameters());
		} finally {
			report.endLevel();
		}
	}

	void tearDown();

	void setUp(Object... parameters);
}
