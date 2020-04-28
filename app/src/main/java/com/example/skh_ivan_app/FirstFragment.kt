package com.example.skh_ivan_app

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import org.json.JSONObject
import org.w3c.dom.Text
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.lang.String.format
import java.util.*

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private val REQUEST_CODE_SPEECH_INPUT = 100

    //For camera
    private val REQUEST_IMAGE_CAPTURE = 1

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

        /*
        view.findViewById<Button>(R.id.button_toast).setOnClickListener {
            val myToast = Toast.makeText(context, "Hello Toast!", Toast.LENGTH_SHORT)
            myToast.show()
        }
         */

        //Button to initiate speaker functions
        view.findViewById<Button>(R.id.button_speak).setOnClickListener{
            speak()
        }

        //Button to take picture
        view.findViewById<Button>(R.id.button_camera).setOnClickListener {
            dispatchTakePictureIntent()
        }

        //Button to initiate web service functions
        view.findViewById<Button>(R.id.button_rest_api).setOnClickListener {

            val textview_output = view.findViewById<TextView>(R.id.textview_debug)

            // Instantiate the RequestQueue.
            val queue = Volley.newRequestQueue(context)

            // To retrieve book data
            val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Get_Book_List"

            val jsonArrayRequest = JsonArrayRequest(Request.Method.POST, url, null,
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
                            val reSizedBookThumbnail = resizeBitmap(bookThumbnail, 200, 300)
                            val bookImageView = ImageView(context)
                            bookImageView.setImageBitmap(reSizedBookThumbnail)
                            bookConstraintView.addView(bookImageView)
                            Log.i(TAG, "Manual Log, image creation card $i")
                        } catch (e: Exception){
                            Log.i(TAG, "Manual Log, image creation error: " + e.message)
                        }

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

    // To initiate camera to take picture
    private fun dispatchTakePictureIntent() {
        val pm = context!!.packageManager
        Log.i(TAG, "Manual Log, Camera function called")
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            Log.i(TAG, "Manual Log, start intent")
            takePictureIntent.resolveActivity(pm)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    // To obtain voice input and perform text to voice recognition
    private fun speak(){
        Log.i(TAG, "Manual Log, Speak function called")
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

    // To create a new book data
    private fun submitBookDetails(bookCover: String){

        val queue = Volley.newRequestQueue(context)
        Log.i(TAG, "Manual Log, book submission function called")

        val ivanTestNumber: String = "6"
        val bookTitle: String = "Cat Book $ivanTestNumber"
        val bookAuthor: String = "Cat Author $ivanTestNumber"
        val publisher: String = "Cat Publisher $ivanTestNumber"

        val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Create_Book"

        //Submit through body
        val params = HashMap<String,String>()
        params["book_Title"] = bookTitle
        params["book_Author"] = bookAuthor
        params["Publisher"] = publisher
        params["Cover"] = bookCover
        val jsonObject = JSONObject(params as Map<*, *>)

        Log.i(TAG, "Manual Log, request submitted: $url")

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            Response.Listener { response ->
                Log.i(TAG, "Manual Log $response")
            },
            Response.ErrorListener { error ->
                Log.i(TAG, "Manual Log: $error")
            })

        queue.add(request)
    }

    private fun resizeBitmap(bitmap:Bitmap, width:Int, height:Int):Bitmap{
        /*
            *** reference source developer.android.com ***
            Bitmap createScaledBitmap (Bitmap src, int dstWidth, int dstHeight, boolean filter)
                Creates a new bitmap, scaled from an existing bitmap, when possible. If the specified
                width and height are the same as the current width and height of the source bitmap,
                the source bitmap is returned and no new bitmap is created.

            Parameters
                src Bitmap : The source bitmap.
                    This value must never be null.

            dstWidth int : The new bitmap's desired width.
            dstHeight int : The new bitmap's desired height.
            filter boolean : true if the source should be filtered.

            Returns
                Bitmap : The new scaled bitmap or the source bitmap if no scaling is required.

            Throws
                IllegalArgumentException : if width is <= 0, or height is <= 0
        */
        return Bitmap.createScaledBitmap(bitmap, width, height,false)
    }

    // Listeners and callback for receiving speech and picture
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val outputText = view?.findViewById<TextView>(R.id.textview_debug)
        when (requestCode){

            //For image retrieval
            REQUEST_IMAGE_CAPTURE -> {
                Log.i(TAG, "Manual Log, received image capture response")
                if (resultCode == RESULT_OK){
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    val imageviewThumbnail = view?.findViewById<ImageView>(R.id.imageview_camera_thumbnail)
                    if (imageviewThumbnail != null) {
                        imageviewThumbnail.setImageBitmap(imageBitmap)

                        Log.i(TAG, "Manual Log, processing image to string")
                        val byteStream = ByteArrayOutputStream()
                        imageBitmap.compress(Bitmap.CompressFormat.PNG,90,byteStream)
                        val byteArray = byteStream.toByteArray()
                        val imageData = Base64.encodeToString(byteArray, Base64.DEFAULT)
                        submitBookDetails(imageData)
                    }
                }
            }

            //For speech retrieval
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