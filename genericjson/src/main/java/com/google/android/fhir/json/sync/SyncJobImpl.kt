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
import androidx.lifecycle.asFlow
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.hasKeyWithValueOfType
import com.google.android.fhir.json.DatastoreUtil
import com.google.android.fhir.json.FhirEngine
import com.google.android.fhir.json.FhirEngineProvider
import com.google.android.fhir.json.OffsetDateTimeTypeAdapter
import com.google.gson.GsonBuilder
import java.time.OffsetDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.mapNotNull
import org.hl7.fhir.r4.model.ResourceType
import timber.log.Timber

class SyncJobImpl(private val context: Context) : SyncJob {
  private val syncWorkType = SyncWorkType.DOWNLOAD_UPLOAD
  private val gson =
    GsonBuilder()
      .registerTypeAdapter(OffsetDateTime::class.java, OffsetDateTimeTypeAdapter().nullSafe())
      .create()

  /** Periodically sync the data with given configuration for given worker class */
  @ExperimentalCoroutinesApi
  override fun <W : FhirSyncWorker> poll(
    periodicSyncConfiguration: PeriodicSyncConfiguration,
    clazz: Class<W>
  ): Flow<State> {
    val workerUniqueName = syncWorkType.workerName

    Timber.d("Configuring polling for $workerUniqueName")

    val periodicWorkRequest = Sync.createPeriodicWorkRequest(periodicSyncConfiguration, clazz)
    val workManager = WorkManager.getInstance(context)

    val flow = stateFlow()

    workManager.enqueueUniquePeriodicWork(
      workerUniqueName,
      ExistingPeriodicWorkPolicy.REPLACE,
      periodicWorkRequest
    )

    return flow
  }

  override fun stateFlow(): Flow<State> {
    return workInfoFlow().mapNotNull { convertToState(it) }
  }

  override fun lastSyncTimestamp(): OffsetDateTime? {
    return DatastoreUtil(context).readLastSyncTimestamp()
  }

  override fun workInfoFlow(): Flow<WorkInfo> {
    return WorkManager.getInstance(context)
      .getWorkInfosForUniqueWorkLiveData(syncWorkType.workerName)
      .asFlow()
      .flatMapConcat { it.asFlow() }
  }

  private fun convertToState(workInfo: WorkInfo): State? {
    return workInfo.progress
      .takeIf { it.keyValueMap.isNotEmpty() && it.hasKeyWithValueOfType<String>("StateType") }
      ?.let {
        val state = it.getString("StateType")!!
        val stateData = it.getString("State")
        gson.fromJson(stateData, Class.forName(state)) as State
      }
  }

  /**
   * Run fhir synchronizer immediately with default sync params configured on initialization and
   * subscribe to given flow
   */
  override suspend fun run(
    fhirEngine: FhirEngine,
    downloadManager: DownloadWorkManager,
    resolver: ConflictResolver,
    subscribeTo: MutableSharedFlow<State>?
  ): Result {
    return FhirEngineProvider.getDataSource(context)?.let {
      FhirSynchronizer(context, fhirEngine, it, downloadManager, conflictResolver = resolver)
        .apply { if (subscribeTo != null) subscribe(subscribeTo) }
        .synchronize()
    }
      ?: Result.Error(
        listOf(
          ResourceSyncException(
            ResourceType.Bundle,
            IllegalStateException(
              "FhirEngineConfiguration.ServerConfiguration is not set. Call FhirEngineProvider.init to initialize with appropriate configuration."
            )
          )
        )
      )
  }
}
