package io.anyway.puzzle.demo.service.impl;

import org.springframework.stereotype.Service;

import io.anyway.puzzle.core.event.PluginFrameworkListener;
import io.anyway.puzzle.demo.domain.DemoEntity;
import io.anyway.puzzle.demo.service.DemoService;

@Service
public class DemoServiceImpl implements DemoService,PluginFrameworkListener{

	@Override
	public DemoEntity getDemoEntity(int id) {
		return new DemoEntity(id,"myname");
	}

	@Override
	public void onFinish() {
		System.out.println("-----------------PluginFrameworkListener------------------");
	}
	
}
