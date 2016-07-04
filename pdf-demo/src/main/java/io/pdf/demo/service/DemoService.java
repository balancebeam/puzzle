package io.pdf.demo.service;

import io.pdf.core.aware.PluginServiceAware;
import io.pdf.demo.domain.DemoEntity;

public interface DemoService extends PluginServiceAware{

	DemoEntity getDemoEntity(int id);
}
