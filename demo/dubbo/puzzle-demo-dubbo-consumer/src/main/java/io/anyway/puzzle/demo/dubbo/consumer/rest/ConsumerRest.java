package io.anyway.puzzle.demo.dubbo.consumer.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.anyway.puzzle.demo.dubbo.api.domain.User;
import io.anyway.puzzle.demo.dubbo.api.service.UserService;

@RestController
@RequestMapping(value = "")
public class ConsumerRest {
	
	@Autowired
	private UserService userService;
	
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public User test(@PathVariable Integer id){
		return userService.getUser(id);
	}
}
