package com.example.imagesqueezer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.collections.ArrayList


class PermissionHandler(private val activity: AppCompatActivity) {
    private var showingDialog: Boolean = false
    private var lock = Any()

    private val requiredPermissions: Array<String?>
        get() = try {
            val info = activity.packageManager
                .getPackageInfo(activity.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
                ps
            } else {
                arrayOfNulls(0)
            }
        } catch (e: Exception) {
            arrayOfNulls(0)
        }

    private val runtimePermissions: Unit
        get() {
            val allNeededPermissions: MutableList<String?> = ArrayList()
            for (permission in requiredPermissions) {
                if (!isPermissionGranted(activity, permission)) {
                    allNeededPermissions.add(permission)
                }
            }
            if (allNeededPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    activity,
                    allNeededPermissions.toTypedArray(),
                    PERMISSION_REQUESTS
                )
            }
        }

    fun checkPermissions() {
        synchronized(lock) { if (showingDialog) return }
        if (!allPermissionsGranted()) {
            runtimePermissions
        }
    }

    private fun isPermissionGranted(
        context: Context,
        permission: String?
    ): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission!!)
            == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(
                TAG,
                context.getString(R.string.mainactivity_permissiongranted_specific, permission)
            )
            return true
        }
        Log.i(
            TAG,
            context.getString(R.string.mainactivity_permissionnotgranted_specific, permission)
        )
        return false
    }

    fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions) {
            if (!isPermissionGranted(activity, permission)) {
                setShouldShowStatus(activity, permission)
                return false
            }
        }
        return true
    }

    fun displayNeverAskAgainDialog(permission: String) {
        synchronized(lock) { if (showingDialog) return }

        val builder = AlertDialog.Builder(activity);
        val cleanerPermission = permission.removePrefix("android.permission.").replace("_", " ")

        builder.setMessage(
            activity.getString(R.string.permissionhandler_dialog_neveraskagain, cleanerPermission)
        )
        builder.setCancelable(false);
        builder.setPositiveButton(activity.getString(R.string.permissionhandler_positivebutton)) { dialog, _ ->
            dialog.dismiss();
            val intent = Intent();
            intent.action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;
            val uri = Uri.fromParts("package", activity.packageName, null);
            intent.data = uri;
            activity.startActivity(intent);
        }
        builder.setNegativeButton(activity.getString(R.string.permissionhandler_negativebutton)) { _, _ -> activity.finish() }
        builder.setOnDismissListener { synchronized(lock) { showingDialog = false } }
        builder.show()
        synchronized(lock) { showingDialog = true }
    }

    fun neverAskAgainSelected(activity: Activity, permission: String?): Boolean {
        val prevShouldShowStatus = getRationaleDisplayStatus(activity, permission)
        val currShouldShowStatus = activity.shouldShowRequestPermissionRationale(permission!!)
        return prevShouldShowStatus != currShouldShowStatus
    }

    private fun setShouldShowStatus(context: Context, permission: String?) {
        val genPrefs = context.getSharedPreferences(PERMISSION_PREFERENCES, Context.MODE_PRIVATE)
        val editor = genPrefs.edit()
        editor.putBoolean(permission, true)
        editor.apply()
    }

    private fun getRationaleDisplayStatus(context: Context, permission: String?): Boolean {
        val genPrefs = context.getSharedPreferences(PERMISSION_PREFERENCES, Context.MODE_PRIVATE)
        return genPrefs.getBoolean(permission, false)
    }

    companion object {
        private val TAG = PermissionHandler::class.simpleName
        private const val PERMISSION_PREFERENCES = "PERMISSION_PREFERENCES"
        private const val PERMISSION_REQUESTS = 1
    }
}