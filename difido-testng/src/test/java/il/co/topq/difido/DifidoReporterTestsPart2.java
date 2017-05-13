package il.co.topq.difido;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import il.co.topq.difido.model.Enums.Status;
import infra.parallel.BaseTest;
import infra.parallel.annotations.MandatoryParams;
import infra.parallel.jaxb.Iteration;

public class DifidoReporterTestsPart2 extends BaseTest {

	@BeforeMethod()
	protected void setUp(Object... params) throws Exception {
		report.startLevel("setup");
		super.setUp(params);
		report.log("Test class before method");
		report.endLevel();
	}
	
	@AfterMethod()
	protected void tearDown() throws Exception {
		report.startLevel("tearDown");
		super.tearDown();
		report.log("Test class after method in child class");
		report.endLevel();
	}

	@Test(dataProvider = "ParallelDataProvider")
	@MandatoryParams(params = { "param1" })
	public void testNotExistingInTestCases(Iteration iteration) {
		report.log("RandomTest");
	}
	
	
	@Test(dataProvider = "ParallelDataProvider")
	@MandatoryParams(params = { "param1", "param2" })
	public void testWithWrongParallelMethodSignature(String arg) {
		report.log("Should not get to this log");
	}
	
	
	@Test(dataProvider = "ParallelDataProvider")
	@MandatoryParams(params = { "param1" })
	public void testWithError(Iteration iteration) throws Exception {
		report.log("Message with error", "Error message", Status.error);
	}

	@Test(dataProvider = "ParallelDataProvider")
	@MandatoryParams(params = { "param1", "param2" })
	public void testWithEncoding(Iteration iteration) {
		report.log("Japanease Yen: \u00A5");
	}

	@Test(dataProvider = "ParallelDataProvider")
	@MandatoryParams(params = { "param1", "param2" })
	public void testWithWarning(Iteration iteration) throws Exception {
		report.log("Message with warning", "Warning message", Status.warning);
	}

	@Test(dataProvider = "ParallelDataProvider")
	@MandatoryParams(params = { "param1", "param2" }, testDescription="a test with various report log messages")
	public void testWithVariousLogMessages(Iteration iteration)
			throws Exception {
		report.step("This is the first step");
		report.startLevel("Starting level");
		report.log("Message inside level");
		report.log("This is title", "this is message");
		report.log("Message inside level", "Inside level");
		report.log("Message inside level", "Inside level");
		report.endLevel();

		report.step("This is the second step");
		report.startLevel("Level with failure");
		report.log("Something wrong happened", Status.failure);
		report.endLevel();
	}

	@Test(dataProvider = "ParallelDataProvider")
	@MandatoryParams(params = { "param1", "param2" }, testDescription="test with the file adding feature, presents in a file link")
	public void testAddFile(Iteration iteration) {
		File file = new File("pom.xml");
		report.addFile(file, "This is the file");
	}

	@Test(dataProvider = "ParallelDataProvider")
	@MandatoryParams(params = { "param1", "param2" }, testDescription="test with screenshot adding as an image link, visible as a small tab in the difido reporter")
	public void testAddScreenshot(Iteration iteration)
			throws IOException, AWTException, InterruptedException {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle screenRectangle = new Rectangle(screenSize);
		Robot robot = new Robot();
		BufferedImage image = robot.createScreenCapture(screenRectangle);
		File imgFile = File.createTempFile("screenshot_file", "png");
		ImageIO.write(image, "png", imgFile);
		Thread.sleep(1000);
		report.addImage(imgFile, "My screenshot file");
		imgFile.delete();

	}
}
