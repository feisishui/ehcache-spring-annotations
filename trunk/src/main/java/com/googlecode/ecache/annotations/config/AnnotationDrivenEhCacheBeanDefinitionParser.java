/**
 * Copyright 2010 Nicholas Blair, Eric Dalquist
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.ecache.annotations.config;

import org.springframework.aop.config.AopNamespaceUtils;
import org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.googlecode.ecache.annotations.Cacheable;
import com.googlecode.ecache.annotations.TriggersRemove;
import com.googlecode.ecache.annotations.impl.CacheAttributeSourceImpl;
import com.googlecode.ecache.annotations.impl.CacheStaticMethodMatcherPointcut;
import com.googlecode.ecache.annotations.interceptor.EhCacheInterceptor;
import com.googlecode.ecache.annotations.key.HashCodeCacheKeyGenerator;


/**
 * {@link BeanDefinitionParser} that sets up {@link DefaultBeanFactoryPointcutAdvisor}
 * instances to wrap {@link EhCacheInterceptor}s around {@link Cacheable}s and {@link TriggersRemove}
 * advised methods.
 * 
 * @author Nicholas Blair, nblair@doit.wisc.edu
 * @version $Id$
 */
public class AnnotationDrivenEhCacheBeanDefinitionParser implements BeanDefinitionParser {

    public static final String EHCACHE_CACHING_ADVISOR_BEAN_NAME = "com.googlecode.ecache.annotations.config.internalEhCacheCachingAdvisor";
    
    public static final String DEFAULT_CACHE_KEY_GENERATOR = HashCodeCacheKeyGenerator.class.getName() + "_DEFFAULT";
    
    /* (non-Javadoc)
     * @see org.springframework.beans.factory.xml.BeanDefinitionParser#parse(org.w3c.dom.Element, org.springframework.beans.factory.xml.ParserContext)
     */
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(parserContext, element);
        if (!parserContext.getRegistry().containsBeanDefinition(EHCACHE_CACHING_ADVISOR_BEAN_NAME)) {
            Object elementSource = parserContext.extractSource(element);
            
            RootBeanDefinition cacheAttributeSource = new RootBeanDefinition(CacheAttributeSourceImpl.class);
            cacheAttributeSource.setSource(elementSource);
            cacheAttributeSource.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            cacheAttributeSource.getPropertyValues().add("cacheManagerBeanName", element.getAttribute("cache-manager"));
            cacheAttributeSource.getPropertyValues().add("createCaches", Boolean.parseBoolean(element.getAttribute("create-missing-caches")));
            String cacheableAttributeSourceBeanName = parserContext.getReaderContext().registerWithGeneratedName(cacheAttributeSource);
            RuntimeBeanReference cacheableAttributeSourceRuntimeReference = new RuntimeBeanReference(cacheableAttributeSourceBeanName);
            
            RootBeanDefinition cacheablePointcutSource = new RootBeanDefinition(CacheStaticMethodMatcherPointcut.class);
            cacheablePointcutSource.setSource(elementSource);
            cacheablePointcutSource.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            cacheablePointcutSource.getPropertyValues().add("cacheAttributeSource", cacheableAttributeSourceRuntimeReference);
            String cacheablePointcutBeanName = parserContext.getReaderContext().registerWithGeneratedName(cacheablePointcutSource);
            
            RootBeanDefinition cachingInterceptorSource = new RootBeanDefinition(EhCacheInterceptor.class);
            cachingInterceptorSource.setSource(elementSource);
            cachingInterceptorSource.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            cachingInterceptorSource.getPropertyValues().add("cacheAttributeSource", cacheableAttributeSourceRuntimeReference);
            String cachingInterceptorBeanName = parserContext.getReaderContext().registerWithGeneratedName(cachingInterceptorSource);
            
            
            RootBeanDefinition cachingPointcutAdvisorSource = new RootBeanDefinition(DefaultBeanFactoryPointcutAdvisor.class);
            cachingPointcutAdvisorSource.setSource(elementSource);
            cachingPointcutAdvisorSource.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            cachingPointcutAdvisorSource.getPropertyValues().add("adviceBeanName", cachingInterceptorBeanName);
            cachingPointcutAdvisorSource.getPropertyValues().add("pointcut", new RuntimeBeanReference(cacheablePointcutBeanName));
            parserContext.getRegistry().registerBeanDefinition(EHCACHE_CACHING_ADVISOR_BEAN_NAME, cachingPointcutAdvisorSource);
            
            
            final RootBeanDefinition defaultKeyGenerator = new RootBeanDefinition(HashCodeCacheKeyGenerator.class);
            defaultKeyGenerator.setSource(elementSource);
            defaultKeyGenerator.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            parserContext.getRegistry().registerBeanDefinition(DEFAULT_CACHE_KEY_GENERATOR, defaultKeyGenerator);
           
        }
        return null;
    }

}
