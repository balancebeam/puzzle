package io.anyway.puzzle.demo.service;

import io.anyway.puzzle.core.aware.PluginServiceAware;
import io.anyway.puzzle.demo.domain.DemoEntity;

public interface DemoService extends PluginServiceAware{

	DemoEntity getDemoEntity(int id);
}
