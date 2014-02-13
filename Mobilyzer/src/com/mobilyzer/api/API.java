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
package com.mobilyzer.api;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;


import android.os.Bundle;

import com.mobilyzer.Config;
import com.mobilyzer.MeasurementTask;
import com.mobilyzer.UpdateIntent;
import com.mobilyzer.exceptions.MeasurementError;
import com.mobilyzer.measurements.DnsLookupTask;
import com.mobilyzer.measurements.HttpTask;
import com.mobilyzer.measurements.ParallelTask;
import com.mobilyzer.measurements.PingTask;
import com.mobilyzer.measurements.SequentialTask;
import com.mobilyzer.measurements.TCPThroughputTask;
import com.mobilyzer.measurements.TracerouteTask;
import com.mobilyzer.measurements.UDPBurstTask;
import com.mobilyzer.measurements.DnsLookupTask.DnsLookupDesc;
import com.mobilyzer.measurements.HttpTask.HttpDesc;
import com.mobilyzer.measurements.ParallelTask.ParallelDesc;
import com.mobilyzer.measurements.PingTask.PingDesc;
import com.mobilyzer.measurements.SequentialTask.SequentialDesc;
import com.mobilyzer.measurements.TCPThroughputTask.TCPThroughputDesc;
import com.mobilyzer.measurements.TracerouteTask.TracerouteDesc;
import com.mobilyzer.measurements.UDPBurstTask.UDPBurstDesc;
import com.mobilyzer.util.Logger;

/**
 * @author jackjia,Hongyi Yao (hyyao@umich.edu)
 * The user API for Mobiperf library.
 * Use singleton design pattern to ensure there only exist one instance of API
 * User: create and add task => Scheduler: run task, send finish intent =>
 * User: register BroadcastReceiver for userResultAction and serverResultAction
 * TODO(Hongyi): is this class thread safe?
 */
public final class API {
  public enum TaskType {
    DNSLOOKUP, HTTP, PING, TRACEROUTE, TCPTHROUGHPUT, UDPBURST,
    PARALLEL, SEQUENTIAL, INVALID
  }

  /**
   * Action name of different type of result for broadcast receiver.
   * userResultAction is not a constant value. We append the clientKey to 
   * UpdateIntent.USER_RESULT_ACTION so that only the user who submit the task
   * can get the result   
   */
  public String userResultAction;
  public static final String SERVER_RESULT_ACTION =
      UpdateIntent.SERVER_RESULT_ACTION;
  
  public final static int USER_PRIORITY = MeasurementTask.USER_PRIORITY;
  public final static int INVALID_PRIORITY = MeasurementTask.INVALID_PRIORITY;
  
  private Context applicationContext;
  
  private boolean isBound = false;
  private boolean isBindingToService = false;
  Messenger mSchedulerMessenger = null;
  
  private String clientKey;
  
  /**
   * Singleton api object for the entire application
   */
  private static API apiObject;
  
  /**
   * Make constructor private for singleton design
   * @param parent Context when the object is created
   * @param clientKey User-defined unique key for this application
   */
  private API(Context parent, String clientKey) {
    Logger.d("API: constructor is called...");
    this.applicationContext = parent.getApplicationContext();
    this.clientKey = clientKey;

    this.userResultAction = UpdateIntent.USER_RESULT_ACTION + "." + clientKey;
    bind();
  }

  /**
   * Actual method to get the singleton API object
   * @param parent Context which the object lies in
   * @param clientKey User-defined unique key for this application
   * @return Singleton API object
   */
  public static API getAPI(Context parent, String clientKey) {
    Logger.d("API: Get API Singeton object...");
    if ( apiObject == null ) {
      Logger.d("API: API object not initialized...");
      apiObject = new API(parent, clientKey);
    }
    else {
      // Safeguard to avoid using unbound API object 
      apiObject.bind();
    }
    return apiObject;
  }
  
  @Override
  public Object clone() throws CloneNotSupportedException {
    // Prevent the singleton object to be copied
    throw new CloneNotSupportedException();
  }
  
  
  /** Defines callbacks for binding and unbinding scheduler*/
  private ServiceConnection serviceConn = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      Logger.d("API -> onServiceConnected called");
      // We've bound to the scheduler's messenger and get Messenger instance
      mSchedulerMessenger = new Messenger(service);
      isBound = true;
      isBindingToService = false;
    }
    
    @Override
    public void onServiceDisconnected(ComponentName arg0) {
      Logger.d("API -> onServiceDisconnected called");
      mSchedulerMessenger = null;
      isBound = false;
    }
  };

  /**
   * Get available messenger after binding to scheduler
   * @return the messenger if bound, null otherwise
   */
  private Messenger getScheduler() {
    if (isBound) {
      Logger.e("API -> get available messenger");
      return mSchedulerMessenger;
    } else {
      Logger.e("API -> have not bound to a scheduler!");
      return null;
    }
  }
  
  /**
   * Bind to scheduler, automatically called when the API is initialized
   */
  public void bind() {
    Logger.e("API-> bind() called "+isBindingToService+" "+isBound);
    if (!isBindingToService && !isBound) {
      Logger.e("API-> bind() called 2");
      // Bind to the scheduler service if it is not bounded
      Intent intent = new Intent("com.mobilyzer.MeasurementScheduler");
      applicationContext.bindService(intent, serviceConn, Context.BIND_AUTO_CREATE);
      isBindingToService = true;
    }
  }
  
  /**
   * Unbind from scheduler, called in activity's onDestroy callback function
   */
  public void unbind() {
    Logger.e("API-> unbind called");
    if (isBound) {
      Logger.e("API-> unbind called 2");
      applicationContext.unbindService(serviceConn);

      isBound = false;
    }
  }

  /**
   * Create a new MeasurementTask based on those parameters. Then submit it to
   * scheduler by addTask or put into task list of parallel or sequential task
   * @param taskType Type of measurement (ping, dns, traceroute, etc.) for this
   *        measurement task.
   * @param startTime Earliest time that measurements can be taken using this 
   *        Task descriptor. The current time will be used in place of a null
   *        startTime parameter. Measurements with a startTime more than 24 
   *        hours from now will NOT be run.
   * @param endTime Latest time that measurements can be taken using this Task
   *        descriptor. Tasks with an endTime before startTime will be canceled.
   *        Corresponding to the 24-hour rule in startTime, tasks with endTime
   *        later than 24 hours from now will be assigned a new endTime that
   *        ends 24 hours from now.
   * @param intervalSec Minimum number of seconds to elapse between consecutive
   *        measurements taken with this description.
   * @param count Maximum number of times that a measurement should be taken
   *        with this description. A count of 0 means to continue the 
   *        measurement indefinitely (until end_time).
   * @param priority Two level of priority: USER_PRIORITY for user task and
   *        INVALID_PRIORITY for server task
   * @param contextIntervalSec interval between the context collection (in sec)
   * @param params Measurement parameters.
   * @return Measurement task filled with those parameters
   * @throws MeasurementError taskType is not valid
   */
  public MeasurementTask createTask( TaskType taskType, Date startTime
    , Date endTime, double intervalSec, long count, long priority
    , int contextIntervalSec, Map<String, String> params)
        throws MeasurementError {
    MeasurementTask task = null;    
    switch ( taskType ) {
      case DNSLOOKUP:
        task = new DnsLookupTask(new DnsLookupDesc(clientKey, startTime, endTime
          , intervalSec, count, priority, contextIntervalSec, params));
        break;
      case HTTP:
        task = new HttpTask(new HttpDesc(clientKey, startTime, endTime
          , intervalSec, count, priority, contextIntervalSec, params));
        break;
      case PING:
        task = new PingTask(new PingDesc(clientKey, startTime, endTime
          , intervalSec, count, priority, contextIntervalSec, params));
        break;
      case TRACEROUTE:
        task = new TracerouteTask(new TracerouteDesc(clientKey, startTime, endTime
          , intervalSec, count, priority, contextIntervalSec, params));
        break;
      case TCPTHROUGHPUT:
        task = new TCPThroughputTask(new TCPThroughputDesc(clientKey, startTime
          , endTime, intervalSec, count, priority, contextIntervalSec, params));
        break;
      case UDPBURST:
        task = new UDPBurstTask(new UDPBurstDesc(clientKey, startTime, endTime
          , intervalSec, count, priority, contextIntervalSec, params));
        break;
      default:
        throw new MeasurementError("Undefined measurement type. Candidate: " +
            "DNSLOOKUP, HTTP, PING, TRACEROUTE, TCPTHROUGHPUT, UDPBURST");
    }
    return task;
  }

  /**
   * Create a parallel or sequential task based on the manner. An ArrayList of
   * MeasurementTask must be provided as the real tasks to be executed
   * @param manner Determine whether tasks in task list will be executed
   *        parallelly or sequentially (back-to-back)
   * @param startTime Earliest time that measurements can be taken using this 
   *        Task descriptor. The current time will be used in place of a null
   *        startTime parameter. Measurements with a startTime more than 24 
   *        hours from now will NOT be run.
   * @param endTime Latest time that measurements can be taken using this Task
   *        descriptor. Tasks with an endTime before startTime will be canceled.
   *        Corresponding to the 24-hour rule in startTime, tasks with endTime
   *        later than 24 hours from now will be assigned a new endTime that
   *        ends 24 hours from now.
   * @param intervalSec Minimum number of seconds to elapse between consecutive
   *        measurements taken with this description.
   * @param count Maximum number of times that a measurement should be taken
   *        with this description. A count of 0 means to continue the 
   *        measurement indefinitely (until end_time).
   * @param priority Two level of priority: USER_PRIORITY for user task and
   *        INVALID_PRIORITY for server task
   * @param contextIntervalSec interval between the context collection (in sec)
   * @param params Measurement parameters.
   * @param taskList tasks to be executed 
   * @return The parallel or sequential task filled with those parameters
   * @throws MeasurementError manner is not valid
   */
  public MeasurementTask composeTasks(TaskType manner, Date startTime,
    Date endTime, double intervalSec, long count, long priority,
    int contextIntervalSec, Map<String, String> params,
    ArrayList<MeasurementTask> taskList) throws MeasurementError {
    MeasurementTask task = null;
    switch ( manner ) {
      case PARALLEL:
        task = new ParallelTask(new ParallelDesc(clientKey, startTime, endTime
          , intervalSec, count, priority, contextIntervalSec, params), taskList);
        break;
      case SEQUENTIAL:
        task = new SequentialTask(new SequentialDesc(clientKey, startTime, endTime
          , intervalSec, count, priority, contextIntervalSec, params), taskList);
        break;
      default:
        throw new MeasurementError("Undefined measurement composing type. " + 
            " Candidate: PARALLEL, SEQUENTIAL");
    }
    return task;
  }
 
  /**
   * Submit task to the scheduler. 
   * Works in async way. The result will be returned in a intent whose action is
   * USER_RESULT_ACTION + clientKey or SERVER_RESULT_ACTION
   * @param task the task to be exectued, created by createTask(..)
   *        or composeTask(..)
   * @throws MeasurementError
   */
  public void submitTask ( MeasurementTask task )
      throws MeasurementError {
    Messenger messenger = getScheduler();
    if ( messenger != null ) {
      // Hongyi: for delay measurement
      task.getDescription().parameters.put("ts_api_send",
        String.valueOf(System.currentTimeMillis()));
      
      Logger.d("Adding new task");
      Message msg = Message.obtain(null, Config.MSG_SUBMIT_TASK);
      Bundle data = new Bundle();
      if ( task != null ) {
        data.putParcelable("measurementTask", task);
        msg.setData(data);  
        try {
          messenger.send(msg);
        } catch (RemoteException e) {
          String err = "remote scheduler failed!";
          Logger.e(err);
          throw new MeasurementError(err);
        }
      }
    }
    else {
      String err = "scheduler doesn't exist";
      Logger.e(err);
      throw new MeasurementError(err);
    }
  }

  /**
   * Cancel the task submitted to the scheduler
   * @param localId task to be cancelled. Got by MeasurementTask.getTaskId() 
   * @return true for succeed, false for fail
   * @throws InvalidParameterException
   */
  public void cancelTask(String taskId) throws MeasurementError{
    Messenger messenger = getScheduler();
    if ( messenger != null ) {
      Message msg = Message.obtain(null, Config.MSG_CANCEL_TASK);      
      Bundle data = new Bundle();
      Logger.d("API: CANCEL task " + taskId);
      data.putString("taskId", taskId);
      data.putString("clientKey", clientKey);
      msg.setData(data);  
      try {
        messenger.send(msg);
      } catch (RemoteException e) {
        String err = "remote scheduler failed!";
        Logger.e(err);
        throw new MeasurementError(err);
      }      
    }
    else {
      String err = "scheduler doesn't exist";
      Logger.e(err);
      throw new MeasurementError(err);
    }
  }

  /** Gets the currently available measurement descriptions*/
  public static Set<String> getMeasurementNames() {
    return MeasurementTask.getMeasurementNames();
  }
  
  /** Get the type of a measurement based on its name. Type is for JSON interface only
   * where as measurement name is a readable string for the UI */
  public static String getTypeForMeasurementName(String name) {
    return MeasurementTask.getTypeForMeasurementName(name);
  }
}
