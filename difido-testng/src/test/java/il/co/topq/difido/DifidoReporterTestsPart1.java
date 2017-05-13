package il.co.topq.difido;

import il.co.topq.difido.model.Enums.Status;
import infra.parallel.BaseTest;
import infra.parallel.annotations.EnableRetry;
import infra.parallel.annotations.MandatoryParams;
import infra.parallel.jaxb.Iteration;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Add description to every test split into classes create waves
 * 
 * @author bm738
 *
 */
public class DifidoReporterTestsPart1 extends BaseTest {

	@BeforeMethod()
	protected void setUp(Object... params) throws Exception {
		report.startLevel("setUp");
		super.setUp(params);
		if (param1.get() != null) {
			if (param1.get().equals("failOnBeforeMethod"))
				throw new Exception("My failure at before method");
		}
		report.log("Test class before method");
		report.endLevel();
	}

	@AfterMethod()
	protected void tearDown() throws Exception {
		super.tearDown();
		report.startLevel("tearDown");
		if (param2.get() == "failOnAfterMethod")
			throw new Exception("My failure at tear down");
		report.log("Test class before method");
		report.endLevel();

	}

	@Test(dataProvider = "ParallelDataProvider")
	@MandatoryParams(params = { "param1",
			"param2" }, testDescription = "simple report call")
	public void simpleReportCall0(Iteration iteration)
			throws InterruptedException {
		report.log("some title0", "Some message", Status.success);
		report.log("print parameter1", param1.get());
		report.log("print parameter2", param2.get());
		report.log("print sut parameter", sutFileName.get());
		Thread.sleep(1000);
	}

	@Test(dataProvider = "ParallelDataProvider")
	@MandatoryParams(params = { "param1",
			"param2" }, testDescription = "test demonstrating the parallel feature")
	public void parallelTest(Iteration iteration) {
		report.log("some title0", "Some message", Status.success);
		report.log("print parameter1", param1.get());
		report.log("print parameter2", param2.get());
		report.log("print sut parameter", sutFileName.get());
	}

	@Test(dataProvider = "ParallelDataProvider")
	@MandatoryParams(params = { "param1, param2" }, testDescription = "test demonstrating mandatory params feature, auto failing a test")
	public void testWithInsufficientMandatoryParams(Iteration iteration) {
		report.log("Shouldnt get to this log", "Some message", Status.success);
	}

	@Test(dataProvider = "ParallelDataProvider")
	@MandatoryParams(params = {}, testDescription = "test with no parameters")
	public void testWithNoParameters(Iteration iteration) {
		report.log("some title0", "Some message", Status.success);
		report.log("Sut is a mandatory param as well, it's by default a must parameter inside the testCases.xml");
		report.log("sut", sutFileName.get());
	}

	@Test(dataProvider = "ParallelDataProvider")
	@MandatoryParams(params = {"param1" }, testDescription = "simple test with a report failure")
	public void testWithFailure(Iteration iteration) throws Exception {
		report.log("About to fail");
		throw new Exception("This is my failure");
	}

	@Test(dataProvider = "ParallelDataProvider")
	@MandatoryParams(params = {"param1" }, testDescription = "test with failure on the TestNG before method")
	public void testFailOnBeforeMethod(Iteration iteration) {
		report.log("will not get to this log", "Some message", Status.success);
	}

//	AtomicInteger tempRetryAttempts = new AtomicInteger(0);
//
//	@EnableRetry
//	@Test(dataProvider = "ParallelDataProvider", retryAnalyzer = RetryAnalyzer.class)
//	@MandatoryParams(params = { "param1",
//			"param2" }, testDescription = "test with the TestNG retry feature , fails at the first iteration ")
//	public void testWithRetryFailure(Iteration iteration) throws Exception {
//		if (tempRetryAttempts.get() == 0) {
//			try {
//				report.log("About to fail");
//				throw new Exception("This is my failure");
//			}
//			finally {
//				tempRetryAttempts.getAndIncrement();
//			}
//		}
//	}

	@Test(dataProvider = "ParallelDataProvider")
	@MandatoryParams(params = { "param1", "param2",
			"param3" }, testDescription = "simple test with 3 parameters")
	public void anotherRandomTest(Iteration iteration) {
		report.startLevel("test start level");
		report.log("RandomTest");
		report.log("parameter1:", param1.get());
		report.log("parameter2:", param2.get());
		report.log("parameter1:", param3.get());
		report.log("sut:", sutFileName.get());
		report.endLevel();
	}

}
