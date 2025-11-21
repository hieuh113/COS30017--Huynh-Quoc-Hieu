package com.example.myapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Call the coroutines function
        callThread()
    }
    
    fun callThread() {
        // Launch coroutine in the main scope
        lifecycleScope.launch {
            coroutineScope {
                // Use async to get results from concurrent operations
                val deferred1 = async(Dispatchers.IO) {
                    thread1()
                }
                
                val deferred2 = async(Dispatchers.IO) {
                    thread2()
                }
                
                // Await results from both async operations
                val result1 = deferred1.await()
                val result2 = deferred2.await()
                
                println("Result from thread1: $result1")
                println("Result from thread2: $result2")
                println("Both threads completed!")
            }
        }
    }
    

    suspend fun thread1(): String {
        delay(1000) // Simulate 1 second of work
        return "Thread 1 completed"
    }

    suspend fun thread2(): String {
        delay(1700) // Simulate 1.5 seconds of work
        return "Thread 2 completed"
    }
}
