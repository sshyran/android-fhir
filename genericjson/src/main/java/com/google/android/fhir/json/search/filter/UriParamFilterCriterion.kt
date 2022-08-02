/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.fhir.json.search.filter

import ca.uhn.fhir.rest.gclient.UriClientParam
import com.google.android.fhir.json.search.ConditionParam
import com.google.android.fhir.json.search.Operation
import com.google.android.fhir.json.search.SearchDslMarker

/**
 * Represents a criterion for filtering [UriClientParam]. e.g. filter(ValueSet.URL, { value =
 * "https://example.com })
 */
@SearchDslMarker
data class UriParamFilterCriterion(val parameter: UriClientParam, var value: String? = null) :
  FilterCriterion {

  override fun getConditionalParams() = listOf(ConditionParam("index_value = ?", value!!))
}

internal data class UriFilterCriteria(
  val parameter: UriClientParam,
  override val filters: List<UriParamFilterCriterion>,
  override val operation: Operation
) : FilterCriteria(filters, operation, parameter, "UriIndexEntity")
