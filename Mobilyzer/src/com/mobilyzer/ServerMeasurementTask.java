/* Copyright 2013 RobustNet Lab, University of Michigan. All Rights Reserved.
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
package com.mobilyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;

import android.content.Intent;

import com.mobilyzer.MeasurementResult.TaskProgress;
import com.mobilyzer.exceptions.MeasurementError;
import com.mobilyzer.exceptions.MeasurementSkippedException;
import com.mobilyzer.util.Logger;
import com.mobilyzer.util.PhoneUtils;

public class ServerMeasurementTask implements Callable<MeasurementResult []> {
  private MeasurementTask realTask;
  private MeasurementScheduler scheduler;
  private ContextCollector contextCollector;
  public ServerMeasurementTask(MeasurementTask task, MeasurementScheduler scheduler) {
    realTask = task;
    this.scheduler = scheduler;
    this.contextCollector= new ContextCollector();
  }

  /**
   * Notify the scheduler that this task is started
   */
  private void broadcastMeasurementStart() {
    Intent intent = new Intent();
    intent.setAction(UpdateIntent.MEASUREMENT_PROGRESS_UPDATE_ACTION);
    intent.putExtra(UpdateIntent.TASK_STATUS_PAYLOAD, Config.TASK_STARTED);
    intent.putExtra(UpdateIntent.TASKID_PAYLOAD, realTask.getTaskId());
    intent.putExtra(UpdateIntent.CLIENTKEY_PAYLOAD, realTask.getKey());
    scheduler.sendBroadcast(intent);
  }

  /**
   * Notify the scheduler that this task is finished executing.
   * The result can be completed, paused or failed due to exception 
   * @param results Results of the task
   * @param error Measurement error leading to task's failure
   */
  private void broadcastMeasurementEnd(MeasurementResult[] results
    , MeasurementError error) {

    // Only broadcast information about measurements if they are true errors.
    if (!(error instanceof MeasurementSkippedException)) {
      Intent intent = new Intent();
      intent.setAction(UpdateIntent.MEASUREMENT_PROGRESS_UPDATE_ACTION);
      intent.putExtra(UpdateIntent.TASK_STATUS_PAYLOAD, Config.TASK_FINISHED);
      intent.putExtra(UpdateIntent.TASK_PRIORITY_PAYLOAD, 
        (int) realTask.getDescription().priority);
      intent.putExtra(UpdateIntent.TASKID_PAYLOAD, realTask.getTaskId());
      intent.putExtra(UpdateIntent.CLIENTKEY_PAYLOAD, realTask.getKey());

      if (results != null){
        // Only single task can be paused
        if(results[0].getTaskProgress()==TaskProgress.PAUSED){
          intent.putExtra(UpdateIntent.TASK_STATUS_PAYLOAD, Config.TASK_PAUSED);
        }
        else {
          intent.putExtra(UpdateIntent.TASK_STATUS_PAYLOAD, Config.TASK_FINISHED);
          intent.putExtra(UpdateIntent.RESULT_PAYLOAD, results);
        }
        scheduler.sendBroadcast(intent);
      }
    }

  }

  @Override
  public MeasurementResult[] call() throws MeasurementError {
    MeasurementResult[] results = null;
    PhoneUtils phoneUtils = PhoneUtils.getPhoneUtils();
    try {
      phoneUtils.acquireWakeLock();

      if(!(phoneUtils.isCharging() ||
          phoneUtils.getCurrentBatteryLevel() > Config.MIN_BATTERY_THRESHOLD)){
        throw new MeasurementSkippedException("Not enough battery power");
      }
      broadcastMeasurementStart();
      try {
        contextCollector.setInterval(realTask.getDescription().contextIntervalSec);
        contextCollector.startCollector();
        results = realTask.call(); 
        ArrayList<HashMap<String, String>> contextResults = 
            contextCollector.stopCollector();
        for (MeasurementResult r: results){
          r.addContextResults(contextResults);
          r.getDeviceProperty().dnResolvability=contextCollector.dnsConnectivity;
          r.getDeviceProperty().ipConnectivity=contextCollector.ipConnectivity;
        }
        
        broadcastMeasurementEnd(results, null);
      } catch (MeasurementError e) {
        String error = "Server measurement " + realTask.getDescriptor() 
            + " has failed: " + e.getMessage() + "\n";
        Logger.e(error);
        results = MeasurementResult.getFailureResult(realTask, e);
        broadcastMeasurementEnd(results, e);
        
      } catch (Exception e) {
        String error = "Server measurement " +
            realTask.getDescriptor() + " has failed\n";
        error += "Unexpected Exception: " + e.getMessage() + "\n";
        Logger.e(error);

        results = MeasurementResult.getFailureResult(realTask, e);
        broadcastMeasurementEnd(results,
          new MeasurementError("Got exception running task", e));
      }
    } finally {
      phoneUtils.releaseWakeLock();
      MeasurementTask currentTask = scheduler.getCurrentTask();
      if(currentTask != null && currentTask.equals(realTask)){
        scheduler.setCurrentTask(null);
      }
    }
    return results;
  }
}
