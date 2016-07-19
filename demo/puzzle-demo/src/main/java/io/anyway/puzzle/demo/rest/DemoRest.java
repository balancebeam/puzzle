package io.anyway.puzzle.demo.rest;

import io.anyway.puzzle.demo.domain.DemoEntity;
import io.anyway.puzzle.demo.service.DemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "")
public class DemoRest {

	public DemoRest(){
		System.err.println("init rest demo");
	}
	
	@Autowired
	private DemoService service;
	
	@RequestMapping(value = "/aa/{id}", method = RequestMethod.GET)
	public DemoEntity test(@PathVariable Integer id){
		System.out.println("accept id: "+id);
		return service.getDemoEntity(id);
	}
}
