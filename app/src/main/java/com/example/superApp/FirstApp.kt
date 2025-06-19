package com.example.superApp;

import android.os.Bundle
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast

import android.widget.EditText
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ExMainActivity : AppCompatActivity() {
    
    fun checkLink(str: String): Boolean
    {
        var expected = "https://"
        if(str.take(8) == expected)
            return true;
        return false;
    } 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this)
        textView.text = "Hello from Kotlin!"

        val editText = EditText(this).apply {
            hint = "Enter text here"
            textSize = 18f
        }

        val button = Button(this)
        button.text = "Click me!"

        button.setOnClickListener {
            if(checkLink(editText.text.toString()))
            {
                
            }else{
                textView.text = "Nu ati dat un link"
            }
        }        

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(editText)
            addView(button)
            addView(textView)
        }



        setContentView(layout)

    }
}
