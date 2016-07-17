package io.pdf.demo.dubbo.provider.service.impl;

import org.springframework.stereotype.Service;

import io.pdf.demo.dubbo.api.domain.User;
import io.pdf.demo.dubbo.api.service.UserService;

@Service
public class UserServiceImpl implements UserService{

	@Override
	public User getUser(int id) {
		User user= new User();
		user.setId(id);
		user.setName("张三");
		return user;
	}

}
