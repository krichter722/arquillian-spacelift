/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.arquillian.spacelift.process;

/**
 * A transformer for process execution output.
 *
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 */
public interface OutputTransformer {

    /**
     * Transforms process output to a different string. This might be handy
     * if you want to visually distinguish in between outputs of different processes.
     *
     * @param output
     *     Sentence to be transformed
     *
     * @return modified output
     */
    Sentence transform(Sentence output);
}
