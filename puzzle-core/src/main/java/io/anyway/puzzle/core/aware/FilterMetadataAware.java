package io.anyway.puzzle.core.aware;

import io.anyway.puzzle.core.servlet.metadata.FilterMetadata;
import org.springframework.beans.factory.Aware;

public interface FilterMetadataAware extends Aware{
	
	void setFilterMetadata(FilterMetadata metadata);
}
