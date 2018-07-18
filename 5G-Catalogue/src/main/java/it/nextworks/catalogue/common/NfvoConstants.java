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
package it.nextworks.catalogue.common;

public final class NfvoConstants {

	public static final String engineQueueNamePrefix = "engine-in-";
	public static final String engineQueueExchange = "engine-queue-exchange";

	public static final String computationQueueName = "computation-in";
	public static final String computationQueueExchange = "computation-queue-exchange";
	
	public static final String allocationQueueNamePrefix = "allocation-in-";
	public static final String allocationQueueExchange = "allocation-queue-exchange";
	
	public static final String vnfmQueueNamePrefix = "vnfm-in-";
	public static final String vnfmQueueExchange = "vnfm-queue-exchange";

}
