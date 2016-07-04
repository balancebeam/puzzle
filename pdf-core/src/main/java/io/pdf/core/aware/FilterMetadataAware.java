package io.pdf.core.aware;

import org.springframework.beans.factory.Aware;

import io.pdf.core.servlet.metadata.FilterMetadata;

public interface FilterMetadataAware extends Aware{
	
	void setFilterMetadata(FilterMetadata metadata);
}
