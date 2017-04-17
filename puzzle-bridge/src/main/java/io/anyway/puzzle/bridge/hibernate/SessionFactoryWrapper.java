package io.anyway.puzzle.bridge.hibernate;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.Reference;

import org.hibernate.Cache;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.TypeHelper;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.UpdateTimestampsCache;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Settings;
import org.hibernate.context.internal.JTASessionContext;
import org.hibernate.context.internal.ManagedSessionContext;
import org.hibernate.context.internal.ThreadLocalSessionContext;
import org.hibernate.context.spi.CurrentSessionContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.query.spi.QueryPlanCache;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.NamedQueryRepository;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.TypeResolver;

/**
 * Hibernate Wrapper
 * 
 * @author yangzz
 *
 */
@SuppressWarnings({ "rawtypes", "serial","unchecked" })
public class SessionFactoryWrapper implements SessionFactory,
		SessionFactoryImplementor {
	
	private transient CurrentSessionContext currentSessionContext;

	private SessionFactoryImpl delegate;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.delegate = (SessionFactoryImpl) sessionFactory;
		synchronized(this){
			if(currentSessionContext==null){
				currentSessionContext= buildCurrentSessionContext();
			}
		}
	}

	public SessionFactory getSessionFactory() {
		return delegate;
	}

	public Reference getReference() throws NamingException {
		return delegate.getReference();
	}

	public Session openSession(Connection connection) {
		return delegate.openSession();
	}

	public Session openSession() throws HibernateException {
		return delegate.openSession();
	}

	@Override
	public Session getCurrentSession() throws HibernateException {
		if ( currentSessionContext == null ) {
			throw new HibernateException( "No CurrentSessionContext configured!" );
		}
		return currentSessionContext.currentSession();
	}

	public ClassMetadata getClassMetadata(Class persistentClass)
			throws HibernateException {
		return delegate.getClassMetadata(persistentClass);
	}

	public ClassMetadata getClassMetadata(String entityName)
			throws HibernateException {
		return delegate.getClassMetadata(entityName);
	}

	public CollectionMetadata getCollectionMetadata(String roleName)
			throws HibernateException {
		return delegate.getCollectionMetadata(roleName);
	}

	public Map getAllClassMetadata() throws HibernateException {
		return delegate.getAllClassMetadata();
	}

	public Map getAllCollectionMetadata() throws HibernateException {
		return delegate.getAllCollectionMetadata();
	}

	public Statistics getStatistics() {
		return delegate.getStatistics();
	}

	public void close() throws HibernateException {
		delegate.close();
	}

	public boolean isClosed() {
		return delegate.isClosed();
	}

	public void evict(Class persistentClass) throws HibernateException {
		delegate.evict(persistentClass);
	}

	public void evict(Class persistentClass, Serializable id)
			throws HibernateException {
		delegate.evict(persistentClass, id);
	}

	public void evictEntity(String entityName) throws HibernateException {
		delegate.evictEntity(entityName);
	}

	public void evictEntity(String entityName, Serializable id)
			throws HibernateException {
		delegate.evictEntity(entityName, id);
	}

	public void evictCollection(String roleName) throws HibernateException {
		delegate.evictCollection(roleName);
	}

	public void evictCollection(String roleName, Serializable id)
			throws HibernateException {
		delegate.evictCollection(roleName, id); 
	}

	public void evictQueries() throws HibernateException {
		delegate.evictQueries();
	}

	public void evictQueries(String cacheRegion) throws HibernateException {
		delegate.evictQueries(cacheRegion);
	}

	public StatelessSession openStatelessSession() {
		return delegate.openStatelessSession();
	}

	public StatelessSession openStatelessSession(Connection connection) {
		return delegate.openStatelessSession(connection);
	}

	public Set<String> getDefinedFilterNames() {
		return delegate.getDefinedFilterNames();
	}

	public FilterDefinition getFilterDefinition(String filterName)
			throws HibernateException {
		return delegate.getFilterDefinition(filterName);
	}

	public Type getIdentifierType(String className) throws MappingException {
		return delegate.getIdentifierType(className);
	}

	public String getIdentifierPropertyName(String className)
			throws MappingException {
		return delegate.getIdentifierPropertyName(className);
	}

	public Type getReferencedPropertyType(String className, String propertyName)
			throws MappingException {
		return delegate.getReferencedPropertyType(className, propertyName);
	}

	public EntityPersister getEntityPersister(String entityName)
			throws MappingException {
		return delegate.getEntityPersister(entityName);
	}

	public CollectionPersister getCollectionPersister(String role)
			throws MappingException {
		return delegate.getCollectionPersister(role);
	}

	public Dialect getDialect() {
		return delegate.getDialect();
	}

	public Interceptor getInterceptor() {
		return delegate.getInterceptor();
	}

	public QueryPlanCache getQueryPlanCache() {
		return delegate.getQueryPlanCache();
	}

	public Type[] getReturnTypes(String queryString) throws HibernateException {
		return delegate.getReturnTypes(queryString);
	}

	public String[] getReturnAliases(String queryString)
			throws HibernateException {
		return delegate.getReturnAliases(queryString);
	}

	public ConnectionProvider getConnectionProvider() {
		return delegate.getConnectionProvider();
	}

	public String[] getImplementors(String className) throws MappingException {
		return delegate.getImplementors(className);
	}

	public String getImportedClassName(String name) {
		return delegate.getImportedClassName(name);
	}

	public QueryCache getQueryCache() {
		return delegate.getQueryCache();
	}

	public QueryCache getQueryCache(String regionName)
			throws HibernateException {
		return delegate.getQueryCache(regionName);
	}

	public UpdateTimestampsCache getUpdateTimestampsCache() {
		return delegate.getUpdateTimestampsCache();
	}

	public StatisticsImplementor getStatisticsImplementor() {
		return delegate.getStatisticsImplementor();
	}

	public NamedQueryDefinition getNamedQuery(String queryName) {
		return delegate.getNamedQuery(queryName);
	}

	public NamedSQLQueryDefinition getNamedSQLQuery(String queryName) {
		return delegate.getNamedSQLQuery(queryName);
	}

	public ResultSetMappingDefinition getResultSetMapping(String name) {
		return delegate.getResultSetMapping(name);
	}

	public IdentifierGenerator getIdentifierGenerator(String rootEntityName) {
		return delegate.getIdentifierGenerator(rootEntityName);
	}

	public Map getAllSecondLevelCacheRegions() {
		return delegate.getAllSecondLevelCacheRegions();
	}

	public SQLExceptionConverter getSQLExceptionConverter() {
		return delegate.getSQLExceptionConverter();
	}

	public Settings getSettings() {
		return delegate.getSettings();
	}

	public Session openTemporarySession() throws HibernateException {
		return delegate.openTemporarySession();
	}

	public Set getCollectionRolesByEntityParticipant(String entityName) {
		return delegate.getCollectionRolesByEntityParticipant(entityName);
	}

	public EntityNotFoundDelegate getEntityNotFoundDelegate() {
		return delegate.getEntityNotFoundDelegate();
	}

	public SQLFunctionRegistry getSqlFunctionRegistry() {
		return delegate.getSqlFunctionRegistry();
	}

	public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
		return delegate.getIdentifierGeneratorFactory();
	}

	public SessionBuilderImplementor withOptions() {
		return delegate.withOptions();
	}

	public TypeResolver getTypeResolver() {
		return delegate.getTypeResolver();
	}

	public Properties getProperties() {
		return delegate.getProperties();
	}

	public Map<String, EntityPersister> getEntityPersisters() {
		return delegate.getEntityPersisters();
	}

	public Map<String, CollectionPersister> getCollectionPersisters() {
		return delegate.getCollectionPersisters();
	}

	public JdbcServices getJdbcServices() {
		return delegate.getJdbcServices();
	}

	public void registerNamedQueryDefinition(String name,
			NamedQueryDefinition definition) {
		delegate.registerNamedQueryDefinition(name, definition);
	}

	public void registerNamedSQLQueryDefinition(String name,
			NamedSQLQueryDefinition definition) {
		delegate.registerNamedSQLQueryDefinition(name, definition);
	}

	public Region getSecondLevelCacheRegion(String regionName) {
		return delegate.getSecondLevelCacheRegion(regionName);
	}

	public Region getNaturalIdCacheRegion(String regionName) {
		return delegate.getNaturalIdCacheRegion(regionName);
	}

	public SqlExceptionHelper getSQLExceptionHelper() {
		return delegate.getSQLExceptionHelper();
	}

	public FetchProfile getFetchProfile(String name) {
		return delegate.getFetchProfile(name);
	}

	public ServiceRegistryImplementor getServiceRegistry() {
		return delegate.getServiceRegistry();
	}

	public void addObserver(SessionFactoryObserver observer) {
		delegate.addObserver(observer);
	}

	public CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy() {
		return delegate.getCustomEntityDirtinessStrategy();
	}

	public CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver() {
		return delegate.getCurrentTenantIdentifierResolver();
	}

	public NamedQueryRepository getNamedQueryRepository() {
		return delegate.getNamedQueryRepository();
	}

	public Iterable<EntityNameResolver> iterateEntityNameResolvers() {
		return delegate.iterateEntityNameResolvers();
	}

	public SessionFactoryOptions getSessionFactoryOptions() {
		return delegate.getSessionFactoryOptions();
	}

	public StatelessSessionBuilder withStatelessOptions() {
		return delegate.withStatelessOptions();
	}

	public Cache getCache() {
		return delegate.getCache();
	}

	public boolean containsFetchProfileDefinition(String name) {
		return delegate.containsFetchProfileDefinition(name);
	}

	public TypeHelper getTypeHelper() {
		return delegate.getTypeHelper();
	}
	
	private boolean canAccessTransactionManager() {
		try {
			return delegate.getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager() != null;
		}
		catch (Exception e) {
			return false;
		}
	}
	private TransactionFactory transactionFactory() {
		return delegate.getServiceRegistry().getService( TransactionFactory.class );
	}
	
	private CurrentSessionContext buildCurrentSessionContext() {
		String impl = getProperties().getProperty( Environment.CURRENT_SESSION_CONTEXT_CLASS );
		// for backward-compatibility
		if ( impl == null ) {
			if ( canAccessTransactionManager() ) {
				impl = "jta";
			}
			else {
				return null;
			}
		}

		if ( "jta".equals( impl ) ) {
			if ( ! transactionFactory().compatibleWithJtaSynchronization() ) {
				//LOG.autoFlushWillNotWork();
			}
			return new JTASessionContext( this );
		}
		else if ( "thread".equals( impl ) ) {
			return new ThreadLocalSessionContext( this );
		}
		else if ( "managed".equals( impl ) ) {
			return new ManagedSessionContext( this );
		}
		else {
			try {
				Class implClass = Class.forName( impl );
				return ( CurrentSessionContext ) implClass
						.getConstructor( new Class[] { SessionFactoryImplementor.class } )
						.newInstance( this );
			}
			catch( Throwable t ) {
				//LOG.unableToConstructCurrentSessionContext( impl, t );
				return null;
			}
		}
	}

}
