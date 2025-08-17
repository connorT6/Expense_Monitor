package com.connort6.expensemonitor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ImagePickerScreen() {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    var compressedImageFile by remember { mutableStateOf<File?>(null) }
    var compressedImageFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Launcher for single image selection
    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            selectedImageUri = uri
            compressedImageFile = null // Reset previous compressed file
        }
    )

    // Launcher for multiple image selection
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5),
        onResult = { uris ->
            selectedImageUris = uris
            compressedImageFiles = emptyList() // Reset previous compressed files
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Make column scrollable
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            singlePhotoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }) {
            Text("Pick Single Photo")
        }

        selectedImageUri?.let { uri ->
            Spacer(modifier = Modifier.height(10.dp))
            Text("Selected Single Image:")
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = "Selected image",
                modifier = Modifier.size(150.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                isLoading = true
                coroutineScope.launch {
                    val result = withContext(Dispatchers.IO) { // Run compression in IO dispatcher
                        ImageCompressor.compressUriToWebP(context, uri, quality = 75)
                    }
                    compressedImageFile = result
                    isLoading = false
                }
            }) {
                Text("Compress to WebP")
            }
        }

        compressedImageFile?.let { file ->
            Spacer(modifier = Modifier.height(10.dp))
            Text("Compressed WebP Image:")
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(data = file) // Load from File
                        .build()
                ),
                contentDescription = "Compressed WebP image",
                modifier = Modifier.size(150.dp),
                contentScale = ContentScale.Crop
            )
            Text(
                "Saved to: ${file.absolutePath}",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            multiplePhotoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly) // Changed to ImageOnly for simplicity here
            )
        }) {
            Text("Pick Multiple Photos (Max 5)")
        }

        if (selectedImageUris.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text("Selected Multiple Images:")
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                selectedImageUris.forEach { uri ->
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .size(100.dp)
                            .padding(4.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                isLoading = true
                coroutineScope.launch {
                    val results = mutableListOf<File>()
                    withContext(Dispatchers.IO) {
                        selectedImageUris.forEach { uri ->
                            ImageCompressor.compressUriToWebP(context, uri, quality = 75)?.let {
                                results.add(it)
                            }
                        }
                    }
                    compressedImageFiles = results
                    isLoading = false
                }
            }) {
                Text("Compress All to WebP")
            }
        }

        if (compressedImageFiles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text("Compressed Multiple WebP Images:")
            Column { // Changed to Column for better layout of multiple compressed images
                compressedImageFiles.forEach { file ->
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(LocalContext.current)
                                .data(data = file) // Load from File
                                .build()
                        ),
                        contentDescription = "Compressed WebP image",
                        modifier = Modifier
                            .size(100.dp)
                            .padding(4.dp),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        "Saved to: ${file.name}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}


object ImageCompressor {

    private const val TAG = "ImageCompressor"

    // Function to compress an image from a URI to WebP format
    fun compressUriToWebP(context: Context, imageUri: Uri, quality: Int = 80): File? {
        return try {
            // 1. Get Bitmap from Uri
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URI: $imageUri")
                return null
            }

            // 2. Create a file to save the compressed image (app's cache directory)
            val outputDir =
                context.cacheDir // Or use context.filesDir for more permanent app-specific storage
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputFile = File(outputDir, "WEBP_$timeStamp.webp")

            // 3. Compress and save the bitmap
            FileOutputStream(outputFile).use { out ->
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY // Or WEBP_LOSSLESS for higher quality
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                originalBitmap.compress(format, quality, out)
                Log.d(TAG, "Image compressed successfully to: ${outputFile.absolutePath}")
            }
            originalBitmap.recycle() // Recycle the bitmap to free memory

            outputFile

        } catch (e: IOException) {
            Log.e(TAG, "Error compressing image: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "An unexpected error occurred: ${e.message}", e)
            null
        }
    }
}
