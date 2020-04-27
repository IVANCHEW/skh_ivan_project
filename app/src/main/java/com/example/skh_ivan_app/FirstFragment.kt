package com.example.skh_ivan_app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.w3c.dom.Text
import java.io.ByteArrayInputStream
import java.lang.Exception
import java.lang.String.format
import java.util.*

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private val REQUEST_CODE_SPEECH_INPUT = 100

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        /*
        view.findViewById<Button>(R.id.button_count).setOnClickListener {
            val myToast = Toast.makeText(context, "Count!", Toast.LENGTH_SHORT)
            myToast.show()
            countMe(view)
            // findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
         */

        view.findViewById<Button>(R.id.button_toast).setOnClickListener {
            val myToast = Toast.makeText(context, "Hello Toast!", Toast.LENGTH_SHORT)
            myToast.show()
        }

        //Button to initiate speaker functions
        view.findViewById<Button>(R.id.button_speak).setOnClickListener{
            speak()
        }

        //Button to initiate web service functions
        view.findViewById<Button>(R.id.button_rest_api).setOnClickListener {

            val ivanTestNumber: String = "2"
            val bookTitle: String = "Title $ivanTestNumber"
            val bookAuthor: String = "Author $ivanTestNumber"
            "Publisher $ivanTestNumber"
            val textview_output = view.findViewById<TextView>(R.id.textview_debug)

            // Instantiate the RequestQueue.
                val queue = Volley.newRequestQueue(context)

            //val url = "http://www.google.com"

            // To create a new book data
            /*
            val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/" +
                    "Create_Book?" +
                    "bookTitle=$bookTitle&" +
                    "publisher=$publisher&" +
                    "bookAuthor=$bookAuthor"
             */

            // To retrieve book data
            val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Get_Book_List"

            // Request a string response from the provided URL.
            /*
            val stringRequest = StringRequest(
                Request.Method.GET, url,
                Response.Listener<String> { response ->
                    // Display the first 500 characters of the response string.
                    //textview_output.text = "Response is: ${response.substring(0, 500)}"
                    textview_output.text = "Response received"
                    Log.i(TAG, "Manual Log $response")
                },
                Response.ErrorListener { textview_output.text = "That didn't work!" })
             */

            val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null,
                Response.Listener { response ->

                    // class Book(val bookTitle: String, val publisher: String, val bookAuthor: String)

                    textview_output.text = "Response received"
                    val responseLength = response.length()
                    Log.i(TAG, "Manual Log, total data: $responseLength raw: $response")
                    queue.stop()

                    //Generate cards in cardView_firstFragment
                    val bookConstraintView = view.findViewById<LinearLayout>(R.id.book_Linear_Layout)

                    //Create this array to store each textview's ID
                    val bookCardTextViewIDArray = Array(responseLength, {i-> i*1})
                    for (i in 0..(responseLength-1)){

                        //Obtain a single book object from the response
                        val bookObject = response.getJSONObject(i)

                        //Retrieve the book title from the object
                        val bookTitle = bookObject.getString("Book_Title")

                        //Create and configure the new textview
                        val tempID = View.generateViewId()
                        bookCardTextViewIDArray[i] = tempID
                        val newBookCard = TextView(context)
                        newBookCard.layoutParams
                        newBookCard.text = "Card $i, $bookTitle"
                        newBookCard.id = tempID
                        bookConstraintView.addView(newBookCard)

                        //Retrieve the thumbnail
                        try {
                            val picData = bookObject.getString("Cover")
                            val picByteArray = Base64.decode(picData,Base64.DEFAULT)
                            val decodeByte = ByteArrayInputStream(picByteArray)
                            val bookThumbnail = BitmapFactory.decodeStream(decodeByte)
                            val bookImageView = ImageView(context)
                            bookImageView.setImageBitmap(bookThumbnail)
                            bookConstraintView.addView(bookImageView)
                            Log.i(TAG, "Manual Log, image creation card $i")
                        } catch (e: Exception){
                            Log.i(TAG, "Manual Log, image creation error: " + e.message)
                        }

                        /*
                        if (i>0){
                            Log.i(TAG, "Manual Log: Called constraint")
                            val set = ConstraintSet()
                            set.connect(bookCardTextViewIDArray[i], ConstraintSet.BOTTOM, bookCardTextViewIDArray[i-1], ConstraintSet.TOP)
                            set.applyTo(bookConstraintView)
                        }
                         */

                    }
                },
                Response.ErrorListener { error ->
                    textview_output.text = "Error"
                    Log.i(TAG, "Manual Log error")
                    Log.e(TAG, "Manual Log $error")
                    queue.stop()
                }
            )

            // Add the request to the RequestQueue.
            //queue.add(stringRequest)
            queue.add(jsonArrayRequest)
        }

        /*
        val setZero = view.findViewById<TextView>(R.id.textview_count)
        var zeroHolder = 0
        setZero.text = zeroHolder.toString()
         */
    }

    /*
    private fun countMe(view: View){
        val showCountTextView = view.findViewById<TextView>(R.id.textview_count)
        val countString = showCountTextView.text.toString()
        var count = countString.toInt()
        count++
        showCountTextView.text = count.toString()
    }
     */

    private fun speak(){
        val mIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        mIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        mIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        mIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hi speak something")
        Toast.makeText(context, "Start receiving speech", Toast.LENGTH_SHORT).show()
        try{
            // If there is no error in showing speechTotext dialogue
            startActivityForResult(mIntent, REQUEST_CODE_SPEECH_INPUT)
        } catch (e: Exception){
            // If there is error
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val outputText = view?.findViewById<TextView>(R.id.textview_debug)
        when (requestCode){
            REQUEST_CODE_SPEECH_INPUT -> {
                if (resultCode== Activity.RESULT_OK && null != data){

                    //get text from result
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    if (outputText != null) {
                        outputText.text = result[0]
                    }

                }
            }
        }
    }

}