package com.paisano.droneinventoryscanner.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.paisano.droneinventoryscanner.R
import com.paisano.droneinventoryscanner.databinding.ActivitySessionSetupBinding
import com.paisano.droneinventoryscanner.session.SessionManager

/**
 * SessionSetupActivity - Initial screen for setting up session context
 * User must enter Cliente and Sector before proceeding to main screen
 */
class SessionSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize SessionManager
        SessionManager.init(this)
        
        // Check if session data already exists
        if (SessionManager.hasSessionData()) {
            // Skip setup and go directly to MainActivity
            startMainActivity()
            return
        }
        
        binding = ActivitySessionSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
    }

    private fun setupUI() {
        binding.btnContinuar.setOnClickListener {
            validateAndProceed()
        }
    }

    private fun validateAndProceed() {
        val cliente = binding.etCliente.text.toString().trim()
        val sector = binding.etSector.text.toString().trim()

        // Validate inputs
        when {
            cliente.isEmpty() -> {
                binding.etCliente.error = getString(R.string.field_required)
                Toast.makeText(this, R.string.please_fill_all_fields, Toast.LENGTH_SHORT).show()
            }
            sector.isEmpty() -> {
                binding.etSector.error = getString(R.string.field_required)
                Toast.makeText(this, R.string.please_fill_all_fields, Toast.LENGTH_SHORT).show()
            }
            else -> {
                // Save session data
                SessionManager.setSessionData(cliente, sector)
                Toast.makeText(this, R.string.session_configured, Toast.LENGTH_SHORT).show()
                startMainActivity()
            }
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
