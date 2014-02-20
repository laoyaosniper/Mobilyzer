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

import com.mobilyzer.MeasurementScheduler.DataUsageProfile;
import com.mobilyzer.MeasurementScheduler.TaskStatus;
import com.mobilyzer.util.Logger;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * @author Hongyi Yao (hyyao@umich.edu)
 * Define message handler to process message request from API
 */
public class APIRequestHandler extends Handler {
  MeasurementScheduler scheduler;
  
  public void sendAttributeToClient(Intent intent, String clientKey, String taskId) {
    if ( taskId != null ) {
      intent.putExtra(UpdateIntent.TASKID_PAYLOAD, taskId);
    }
    Logger.d("Sending attribute to client " + clientKey);
    scheduler.sendBroadcast(intent);
  }
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
    String clientKey = data.getString(UpdateIntent.CLIENTKEY_PAYLOAD);

    MeasurementTask task = null;
    String taskId = null;
    int batteryThreshold = 0;
    long interval = 0;
    Intent intent = new Intent();
    DataUsageProfile profile = DataUsageProfile.NOTFOUND;
    switch (msg.what) {
      case Config.MSG_SUBMIT_TASK:
        task = (MeasurementTask)
          data.getParcelable(UpdateIntent.MEASUREMENT_TASK_PAYLOAD);
        if ( task != null ) {
          // Hongyi: for delay measurement
          task.getDescription().parameters.put("ts_scheduler_recv",
            String.valueOf(System.currentTimeMillis()));
          
          Logger.d("New task added from " + clientKey
            + ": taskId " + task.getTaskId());
          taskId = scheduler.submitTask(task);
        }
        break;
      case Config.MSG_CANCEL_TASK:
        taskId = data.getString(UpdateIntent.TASKID_PAYLOAD);
        if ( taskId != null && clientKey != null ) {
          Logger.d("cancel taskId: " + taskId + ", clientKey: " + clientKey);
          scheduler.cancelTask(taskId, clientKey);
        }
        break;
      case Config.MSG_SET_BATTERY_THRESHOLD:
        batteryThreshold = data.getInt(UpdateIntent.BATTERY_THRESHOLD_PAYLOAD);
        if ( batteryThreshold != 0 ) {
          Logger.d("set battery threshold to " + batteryThreshold);
          scheduler.setBatteryThresh(batteryThreshold);
        }
        break;
      case Config.MSG_GET_BATTERY_THRESHOLD:
        batteryThreshold = scheduler.getBatteryThresh();
        Logger.d("get battery threshold " + batteryThreshold);
        intent.setAction(UpdateIntent.BATTERY_THRESHOLD_ACTION + "." + clientKey);
        intent.putExtra(UpdateIntent.BATTERY_THRESHOLD_PAYLOAD, batteryThreshold);
        sendAttributeToClient(intent, clientKey, null);
        break;
      case Config.MSG_SET_CHECKIN_INTERVAL:
        interval = data.getLong(UpdateIntent.CHECKIN_INTERVAL_PAYLOAD);
        if ( interval != 0 ) {
          Logger.d("set checkin interval to " + interval);
          scheduler.setCheckinInterval(interval);
        }
        break;
      case Config.MSG_GET_CHECKIN_INTERVAL:
        interval = scheduler.getCheckinInterval();
        Logger.d("get checkin interval " + interval);
        intent.setAction(UpdateIntent.CHECKIN_INTERVAL_ACTION + "." + clientKey);
        intent.putExtra(UpdateIntent.CHECKIN_INTERVAL_PAYLOAD, interval);
        sendAttributeToClient(intent, clientKey, null);
        break;
      case Config.MSG_GET_TASK_STATUS:
        taskId = data.getString(UpdateIntent.TASKID_PAYLOAD);
        TaskStatus taskStatus = scheduler.getTaskStatus(taskId);
        Logger.d("get task status for taskId " + taskId + " " + taskStatus);
        intent.setAction(UpdateIntent.TASK_STATUS_ACTION + "." + clientKey);
        intent.putExtra(UpdateIntent.TASKID_PAYLOAD, taskId);
        intent.putExtra(UpdateIntent.TASK_STATUS_PAYLOAD, taskStatus);
        sendAttributeToClient(intent, clientKey, taskId);
        break;
      case Config.MSG_SET_DATA_USAGE:
        profile = (DataUsageProfile)
          data.getSerializable(UpdateIntent.DATA_USAGE_PAYLOAD);
        if ( profile != DataUsageProfile.NOTFOUND ) {
          Logger.d(clientKey + " set data usage to " + profile );
          scheduler.setDataUsageLimit(profile);
        }
        break;
      case Config.MSG_GET_DATA_USAGE:
        profile = scheduler.getDataUsageProfile();
        Logger.d("get data usage " + profile);
        intent.setAction(UpdateIntent.DATA_USAGE_ACTION + "." + clientKey);
        intent.putExtra(UpdateIntent.DATA_USAGE_PAYLOAD, profile);
        sendAttributeToClient(intent, clientKey, taskId);
      default:
        break;
    }
  }
}