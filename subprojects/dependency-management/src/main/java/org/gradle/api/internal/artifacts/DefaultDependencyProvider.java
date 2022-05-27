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

package org.gradle.api.internal.artifacts;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyProvider;
import org.gradle.api.internal.provider.AbstractMinimalProvider;
import org.gradle.api.internal.provider.ProviderInternal;

import javax.annotation.Nullable;

/**
 * A caching provider that only works for {@link Dependency Dependencies}.
 *
 * @param <D> the dependency type
 */
public class DefaultDependencyProvider<D extends Dependency> extends AbstractMinimalProvider<D> implements DependencyProvider<D> {
    private final ProviderInternal<D> delegate;
    @Nullable
    private Value<? extends D> cache;

    public DefaultDependencyProvider(ProviderInternal<D> delegate) {
        this.delegate = delegate;
    }

    @Override
    protected Value<? extends D> calculateOwnValue(ValueConsumer consumer) {
        if (cache == null) {
            cache = delegate.calculateValue(consumer);
        }
        return cache;
    }

    @Nullable
    @Override
    public Class<D> getType() {
        return delegate.getType();
    }
}
