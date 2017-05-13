package infra.parallel.jaxb;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;


public class Sut {

	private String name;

	private List<Iteration> iteration;

	@XmlElement
	public List<Iteration> getIteration() {
		return iteration;
	}

	public void setIteration(List<Iteration> iteration) {
		this.iteration = iteration;
	}

	@XmlAttribute
	public String getName(){
		return name;
	}

	public void setName(String machineName) {
		this.name = machineName;
	}
	
	public int getIterationsCount(){
		return (int) iteration.stream().count();
	}

}
