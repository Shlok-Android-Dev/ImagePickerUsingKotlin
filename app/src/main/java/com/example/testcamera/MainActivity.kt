package com.example.testcamera

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.testcamera.databinding.ActivityMainBinding
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
    private val CAMERA_REQUEST_CODE = 100
    private val IMAGE_CAPTURE_CODE = 200
    private val GALLERY_REQUEST_CODE = 300
    private lateinit var currentPhotoPath: String
    private var selectedImageUri: Uri? = null


    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.camera.setOnClickListener {
            checkPermissionAndOpenCamera()
        }

        binding.galary.setOnClickListener {
            openGallery()
        }

        binding.uploadBtn.setOnClickListener {
            if (selectedImageUri != null){
                binding.progressBar.visibility = View.VISIBLE
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                    alertDialog("Are You Sure Upload Image?","Upload Image")

            }else Toast.makeText(this@MainActivity, "Please Choose Image 🏞️", Toast.LENGTH_SHORT).show()


        }
    }

    private fun checkPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        } else {
            dispatchTakePictureIntent()
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File = createImageFile()
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    photoFile
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, IMAGE_CAPTURE_CODE)
            }
        }
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                IMAGE_CAPTURE_CODE -> {
                    val imgFile = File(currentPhotoPath)
                    if (imgFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                        binding.preview.setImageBitmap(bitmap)
                        selectedImageUri = Uri.fromFile(imgFile) // Store the captured image URI
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    selectedImageUri = data?.data // Store the selected image URI
                    if (selectedImageUri != null) {
                        binding.preview.setImageURI(selectedImageUri)
                    }
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun uploadImgOnFirebase() {
        val imageUri = selectedImageUri ?: return
        imageUri.let { uri ->
            val storageRef = FirebaseStorage.getInstance().reference.child("MyUpload/${uri.lastPathSegment}")
            storageRef.putFile(uri).addOnSuccessListener {
                Toast.makeText(this, "Upload Successful", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                binding.preview.setImageURI(selectedImageUri)
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Upload Failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                binding.progressBar.visibility = View.GONE

            }
        }
    }

    private fun alertDialog(title:String, message:String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, which ->
            uploadImgOnFirebase()
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            Toast.makeText(this, "Cancle", Toast.LENGTH_SHORT).show()        }
        val dialog = builder.create()
        dialog.show()
    }
}
