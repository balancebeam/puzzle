package io.anyway.puzzle.core.aware;

import org.springframework.beans.factory.Aware;

import io.anyway.puzzle.core.servlet.metadata.ServletMetadata;

public interface ServletMetadataAware extends Aware {
	
	void setServletMetadata(ServletMetadata metadata);
}
