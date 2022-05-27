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

package org.gradle.api.plugins.jvm;

import org.gradle.api.Action;
import org.gradle.api.Incubating;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.FileCollectionDependency;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.dsl.DependencyFactory;
import org.gradle.api.artifacts.dsl.DependencyProvider;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderConvertible;

import java.util.Map;

/**
 * This DSL element is used to add dependencies to a component, like {@link JvmTestSuite}.
 *
 * <ul>
 *     <li><code>implementation</code> dependencies are used at compilation and runtime.</li>
 *     <li><code>compileOnly</code> dependencies are used only at compilation and are not available at runtime.</li>
 *     <li><code>runtimeOnly</code> dependencies are not available at  compilation and are used only at runtime.</li>
 * </ul>
 *
 * @since 7.3
 *
 * @see org.gradle.api.artifacts.dsl.DependencyHandler For more information.
 */
@Incubating
public interface JvmComponentDependencies {

    // Easily-usable methods for commonly used dependency notations
    void implementation(String dependencyNotation);
    void implementation(String dependencyNotation, Action<? super ExternalModuleDependency> configuration);
    void implementation(Map<String, ?> dependencyNotation);
    void implementation(Map<String, ?> dependencyNotation, Action<? super ExternalModuleDependency> configuration);
    void implementation(Project dependencyNotation);
    void implementation(Project dependencyNotation, Action<? super ProjectDependency> configuration);
    void implementation(FileCollection dependencyNotation);
    void implementation(FileCollection dependencyNotation, Action<? super FileCollectionDependency> configuration);
    // Note that these are specifically typed to MinimalExternalModuleDependency, because they're only for version catalog
    // If a user wants to Provider a different type of dependency, the more verbose mechanism below should be used.
    void implementation(Provider<? extends MinimalExternalModuleDependency> dependencyNotation);
    void implementation(Provider<? extends MinimalExternalModuleDependency> dependencyNotation, Action<? super ExternalModuleDependency> configuration);
    void implementation(ProviderConvertible<? extends MinimalExternalModuleDependency> dependencyNotation);
    void implementation(ProviderConvertible<? extends MinimalExternalModuleDependency> dependencyNotation, Action<? super ExternalModuleDependency> configuration);
    // Generic dependency notation methods
    void implementation(Dependency dependencyNotation);
    <D extends Dependency> void implementation(D dependencyNotation, Action<? super D> configuration);
    void implementation(DependencyProvider<?> dependencyNotation);
    <D extends Dependency> void implementation(DependencyProvider<? extends D> dependencyNotation, Action<? super D> configuration);

    /**
     * Add a dependency to the set of compileOnly dependencies.
     * <p><br>
     * <code>compileOnly</code> dependencies are used only at compilation and are not available at runtime.
     *
     * @param dependencyNotation dependency to add
     * @see org.gradle.api.artifacts.dsl.DependencyHandler Valid dependency notations.
     */
    void compileOnly(Object dependencyNotation);
    /**
     * Add a dependency to the set of compileOnly dependencies with additional configuration.
     * <p><br>
     * <code>compileOnly</code> dependencies are used only at compilation and are not available at runtime.
     *
     * @param dependencyNotation dependency to add
     * @param configuration additional configuration for the provided dependency
     * @see org.gradle.api.artifacts.dsl.DependencyHandler Valid dependency notations.
     */
    void compileOnly(Object dependencyNotation, Action<? super Dependency> configuration);

    /**
     * Add a dependency to the set of runtimeOnly dependencies.
     * <p><br>
     * <code>runtimeOnly</code> dependencies are not available at  compilation and are used only at runtime.
     *
     * @param dependencyNotation dependency to add
     * @see org.gradle.api.artifacts.dsl.DependencyHandler Valid dependency notations.
     */
    void runtimeOnly(Object dependencyNotation);
    /**
     * Add a dependency to the set of runtimeOnly dependencies with additional configuration.
     * <p><br>
     * <code>runtimeOnly</code> dependencies are not available at  compilation and are used only at runtime.
     *
     * @param dependencyNotation dependency to add
     * @param configuration additional configuration for the provided dependency
     * @see org.gradle.api.artifacts.dsl.DependencyHandler Valid dependency notations.
     */
    void runtimeOnly(Object dependencyNotation, Action<? super Dependency> configuration);

    /**
     * Add a dependency to the set of annotationProcessor dependencies.
     * <p><br>
     * <code>annotationProcessor</code> dependencies containing annotation processors to be run at compile time.
     *
     * @param dependencyNotation dependency to add
     * @see org.gradle.api.artifacts.dsl.DependencyHandler Valid dependency notations.
     * @since 7.5
     */
    void annotationProcessor(Object dependencyNotation);
    /**
     * Add a dependency to the set of annotationProcessor dependencies.
     * <p><br>
     * <code>annotationProcessor</code> dependencies containing annotation processors to be run at compile time.
     *
     * @param dependencyNotation dependency to add
     * @param configuration additional configuration for the provided dependency
     * @see org.gradle.api.artifacts.dsl.DependencyHandler Valid dependency notations.
     * @since 7.5
     */
    void annotationProcessor(Object dependencyNotation, Action<? super Dependency> configuration);
}
