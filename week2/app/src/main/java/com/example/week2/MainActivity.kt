package com.example.week2

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileWriter
import java.io.FileInputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val loginButton: Button = findViewById(R.id.loginButton)
        val readButton: Button = findViewById(R.id.readButton)
        val txtInfo: TextView = findViewById(R.id.txtInfo)

        loginButton.setOnClickListener {
            // Thêm dòng "hello world" vào file test.txt
            try {
                val file = File(filesDir, "test.txt")
                val fileWriter = FileWriter(file, true) // true để append thay vì ghi đè
                fileWriter.append("hello world\n")
                fileWriter.close()
                Toast.makeText(this, "Đã thêm 'hello world' vào test.txt", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(this, "Lỗi khi ghi file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        readButton.setOnClickListener {
            try {
                val file = File(filesDir, "test.txt")
                if (file.exists()) {
                    val fileInputStream = FileInputStream(file)
                    val data = ByteArray(1024)
                    val bytesRead = fileInputStream.read(data)
                    fileInputStream.close()
                    
                    if (bytesRead > 0) {
                        val content = String(data, 0, bytesRead, Charsets.UTF_8)
                        txtInfo.text = content
                        Toast.makeText(this, "Đã đọc file thành công", Toast.LENGTH_SHORT).show()
                    } else {
                        txtInfo.text = "File rỗng"
                    }
                } else {
                    txtInfo.text = "File test.txt không tồn tại"
                    Toast.makeText(this, "File không tồn tại", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                txtInfo.text = "Lỗi khi đọc file: ${e.message}"
                Toast.makeText(this, "Lỗi khi đọc file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}












