package com.example.assignment21

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class DetailActivity : AppCompatActivity() {
    
    private lateinit var detailImage: ImageView
    private lateinit var detailItemName: TextView
    private lateinit var detailPrice: TextView
    private lateinit var customerNameInput: TextInputEditText
    private lateinit var customerEmailInput: TextInputEditText
    private lateinit var durationRadioGroup: RadioGroup
    private lateinit var totalCostText: TextView
    private lateinit var cancelButton: Button
    private lateinit var saveButton: Button
    
    private lateinit var rentalItem: RentalItem
    private var selectedDuration = 1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        
        // Get the rental item from intent
        rentalItem = intent.getParcelableExtra("rental_item", RentalItem::class.java) ?: return
        
        initializeViews()
        setupItemDisplay()
        setupClickListeners()
        updateTotalCost()
    }
    
    private fun initializeViews() {
        detailImage = findViewById(R.id.detailImage)
        detailItemName = findViewById(R.id.detailItemName)
        detailPrice = findViewById(R.id.detailPrice)
        customerNameInput = findViewById(R.id.customerNameInput)
        customerEmailInput = findViewById(R.id.customerEmailInput)
        durationRadioGroup = findViewById(R.id.durationRadioGroup)
        totalCostText = findViewById(R.id.totalCostText)
        cancelButton = findViewById(R.id.cancelButton)
        saveButton = findViewById(R.id.saveButton)
    }
    
    private fun setupItemDisplay() {
        detailImage.setImageResource(rentalItem.imageResource)
        detailItemName.text = rentalItem.name
        detailPrice.text = "Price: ${rentalItem.pricePerMonth} credits/month"
    }
    
    private fun setupClickListeners() {
        cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        
        saveButton.setOnClickListener {
            if (validateInputs()) {
                createBooking()
            }
        }
        
        durationRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedDuration = when (checkedId) {
                R.id.oneMonthRadio -> 1
                R.id.threeMonthsRadio -> 3
                R.id.sixMonthsRadio -> 6
                else -> 1
            }
            updateTotalCost()
        }
    }
    
    private fun updateTotalCost() {
        val totalCost = rentalItem.pricePerMonth * selectedDuration
        totalCostText.text = "Total Cost: $totalCost credits"
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Validate name
        val name = customerNameInput.text.toString().trim()
        if (TextUtils.isEmpty(name)) {
            customerNameInput.error = "Name is required"
            isValid = false
        } else if (name.length < 2) {
            customerNameInput.error = "Name must be at least 2 characters"
            isValid = false
        } else {
            customerNameInput.error = null
        }
        
        // Validate email
        val email = customerEmailInput.text.toString().trim()
        if (TextUtils.isEmpty(email)) {
            customerEmailInput.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            customerEmailInput.error = "Please enter a valid email address"
            isValid = false
        } else {
            customerEmailInput.error = null
        }
        
        // Validate credit limit (assuming user has 500 credits max)
        val totalCost = rentalItem.pricePerMonth * selectedDuration
        if (totalCost > 500) {
            Toast.makeText(this, "Insufficient credits! Maximum 500 credits allowed.", Toast.LENGTH_LONG).show()
            isValid = false
        }
        
        return isValid
    }
    
    private fun createBooking() {
        val name = customerNameInput.text.toString().trim()
        val email = customerEmailInput.text.toString().trim()
        val totalCost = rentalItem.pricePerMonth * selectedDuration
        
        val booking = Booking(
            rentalItem = rentalItem,
            customerName = name,
            customerEmail = email,
            rentalDuration = selectedDuration,
            totalCost = totalCost,
            isConfirmed = true
        )
        
        // Return the booking to MainActivity
        val resultIntent = Intent()
        resultIntent.putExtra("booking_result", booking)
        setResult(RESULT_OK, resultIntent)
        
        // Show success message
        Toast.makeText(this, "Booking confirmed for ${rentalItem.name}!", Toast.LENGTH_LONG).show()
        
        finish()
    }
}

