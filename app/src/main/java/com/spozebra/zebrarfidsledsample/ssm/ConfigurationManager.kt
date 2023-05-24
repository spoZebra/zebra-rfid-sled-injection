package com.spozebra.zebrarfidsledsample.ssm

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.database.Cursor
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.spozebra.zebrarfidsledsample.Constants
import java.nio.charset.StandardCharsets

class ConfigurationManager(context: Context) {
    var cpUri: Uri
    var context: Context
    var APP_PACKAGE_NAME = ""
    private var APP_SIGNATURE: String? = ""

    init {
        this.context = context
        cpUri = Uri.parse(Constants.SSM_AUTHORITY)
        APP_PACKAGE_NAME = context.packageName
        APP_SIGNATURE = getPublicSignature(context)
    }

    @Suppress("DEPRECATION")
    @SuppressLint("PackageManagerGetSignatures")
    private fun getPublicSignature(context: Context): String? {
        var appSignature: String? = null
        try {
            val signature: Signature = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            ).signatures[0]
            if (signature != null) {
                val data: ByteArray = Base64.encode(signature.toByteArray(), Base64.DEFAULT)
                val sign = String(data, StandardCharsets.UTF_8)
                appSignature = sign.replace("\\s+".toRegex(), "")
                // Util.log(TAG, LogType.INFO, "getPublicSignature: " + appSignature);
            } else {
                Log.e(TAG, "getPublicSignature error")
            }
        } catch (e: Exception) {
            Log.e(TAG, "getPublicSignature error" + e.message)
        }
        return appSignature
    }

    private fun buildContentValues(paramName: String, value: Any): ContentValues {
        val values = ContentValues()
        values.put(Constants.SSM_TARGET_APP_PACKAGE, "{\"pkgs_sigs\": [{\"pkg\":\"$APP_PACKAGE_NAME\",\"sig\":\"$APP_SIGNATURE\"}]}")
        values.put(Constants.DATA_NAME, paramName)
        values.put(Constants.DATA_VALUE, value.toString())
        values.put(Constants.DATA_INPUT_FORM, "1") //plaintext =1, encrypted=2
        values.put(Constants.DATA_OUTPUT_FORM, "1") //plaintext=1, encrypted=2, keystrokes=3
        values.put(Constants.DATA_PERSIST_REQUIRED, "false")
        values.put(Constants.MULTI_INSTANCE_REQUIRED, "true")
        return values
    }

    fun updateValue(paramName: String, value: Any) {
        try {
            Constants.SSM_TARGET_APP_PACKAGE + "= '" + context.packageName + "'"
            ContentValues()
            context.contentResolver.update(cpUri, buildContentValues(paramName, value), null, null)
        } catch (e: Exception){
            Log.e(TAG, "Error: " + e.message)
        }
    }

    @SuppressLint("Range")
    operator fun getValue(paramName: String, defaultValue: Any): String {
        val cpUriQuery: Uri = Uri.parse(Constants.SSM_AUTHORITY + "/[" + context.packageName + "]")
        val selection =
            Constants.SSM_TARGET_APP_PACKAGE + "= '" + context.packageName + "'" + "AND " + "data_persist_required = '" + "false" + "'" +
                    "AND " + "multi_instance_required = '" + "true" + "'"
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(cpUriQuery, null, selection, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error: " + e.message)
        }
        try {
            if (cursor != null && cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    val name: String =
                        java.lang.String.valueOf(cursor.getString(cursor.getColumnIndex(Constants.DATA_NAME)))
                    if (name == paramName) {
                        return cursor.getString(cursor.getColumnIndex(Constants.DATA_VALUE))
                    }
                    cursor.moveToNext()
                }
            }
            // Param does not exists, create it
            context.contentResolver.insert(cpUri, buildContentValues(paramName, defaultValue))
        } catch (e: Exception) {
            Log.e(TAG, "Query data error: " + e.message)
        } finally {
            cursor?.close()
        }
        return defaultValue.toString()
    }

    companion object {
        val TAG = ConfigurationManager::class.java.simpleName
    }
}