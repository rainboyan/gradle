/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.plugins.jvm.internal;

import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.FileCollectionDependency;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.dsl.DependencyProvider;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.artifacts.dsl.dependencies.DependencyFactoryInternal;
import org.gradle.api.internal.catalog.DependencyBundleValueSource;
import org.gradle.api.internal.provider.DefaultValueSourceProviderFactory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.jvm.JvmComponentDependencies;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderConvertible;
import org.gradle.api.provider.ValueSource;
import org.gradle.internal.Cast;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultJvmComponentDependencies implements JvmComponentDependencies {
    private final Configuration implementation;
    private final Configuration compileOnly;
    private final Configuration runtimeOnly;
    private final Configuration annotationProcessor;

    @Inject
    public DefaultJvmComponentDependencies(Configuration implementation, Configuration compileOnly, Configuration runtimeOnly, Configuration annotationProcessor) {
        this.implementation = implementation;
        this.compileOnly = compileOnly;
        this.runtimeOnly = runtimeOnly;
        this.annotationProcessor = annotationProcessor;
    }

    @Inject
    protected DependencyFactoryInternal getDependencyFactory() {
        throw new UnsupportedOperationException();
    }

    @Inject
    protected ObjectFactory getObjectFactory() {
        throw new UnsupportedOperationException();
    }
    @Override
    public void implementation(String dependencyNotation) {
        doAddEager(implementation, getDependencyFactory().fromCharSequence(dependencyNotation), null);
    }

    @Override
    public void implementation(String dependencyNotation, Action<? super ExternalModuleDependency> configuration) {
        doAddEager(implementation, getDependencyFactory().fromCharSequence(dependencyNotation), dependency -> configuration.execute((ExternalModuleDependency) dependency));
    }

    @Override
    public void implementation(Map<String, ?> dependencyNotation) {
        doAddEager(implementation, getDependencyFactory().fromMap(dependencyNotation), null);
    }

    @Override
    public void implementation(Map<String, ?> dependencyNotation, Action<? super ExternalModuleDependency> configuration) {
        doAddEager(implementation, getDependencyFactory().fromMap(dependencyNotation), dependency -> configuration.execute((ExternalModuleDependency) dependency));
    }

    @Override
    public void implementation(Project dependencyNotation) {
        doAddEager(implementation, getDependencyFactory().fromProject(dependencyNotation), null);
    }

    @Override
    public void implementation(Project dependencyNotation, Action<? super ProjectDependency> configuration) {
        doAddEager(implementation, getDependencyFactory().fromProject(dependencyNotation), dependency -> configuration.execute((ProjectDependency) dependency));
    }

    @Override
    public void implementation(FileCollection dependencyNotation) {
        doAddEager(implementation, getDependencyFactory().fromFileCollection(dependencyNotation), null);
    }

    @Override
    public void implementation(FileCollection dependencyNotation, Action<? super FileCollectionDependency> configuration) {
        doAddEager(implementation, getDependencyFactory().fromFileCollection(dependencyNotation), dependency -> configuration.execute((FileCollectionDependency) dependency));
    }

    @Override
    public void implementation(Provider<? extends MinimalExternalModuleDependency> dependencyNotation) {
        doAddLazy(implementation, getDependencyFactory().fromMinimal(dependencyNotation), null);
    }

    @Override
    public void implementation(Provider<? extends MinimalExternalModuleDependency> dependencyNotation, Action<? super ExternalModuleDependency> configuration) {
        doAddLazy(implementation, getDependencyFactory().fromMinimal(dependencyNotation), dependency -> configuration.execute((ExternalModuleDependency) dependency));
    }

    @Override
    public void implementation(ProviderConvertible<? extends MinimalExternalModuleDependency> dependencyNotation) {
        implementation(dependencyNotation.asProvider());
    }

    @Override
    public void implementation(ProviderConvertible<? extends MinimalExternalModuleDependency> dependencyNotation, Action<? super ExternalModuleDependency> configuration) {
        implementation(dependencyNotation.asProvider(), configuration);
    }

    @Override
    public void implementation(Dependency dependencyNotation) {
        doAddEager(implementation, dependencyNotation, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <D extends Dependency> void implementation(D dependencyNotation, Action<? super D> configuration) {
        doAddEager(implementation, dependencyNotation, dependency -> configuration.execute((D) dependency));
    }

    @Override
    public void implementation(DependencyProvider<?> dependencyNotation) {
        doAddLazy(implementation, dependencyNotation, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <D extends Dependency> void implementation(DependencyProvider<? extends D> dependencyNotation, Action<? super D> configuration) {
        doAddLazy(implementation, dependencyNotation, dependency -> configuration.execute((D) dependency));
    }

    @Override
    public void runtimeOnly(Object dependency) {
        runtimeOnly(dependency, null);
    }

    @Override
    public void runtimeOnly(Object dependency, @Nullable Action<? super Dependency> configuration) {
        doAdd(runtimeOnly, dependency, configuration);
    }

    @Override
    public void compileOnly(Object dependency) {
        compileOnly(dependency, null);
    }

    @Override
    public void compileOnly(Object dependency, @Nullable Action<? super Dependency> configuration) {
        doAdd(compileOnly, dependency, configuration);
    }

    @Override
    public void annotationProcessor(Object dependency) {
        annotationProcessor(dependency, null);
    }

    @Override
    public void annotationProcessor(Object dependency, @Nullable Action<? super Dependency> configuration) {
        doAdd(annotationProcessor, dependency, configuration);
    }

    private void doAdd(Configuration bucket, Object dependency, @Nullable Action<? super Dependency> configuration) {
        if (dependency instanceof ProviderConvertible<?>) {
            doAddLazy(bucket, ((ProviderConvertible<?>) dependency).asProvider(), configuration);
        } else if (dependency instanceof Provider<?>) {
            doAddLazy(bucket, (Provider<?>) dependency, configuration);
        } else {
            doAddEager(bucket, dependency, configuration);
        }
    }

    private void doAddEager(Configuration bucket, Object dependency, @Nullable Action<? super Dependency> configuration) {
        Dependency created = create(dependency, configuration);
        bucket.getDependencies().add(created);
    }

    private void doAddLazy(Configuration bucket, Provider<?> dependencyProvider, @Nullable Action<? super Dependency> configuration) {
        if (dependencyProvider instanceof DefaultValueSourceProviderFactory.ValueSourceProvider) {
            Class<? extends ValueSource<?, ?>> valueSourceType = ((DefaultValueSourceProviderFactory.ValueSourceProvider<?, ?>) dependencyProvider).getValueSourceType();
            if (valueSourceType.isAssignableFrom(DependencyBundleValueSource.class)) {
                doAddListProvider(bucket, dependencyProvider, configuration);
                return;
            }
        }
        Provider<Dependency> lazyDependency = dependencyProvider.map(mapDependencyProvider(bucket, configuration));
        bucket.getDependencies().addLater(lazyDependency);
    }

    private void doAddListProvider(Configuration bucket, Provider<?> dependency, @Nullable Action<? super Dependency> configuration) {
        // workaround for the fact that mapping to a list will not create a `CollectionProviderInternal`
        final ListProperty<Dependency> dependencies = getObjectFactory().listProperty(Dependency.class);
        dependencies.set(dependency.map(notation -> {
            List<MinimalExternalModuleDependency> deps = Cast.uncheckedCast(notation);
            return deps.stream().map(d -> create(d, configuration)).collect(Collectors.toList());
        }));
        bucket.getDependencies().addAllLater(dependencies);
    }

    private <T> Transformer<Dependency, T> mapDependencyProvider(Configuration bucket, @Nullable Action<? super Dependency> configuration) {
        return lazyNotation -> {
            if (lazyNotation instanceof Configuration) {
                throw new InvalidUserDataException("Adding a configuration as a dependency using a provider isn't supported. You should call " + bucket.getName() + ".extendsFrom(" + ((Configuration) lazyNotation).getName() + ") instead");
            }
            return create(lazyNotation, configuration);
        };
    }

    private Dependency create(Object dependency, @Nullable Action<? super Dependency> configuration) {
        final Dependency created = getDependencyFactory().createDependency(dependency);
        if (configuration != null) {
            configuration.execute(created);
        }
        return created;
    }
}
