package com.spozebra.zebrarfidsledinjection.emdk


import android.content.Context
import android.util.Log
import com.symbol.emdk.EMDKManager
import com.symbol.emdk.EMDKResults
import com.symbol.emdk.ProfileManager


class EmdkEngine private constructor(context: Context, listener: IEmdkEngineListener) : EMDKManager.EMDKListener {

    //Declare a variable to store ProfileManager object
    private var profileManager: ProfileManager? = null

    //Declare a variable to store EMDKManager object
    private var emdkManager: EMDKManager? = null

    private var listener: IEmdkEngineListener? = null

    init {
        this.listener  = listener

        //The EMDKManager object will be created and returned in the callback.
        val results: EMDKResults = EMDKManager.getEMDKManager(context, this)

        //Check the return status of EMDKManager object creation.
        if (results.statusCode === EMDKResults.STATUS_CODE.SUCCESS) {
            //EMDKManager object creation success
        } else {
            //EMDKManager object creation failed
        }
    }

    fun setProfile(profileName: String?, extraData: Array<String?>?): EMDKResults? {
        return try {
            profileManager?.processProfile(
                profileName,
                ProfileManager.PROFILE_FLAG.SET,
                extraData
            )
        } catch (ex: Exception) {
            null
        }
    }

    fun getProfile(
        profileName: String?,
        extraData: Array<String?>?
    ): EMDKResults? {
        return profileManager?.processProfile(
            profileName,
            ProfileManager.PROFILE_FLAG.GET,
            extraData
        )
    }

    override fun onClosed() {

        //This callback will be issued when the EMDK closes unexpectedly.
        if (emdkManager != null) {
            emdkManager?.release()
            emdkManager = null
        }
        Log.d("Emdk", "EMDK closed unexpectedly! Please close and restart the application.")
    }

    override fun onOpened(emdkManager: EMDKManager) {

        //This callback will be issued when the EMDK is ready to use.
        Log.d("Emdk", "EMDK open success.")
        this.emdkManager = emdkManager

        //Get the ProfileManager object to process the profiles
        profileManager = emdkManager.getInstance(EMDKManager.FEATURE_TYPE.PROFILE) as ProfileManager
        listener?.emdkInitialized()
    }

    protected fun releaseResources() {

        //Clean up the objects created by EMDK manager
        if (profileManager != null) profileManager = null
        if (emdkManager != null) {
            emdkManager?.release()
            emdkManager = null
        }
    }

    companion object {
        private var instance: EmdkEngine? = null
        fun getInstance(context: Context, listener: IEmdkEngineListener): EmdkEngine? {
            if (instance == null) {
                instance = EmdkEngine(context, listener)
            } else {
                listener.emdkInitialized()
            }
            return instance
        }
    }
}