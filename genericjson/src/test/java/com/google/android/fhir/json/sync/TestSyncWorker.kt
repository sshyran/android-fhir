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

package com.google.android.fhir.json.sync

import android.content.Context
import androidx.work.WorkerParameters
import com.google.android.fhir.json.resource.TestingUtils

class TestSyncWorker(appContext: Context, workerParams: WorkerParameters) :
  JsonSyncWorker(appContext, workerParams) {

  override fun getDataSource() = TestingUtils.TestDataSourceImpl

  override fun getFhirEngine() = TestingUtils.TestJsonEngineImpl

  override fun getDownloadWorkManager() = TestingUtils.TestDownloadManagerImpl()

  override fun getConflictResolver() = AcceptRemoteConflictResolver
}
