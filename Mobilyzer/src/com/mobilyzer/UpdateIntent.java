package com.mobilyzer;
import java.security.InvalidParameterException;

import android.content.Intent;

import android.os.Process;
/**
 * A repackaged Intent class that includes MobiLib-specific information. 
 */
public class UpdateIntent extends Intent {
  
  // Different types of payloads that this intent can carry:
  
  public static final String TASKID_PAYLOAD = "TASKID_PAYLOAD";
  public static final String CLIENTKEY_PAYLOAD = "CLIENTKEY_PAYLOAD";
  public static final String TASK_PRIORITY_PAYLOAD = "TASK_PRIORITY_PAYLOAD";
  public static final String RESULT_PAYLOAD = "RESULT_PAYLOAD";
  public static final String MEASUREMENT_TASK_PAYLOAD = "MEASUREMENT_TASK_PAYLOAD";
  public static final String BATTERY_THRESHOLD_PAYLOAD = "BATTERY_THRESHOLD_PAYLOAD";
  public static final String CHECKIN_INTERVAL_PAYLOAD = "CHECKIN_INTERVAL_PAYLOAD";
  public static final String TASK_STATUS_PAYLOAD = "TASK_STATUS_PAYLOAD";
  public static final String DATA_USAGE_PAYLOAD = "DATA_USAGE_PAYLOAD";
  public static final String VERSION_PAYLOAD = "VERSION_PAYLOAD";
  public static final String AUTH_ACCOUNT_PAYLOAD = "AUTH_ACCOUNT_PAYLOAD";
  
  // Different types of actions that this intent can represent:
  private static final String PACKAGE_PREFIX =
      UpdateIntent.class.getPackage().getName();
  private static final String APP_PREFIX = 
      UpdateIntent.class.getPackage().getName() + Process.myPid();
  
  public static final String MEASUREMENT_ACTION =
      APP_PREFIX + ".MEASUREMENT_ACTION";
  public static final String CHECKIN_ACTION =
      APP_PREFIX + ".CHECKIN_ACTION";
  public static final String CHECKIN_RETRY_ACTION =
      APP_PREFIX + ".CHECKIN_RETRY_ACTION";
  public static final String MEASUREMENT_PROGRESS_UPDATE_ACTION =
      APP_PREFIX + ".MEASUREMENT_PROGRESS_UPDATE_ACTION";
  
  public static final String USER_RESULT_ACTION =
      PACKAGE_PREFIX + ".USER_RESULT_ACTION";  
  public static final String SERVER_RESULT_ACTION =
      PACKAGE_PREFIX + ".SERVER_RESULT_ACTION";
  public static final String BATTERY_THRESHOLD_ACTION =
      PACKAGE_PREFIX + ".BATTERY_THRESHOLD_ACTION";
  public static final String CHECKIN_INTERVAL_ACTION =
      PACKAGE_PREFIX + ".CHECKIN_INTERVAL_ACTION";
  public static final String TASK_STATUS_ACTION =
      PACKAGE_PREFIX + ".TASK_STATUS_ACTION";
  public static final String DATA_USAGE_ACTION =
      PACKAGE_PREFIX + ".DATA_USAGE_ACTION";
  public static final String AUTH_ACCOUNT_ACTION =
      PACKAGE_PREFIX + ".AUTH_ACCOUNT_ACTION";

  /**
   * Creates an intent of the specified action with an optional message
   */
  protected UpdateIntent(String action)
      throws InvalidParameterException {
    super();
    if (action == null) {
      throw new InvalidParameterException("action of UpdateIntent should not be null");
    }
    this.setAction(action);
  }
}
