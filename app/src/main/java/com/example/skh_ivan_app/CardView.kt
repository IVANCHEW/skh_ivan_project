package com.example.skh_ivan_app

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class CardView(context: Context?) : View(context) {
    companion object CreatedViews{
    }

    val cardTextView = TextView(context)

    val textviewLayoutParam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.MATCH_PARENT)
    val imageViewLayoutParam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT)
    val cardLayoutParam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT)

    init{
        Log.i(ContentValues.TAG, "Manual Log, CardView Class, Init")
    }

    fun setTextview(inputText: String){
        cardTextView.text = inputText
    }

}