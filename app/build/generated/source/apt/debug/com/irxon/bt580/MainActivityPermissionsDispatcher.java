// This file was generated by PermissionsDispatcher. Do not modify!
package com.irxon.bt580;

import android.support.v4.app.ActivityCompat;
import java.lang.String;
import permissions.dispatcher.PermissionUtils;

final class MainActivityPermissionsDispatcher {
  private static final int REQUEST_LOCATIONPERMISSION = 0;

  private static final String[] PERMISSION_LOCATIONPERMISSION = new String[] {"android.permission.ACCESS_COARSE_LOCATION"};

  private MainActivityPermissionsDispatcher() {
  }

  static void LocationPermissionWithPermissionCheck(MainActivity target) {
    if (PermissionUtils.hasSelfPermissions(target, PERMISSION_LOCATIONPERMISSION)) {
      target.LocationPermission();
    } else {
      ActivityCompat.requestPermissions(target, PERMISSION_LOCATIONPERMISSION, REQUEST_LOCATIONPERMISSION);
    }
  }

  static void onRequestPermissionsResult(MainActivity target, int requestCode, int[] grantResults) {
    switch (requestCode) {
      case REQUEST_LOCATIONPERMISSION:
      if (PermissionUtils.verifyPermissions(grantResults)) {
        target.LocationPermission();
      }
      break;
      default:
      break;
    }
  }
}
