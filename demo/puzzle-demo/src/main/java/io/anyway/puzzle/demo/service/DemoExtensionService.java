package io.anyway.puzzle.demo.service;

import org.springframework.stereotype.Service;

import io.anyway.puzzle.core.PluginContext;
import io.anyway.puzzle.core.service.PluginExtensionPointService;

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
