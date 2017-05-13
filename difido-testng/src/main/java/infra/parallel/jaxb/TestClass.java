package infra.parallel.jaxb;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class TestClass {

	private String name;

	private List<TestMethod> testmethod;

	public TestClass() {
		
	}

	public TestClass(String name, List<TestMethod> testmethod) {
		super();
		this.name = name;
		this.testmethod = testmethod;
	}

	@XmlAttribute
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement
	public List<TestMethod> getTestmethod() {
		return testmethod;
	}

	public void setTestmethod(List<TestMethod> testmethod) {
		this.testmethod = testmethod;
	}

	public List<Iteration> getIterationsByMethod(String testMethodName) {
		Optional<TestMethod> findFirst = testmethod.stream().filter(tm -> tm.getName().equals(testMethodName))
				.findFirst();
		if (!findFirst.isPresent()) {
			return new ArrayList<Iteration>();
		}
		return findFirst.get().getAllIterationsAndSetSutParent();
	}
	

	public Optional<TestMethod> getTestMethodByName(String testMethodName) {
		return testmethod.stream().filter(tm -> tm.getName().equals(testMethodName)).findFirst();
	}
}