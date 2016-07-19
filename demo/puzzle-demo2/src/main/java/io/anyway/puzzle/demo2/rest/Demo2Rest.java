package io.anyway.puzzle.demo2.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.anyway.puzzle.demo.domain.DemoEntity;
import io.anyway.puzzle.demo.service.DemoService;

@RestController
@RequestMapping(value = "")
public class Demo2Rest {

	public Demo2Rest(){
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
