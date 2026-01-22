package com.paisano.droneinventoryscanner.session

import android.content.Context
import android.content.SharedPreferences

/**
 * SessionManager - Singleton to manage session context (Cliente and Sector)
 */
object SessionManager {
    
    private const val PREF_NAME = "drone_session"
    private const val KEY_CLIENTE = "cliente"
    private const val KEY_SECTOR = "sector"
    
    private var preferences: SharedPreferences? = null
    
    fun init(context: Context) {
        if (preferences == null) {
            preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }
    
    fun setSessionData(cliente: String, sector: String) {
        preferences?.edit()?.apply {
            putString(KEY_CLIENTE, cliente)
            putString(KEY_SECTOR, sector)
            apply()
        }
    }
    
    fun getCliente(): String? {
        return preferences?.getString(KEY_CLIENTE, null)
    }
    
    fun getSector(): String? {
        return preferences?.getString(KEY_SECTOR, null)
    }
    
    fun hasSessionData(): Boolean {
        return !getCliente().isNullOrEmpty() && !getSector().isNullOrEmpty()
    }
    
    fun clearSession() {
        preferences?.edit()?.clear()?.apply()
    }
    
    fun getFilenamePrefix(): String {
        val cliente = getCliente() ?: "Unknown"
        val sector = getSector() ?: "Unknown"
        return "${cliente}_${sector}"
    }
}
