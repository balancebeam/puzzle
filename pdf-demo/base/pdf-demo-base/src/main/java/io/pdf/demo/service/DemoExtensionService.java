package io.pdf.demo.service;

import org.springframework.stereotype.Service;

import io.pdf.core.PluginContext;
import io.pdf.core.service.PluginExtensionPointService;

@Service
public class DemoExtensionService extends PluginExtensionPointService{

	@Override
	public void register(PluginContext ctx) {
		System.out.println("extension demo service");
	}

	@Override
	public void unregister(PluginContext ctx) {
		
	}

}
