/*
* Copyright 2018 Nextworks s.r.l.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package it.nextworks.catalogue.common.exception;

/**
 * Created by Marco Capitani on 14/04/17.
 *
 * @author Marco Capitani (m.capitani AT nextworks.it)
 */
public class OptimizationFailedException extends Exception {

    public OptimizationFailedException() {
        super();
    }

    public OptimizationFailedException(Throwable throwable) {
        super(throwable);
    }

    protected OptimizationFailedException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }

    public OptimizationFailedException(String message) {
        super(message);
    }

    public OptimizationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
