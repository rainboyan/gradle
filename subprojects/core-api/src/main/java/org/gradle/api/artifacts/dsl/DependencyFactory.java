/*
 * Copyright 2022 the original author or authors.
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

package org.gradle.api.artifacts.dsl;

import org.gradle.api.NonExtensible;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.FileCollectionDependency;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderConvertible;
import org.gradle.internal.HasInternalProtocol;

import java.util.Map;

/**
 * Factory class for creating {@link Dependency} instances, with strong typing.
 *
 * <p>
 * <b>Note:</b> This interface is not intended for implementation by build script or plugin authors.
 * </p>
 */
@HasInternalProtocol
@NonExtensible
public interface DependencyFactory {
    ExternalModuleDependency fromCharSequence(CharSequence dependencyNotation);

    default DependencyProvider<ExternalModuleDependency> fromCharSequence(Provider<? extends CharSequence> dependencyNotation) {
        return fromDependency(dependencyNotation.map(this::fromCharSequence));
    }

    default DependencyProvider<ExternalModuleDependency> fromCharSequence(ProviderConvertible<? extends CharSequence> dependencyNotation) {
        return fromCharSequence(dependencyNotation.asProvider());
    }

    ExternalModuleDependency fromMinimal(MinimalExternalModuleDependency dependencyNotation);

    default DependencyProvider<ExternalModuleDependency> fromMinimal(Provider<? extends MinimalExternalModuleDependency> dependencyNotation) {
        return fromDependency(dependencyNotation.map(this::fromMinimal));
    }

    default DependencyProvider<ExternalModuleDependency> fromMinimal(ProviderConvertible<? extends MinimalExternalModuleDependency> dependencyNotation) {
        return fromMinimal(dependencyNotation.asProvider());
    }

    ExternalModuleDependency fromMap(Map<String, ?> dependencyNotation);

    default DependencyProvider<ExternalModuleDependency> fromMap(Provider<? extends Map<String, ?>> dependencyNotation) {
        return fromDependency(dependencyNotation.map(this::fromMap));
    }

    default DependencyProvider<ExternalModuleDependency> fromMap(ProviderConvertible<? extends Map<String, ?>> dependencyNotation) {
        return fromMap(dependencyNotation.asProvider());
    }

    // Perhaps this should just be fromFiles(Object...)?
    FileCollectionDependency fromFileCollection(FileCollection dependencyNotation);

    // Do we need these? FileCollection is already lazy....
    // If someone needed to calculate one lazily, ObjectFactory.getFileCollection().from(...) works fine!
    default DependencyProvider<FileCollectionDependency> fromFileCollection(Provider<? extends FileCollection> dependencyNotation) {
        return fromDependency(dependencyNotation.map(this::fromFileCollection));
    }

    default DependencyProvider<FileCollectionDependency> fromFileCollection(ProviderConvertible<? extends FileCollection> dependencyNotation) {
        return fromFileCollection(dependencyNotation.asProvider());
    }

    ProjectDependency fromProject(Project dependencyNotation);

    default DependencyProvider<ProjectDependency> fromProject(Provider<? extends Project> dependencyNotation) {
        return fromDependency(dependencyNotation.map(this::fromProject));
    }

    default DependencyProvider<ProjectDependency> fromProject(ProviderConvertible<? extends Project> dependencyNotation) {
        return fromProject(dependencyNotation.asProvider());
    }

    // No point in having fromDependency(D)... it's just the identity.

    // We need this to allow narrowing arbitrary providers to DependencyProvider.
    // It creates a lazy provider, so the value will also be cached.
    <D extends Dependency> DependencyProvider<D> fromDependency(Provider<D> dependencyNotation);

    default <D extends Dependency> DependencyProvider<D> fromDependency(ProviderConvertible<D> dependencyNotation) {
        return fromDependency(dependencyNotation.asProvider());
    }
}
