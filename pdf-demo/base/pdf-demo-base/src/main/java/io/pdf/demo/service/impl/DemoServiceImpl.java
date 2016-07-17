package io.pdf.demo.service.impl;

import org.springframework.stereotype.Service;

import io.pdf.core.event.PluginFrameworkListener;
import io.pdf.demo.domain.DemoEntity;
import io.pdf.demo.service.DemoService;

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
