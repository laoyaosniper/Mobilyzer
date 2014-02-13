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

import com.mobilyzer.util.Logger;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * @author Hongyi Yao (hyyao@umich.edu)
 * Define message handler to process message request from API
 */
public class APIRequestHandler extends Handler {
  MeasurementScheduler scheduler;
  
  /**
   * Constructor for APIRequestHandler
   * @param scheduler Parent context for this object
   */
  public APIRequestHandler(MeasurementScheduler scheduler) {
    this.scheduler = scheduler;
  }
  
  @Override
  public void handleMessage(Message msg) {
    Bundle data = msg.getData();
    data.setClassLoader(scheduler.getApplicationContext().getClassLoader());
    String clientKey = data.getString("clientKey");

    MeasurementTask task = null;
    String taskId = null;
    switch (msg.what) {
      case Config.MSG_SUBMIT_TASK:
        task = (MeasurementTask) data.getParcelable("measurementTask");
        if ( task != null ) {
          // Hongyi: for delay measurement
          task.getDescription().parameters.put("ts_scheduler_recv",
            String.valueOf(System.currentTimeMillis()));
          
          Logger.d("New task added: taskId " + task.getTaskId());
          
          taskId = scheduler.submitTask(task);
        }
        break;
      case Config.MSG_CANCEL_TASK:
        taskId = data.getString("taskId");
        if ( taskId != null && clientKey != null ) {
          Logger.d("cancel taskId: " + taskId + ", clientKey: " + clientKey);
          scheduler.cancelTask(taskId, clientKey);
        }
        break;
      default:
        break;
    }
  }
}