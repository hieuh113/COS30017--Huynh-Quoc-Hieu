package com.example.assignment02

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    
    private lateinit var itemImage: ImageView
    private lateinit var itemName: TextView
    private lateinit var itemRating: RatingBar
    private lateinit var itemCategory: TextView
    private lateinit var itemPrice: TextView
    private lateinit var previousButton: Button
    private lateinit var nextButton: Button
    private lateinit var borrowButton: Button
    private lateinit var bookingStatus: TextView
    
    private var currentItemIndex = 0
    private var rentalItems = mutableListOf<RentalItem>()
    private var currentBooking: Booking? = null
    
    private val bookingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val booking = result.data!!.getParcelableExtra("booking_result", Booking::class.java)
            if (booking != null) {
                currentBooking = booking
                showBookingStatus()
            }
        } else if (result.resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Booking cancelled", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        initializeViews()
        setupRentalItems()
        updateItemDisplay()
        setupClickListeners()
        
        // Check if we have a booking result from DetailActivity
        if (intent.hasExtra("booking_result")) {
            val booking = intent.getParcelableExtra("booking_result", Booking::class.java)
            if (booking != null) {
                currentBooking = booking
                showBookingStatus()
            }
        }
    }
    
    private fun initializeViews() {
        itemImage = findViewById(R.id.itemImage)
        itemName = findViewById(R.id.itemName)
        itemRating = findViewById(R.id.itemRating)
        itemCategory = findViewById(R.id.itemCategory)
        itemPrice = findViewById(R.id.itemPrice)
        previousButton = findViewById(R.id.previousButton)
        nextButton = findViewById(R.id.nextButton)
        borrowButton = findViewById(R.id.borrowButton)
        bookingStatus = findViewById(R.id.bookingStatus)
    }
    
    private fun setupRentalItems() {
        rentalItems = mutableListOf(
            RentalItem(
                name = "Electric Guitar",
                rating = 4.5f,
                category = "String Instruments",
                pricePerMonth = 50,
                imageResource = R.drawable.guitar
            ),
            RentalItem(
                name = "Digital Piano",
                rating = 4.8f,
                category = "Keyboard Instruments",
                pricePerMonth = 80,
                imageResource = R.drawable.piano
            ),
            RentalItem(
                name = "Drum Kit",
                rating = 4.2f,
                category = "Percussion",
                pricePerMonth = 120,
                imageResource = R.drawable.drums
            ),
            RentalItem(
                name = "Professional Microphone",
                rating = 4.7f,
                category = "Audio Equipment",
                pricePerMonth = 30,
                imageResource = R.drawable.microphone
            )
        )
    }
    
    private fun updateItemDisplay() {
        if (rentalItems.isNotEmpty()) {
            val currentItem = rentalItems[currentItemIndex]
            
            itemImage.setImageResource(currentItem.imageResource)
            itemName.text = currentItem.name
            itemRating.rating = currentItem.rating
            itemCategory.text = "Category: ${currentItem.category}"
            itemPrice.text = "Price: ${currentItem.pricePerMonth} credits/month"
            
            // Update navigation buttons
            previousButton.isEnabled = currentItemIndex > 0
            nextButton.isEnabled = currentItemIndex < rentalItems.size - 1
        }
    }
    
    private fun setupClickListeners() {
        previousButton.setOnClickListener {
            if (currentItemIndex > 0) {
                currentItemIndex--
                updateItemDisplay()
            }
        }
        
        nextButton.setOnClickListener {
            if (currentItemIndex < rentalItems.size - 1) {
                currentItemIndex++
                updateItemDisplay()
            }
        }
        
        borrowButton.setOnClickListener {
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("rental_item", rentalItems[currentItemIndex])
            bookingLauncher.launch(intent)
        }
    }
    
    private fun showBookingStatus() {
        currentBooking?.let { booking ->
            bookingStatus.text = "Booked: ${booking.rentalItem.name} for ${booking.rentalDuration} month(s)"
            bookingStatus.visibility = View.VISIBLE
            
            // Show toast notification
            Toast.makeText(this, "Booking confirmed for ${booking.rentalItem.name}!", Toast.LENGTH_LONG).show()
        }
    }
    
}