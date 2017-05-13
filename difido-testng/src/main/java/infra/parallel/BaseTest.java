package infra.parallel;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.testng.ITest;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import com.beust.jcommander.internal.Sets;
import binders.IReporter;
import binders.ReportManager;
import il.co.topq.difido.RetryAnalyzer;
import il.co.topq.difido.model.Enums.Status;
import infra.parallel.annotations.EnableRetry;
import infra.parallel.annotations.MandatoryParams;
import infra.parallel.jaxb.Iteration;
import infra.parallel.jaxb.Scenarios;

@Listeners(il.co.topq.difido.ReportManagerHook.class)
public class BaseTest implements ITest {

	protected ThreadLocal<String> param1 = new ThreadLocal<>();
	protected ThreadLocal<String> param2 = new ThreadLocal<>();
	protected ThreadLocal<String> param3 = new ThreadLocal<>();
	protected IReporter report = ReportManager.getInstance();
	private static Scenarios _scenarios = null;
	protected ThreadLocal<String> sutFileName = new ThreadLocal<>();

	@Override
	public String getTestName() {
		return param1.get() + "  " + param2.get();
	}

	@BeforeSuite
	public void suitePreConfigure(ITestContext context) throws Exception {
		if (_scenarios == null) {
			try {
				JAXBContext jc = JAXBContext.newInstance(Scenarios.class);
				Unmarshaller unmarshaller = jc.createUnmarshaller();
				File xml = new File(context.getSuite().getParameter("TestCasesFilePath"));
				_scenarios = (Scenarios) unmarshaller.unmarshal(xml);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@BeforeMethod()
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void setUp(Object... params) throws Exception {
		if (params.length == 0)
			throw new IndexOutOfBoundsException(
					"Test iteration entity was null, or dataProvider attribute is missing inside method annotation");

		Iteration iteration = ((Iteration) params[0]);
		sutFileName.set(iteration.getSut());
		List<Field> fields = new LinkedList<>();
		for (Class<?> c = this.getClass(); c != Object.class; c = c.getSuperclass())
			Collections.addAll(fields, c.getDeclaredFields());
		Stream.of(fields.toArray(new Field[0])).forEach(field -> {
			if (field.getType() == ThreadLocal.class && iteration.containsParameter(field.getName())) {
				try {
					field.setAccessible(true);
					Class<?> fieldTypeArg = (Class<?>) ((ParameterizedType) field.getGenericType())
							.getActualTypeArguments()[0];
					if (fieldTypeArg.isEnum()) {
						Method enumValueOf = fieldTypeArg.getMethod("valueOf", String.class);
						((ThreadLocal<Enum>) field.get(this))
								.set((Enum) enumValueOf.invoke(fieldTypeArg, iteration.getParamValue(field.getName())));
					}
					if (fieldTypeArg.isAssignableFrom(String.class))
						((ThreadLocal<String>) field.get(this)).set((String) iteration.getParamValue(field.getName()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		iteration.setTestName(getTestName());
	}

	/**
	 * This method is the first to be called from within testNG.xml execution,
	 * to this method , all existing testMethods From testng.xml will call it
	 * once, and return with their set of iterations.
	 * 
	 * Reflection verifications: - TestCases.xml contains executed test classes
	 * and test methods, if fails, posts it on the difido reporter
	 * - @MandatoryParams exists above the current test method, do the mandatory
	 * parameters match the parameters inside testCases.xml - Test-Method
	 * legible signature (Iteration iteration)
	 * 
	 * Reflection configurations: - @EnableRetry if exists, initialize the
	 * ITestNGMethod retryAnalyzer - Test method's name/description - Test
	 * method's sut/environment
	 * 
	 * Right after every method calls once the dataprovider, every iteration
	 * starts calling the @BeforeMethod.
	 * 
	 * @param ctx
	 * @param method
	 * @return
	 * @throws Exception
	 */
	@DataProvider(name = "ParallelDataProvider", parallel = true)
	public Iterator<Object[]> fileDataProvider(ITestContext ctx, ITestNGMethod method) throws Exception {

		Set<Object[]> set = Sets.newLinkedHashSet();

		String className = method.getTestClass().getRealClass().getSimpleName();
		String methodName = method.getConstructorOrMethod().getMethod().getName();
		Method testMethod = method.getConstructorOrMethod().getMethod();
		_scenarios.testMethodExistInTestCasesXML(method);
		_scenarios.testMethodSignatureValidation(method);

		MandatoryParams mandatoryParamsAnnotation = testMethod.getAnnotation(MandatoryParams.class);
		if (mandatoryParamsAnnotation == null) {
			try {
				String annotationMissingError = "\nannotation @MandatoryParams is missing" + ", above test method: "
						+ methodName + " in class: " + method.getTestClass().getRealClass();
				report.log(annotationMissingError, Status.failure);
				throw new Exception(annotationMissingError);
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}

		if (testMethod.getAnnotation(EnableRetry.class) != null)
			method.setRetryAnalyzer(new RetryAnalyzer());
		List<Iteration> iterations = _scenarios.getTestMethodIterations(methodName, className);

		Set<String> allSuts = iterations.stream().map((iter) -> iter.getSut()).collect(Collectors.toSet());
		
		method.setDescription(mandatoryParamsAnnotation.testDescription());
		iterations.stream().forEach(iteration -> {
			iteration.containsMandatoryParams(mandatoryParamsAnnotation.params(),
					method);
				set.add(new Object[] { iteration });
		});
		return set.iterator();
	}

	/**
	 * Can set the thread count to the number of existing iterations in the
	 * testCases.xml Or manually set programmatically set the
	 * data-provider-thread-count value.
	 * 
	 * @param context
	 * @param threadCount
	 * @throws Exception
	 */

	@AfterMethod()
	protected void tearDown() throws Exception {
		report.log("tear down in base test");
	}

	public void setThreadPoolCount(ITestContext context, int threadCount) throws Exception {
		if (!context.getSuite().getXmlSuite().toXml().contains("data-provider-thread-count"))
			System.err.println("<< The thread count default limit is 10 >>");
		threadCount = threadCount != 0 ? threadCount
				: _scenarios.getSuiteIterationsCount((context.getAllTestMethods()));
		context.getSuite().getXmlSuite().setDataProviderThreadCount(threadCount);
	}

}
