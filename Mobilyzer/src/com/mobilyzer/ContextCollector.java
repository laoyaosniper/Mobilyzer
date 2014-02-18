/*
 * Copyright 2013 RobustNet Lab, University of Michigan. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.mobilyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.net.TrafficStats;

import com.mobilyzer.util.PhoneUtils;

/**
 * 
 * @author Jack Jia, Ashkan Nikravesh (ashnik@umich.edu) Collects context information periodically
 *         (using a Timer). User can specify the interval.
 */
public class ContextCollector {

  private volatile ArrayList<HashMap<String, String>> contextResultArray;
  private PhoneUtils phoneUtils;
  private int interval;
  private Timer timer;
  private volatile boolean isRunning;
  private int count;
  public String ipconnectivity = "NOT SUPPORTED";
  public String dnsresolvability = "NOT SUPPORTED";
  public ContextCollector() {
    phoneUtils = PhoneUtils.getPhoneUtils();
    this.isRunning = false;
    this.timer = new Timer();
    contextResultArray = new ArrayList<HashMap<String, String>>();
    count = 0;
  }

  /**
   * this function sets the interval of context collection (in seconds)
   * 
   * @param intervalSecond time between each context info snapshot
   */
  public void setInterval(int intervalSecond) {
    this.interval = intervalSecond;
    if (intervalSecond <= 0) {
      this.interval = Config.DEFAULT_CONTEXT_INTERVAL_SEC;
    }
  }

  /**
   * called by the timer and return the current context info of device
   * 
   * @return a hash map that contains all the context data
   */
  private HashMap<String, String> getCurrentContextInfo() {
    HashMap<String, String> currentContext = new HashMap<String, String>();;
    long prevSend = 0;
    long prevRecv = 0;
    long sendBytes = 0;
    long recvBytes = 0;
    long intervalSend = 0;
    long intervalRecv = 0;
    long prevPktSend = 0;
    long prevPktRecv = 0;
    long sendPkt = 0;
    long recvPkt = 0;
    long intervalPktSend = 0;
    long intervalPktRecv = 0;
    
    PhoneUtils phoneUtils = PhoneUtils.getPhoneUtils();
    ipconnectivity = phoneUtils.getIpConnectivity();
    dnsresolvability = phoneUtils.getDnResolvability();
  
    sendBytes = TrafficStats.getMobileTxBytes();
    recvBytes = TrafficStats.getMobileRxBytes();
    sendPkt = TrafficStats.getMobileTxPackets();
    sendPkt = TrafficStats.getMobileRxPackets();
    if (prevSend > 0 || prevRecv > 0) {
      intervalSend = sendBytes - prevSend;
      intervalRecv = recvBytes - prevRecv;
    }
    if (prevPktSend > 0 || prevPktRecv > 0) {
      intervalPktSend = sendPkt - prevPktSend;
      intervalPktRecv = recvPkt - prevPktRecv;
    }
    prevSend = sendBytes;
    prevRecv = recvBytes;
    prevPktSend = sendPkt;
    prevPktRecv = recvPkt;

    currentContext.put("timestamp", (System.currentTimeMillis() * 1000) + "");
    currentContext.put("rssi", phoneUtils.getCurrentRssi() + "");
    currentContext.put("inc_mobile_bytes_send", intervalSend + "");
    currentContext.put("inc_mobile_bytes_recv", intervalRecv + "");
    currentContext.put("inc_mobile_pkt_send", intervalPktSend + "");
    currentContext.put("inc_mobile_pkt_recv", intervalPktRecv + "");
    currentContext.put("battery_level", phoneUtils.getCurrentBatteryLevel() + "");
    return currentContext;
  }

  /**
   * Starts the context collection timer task. It should be called when a measurement task gets
   * started.
   * 
   * @return false if the collector is already running.
   */
  public boolean startCollector() {
    if (isRunning) {
      return false;
    }
    isRunning = true;
    timer.scheduleAtFixedRate(timerTask, 0, interval * 1000);
    return true;


  }

  /**
   * Stops the context collection task. It attaches the current context data to the results
   * 
   * @return array of all context info collected at specific time intervals
   */
  public ArrayList<HashMap<String, String>> stopCollector() {
    if (!isRunning) {
      return null;
    }
    timerTask.cancel();
    timer.cancel();
    isRunning = false;
    contextResultArray.add(getCurrentContextInfo());
    return contextResultArray;
  }
  
  /**
   * Return the current Ip connectivity
   * 
   * @return A string that represents the current ip connectivity.
   */
  public String getCurrentIPConnectivity(){
	  return ipconnectivity;
  }
  
  /**
   * Return the current DNS resolvability
   * 
   * @return A string that represents the current DNS resolvability.
   */
  public String getCurrentDNSResolvability(){
	  return dnsresolvability;
  } 

  private TimerTask timerTask = new TimerTask() {
    @Override
    public void run() {
      if (ContextCollector.this.count < Config.MAX_CONTEXT_INFO_COLLECTIONS_PER_TASK) {
        contextResultArray.add(getCurrentContextInfo());
        ContextCollector.this.count++;
      }
    }
  };


}
