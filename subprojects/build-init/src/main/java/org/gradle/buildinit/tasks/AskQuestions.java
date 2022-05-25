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

package org.gradle.buildinit.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.internal.tasks.userinput.UserInputHandler;
import org.gradle.api.tasks.TaskAction;
import org.gradle.buildinit.interrogator.Interrogator;
import org.gradle.buildinit.interrogator.model.Descriptor;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

public class AskQuestions extends DefaultTask {

    private UserInputHandler userInputHandler;

    @Inject
    public AskQuestions(UserInputHandler userInputHandler) {
        this.isCompatibleWithConfigurationCache();
        this.userInputHandler = userInputHandler;
    }

    @TaskAction
    public void askQuestions() throws IOException {
        File descriptorFile = new File("/Users/bhegyi/Developer/Coding/gradle/subprojects/build-init/src/test/resources/org/gradle/buildinit/interrogator/test.json");
        Descriptor descriptor = Descriptor.read(descriptorFile);

        Interrogator interrogator = new Interrogator(userInputHandler);
        interrogator.askQuestions(descriptor.getQuestions());
    }

}
