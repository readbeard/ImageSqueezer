package com.example.imagesqueezer

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.PathUtils
import androidx.lifecycle.Observer
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kbeanie.multipicker.api.ImagePicker
import com.kbeanie.multipicker.api.Picker
import com.kbeanie.multipicker.api.callbacks.ImagePickerCallback
import com.kbeanie.multipicker.api.entity.ChosenImage
import java.io.File


class MainActivity : AppCompatActivity(), ImagePickerCallback,
    ActivityCompat.OnRequestPermissionsResultCallback {
    private lateinit var imagePicker: ImagePicker
    private lateinit var lvResults: ListView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var importPicturesButton: Button

    private val requiredPermissions: Array<String?>
        get() = try {
            val info = this.packageManager
                .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
                ps
            } else {
                arrayOfNulls(0)
            }
        } catch (e: Exception) {
            arrayOfNulls(0)
        }

    private val runtimePermissions: Unit
        get() {
            val allNeededPermissions: MutableList<String?> = ArrayList()
            for (permission in requiredPermissions) {
                if (!isPermissionGranted(this, permission)) {
                    allNeededPermissions.add(permission)
                }
            }
            if (allNeededPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    allNeededPermissions.toTypedArray(),
                    PERMISSION_REQUESTS
                )
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            setIntentForResultAndFinish()
        }

        initializeImagePicker()

        findViewById<Button>(R.id.button_contentmain_pick).setOnClickListener {
            imagePicker.pickImage()
        }

        lvResults = findViewById<ListView>(R.id.lvResults);
        loadingProgressBar = findViewById(R.id.progressBar_cyclic)
        importPicturesButton = findViewById(R.id.button_contentmain_pick)

        deleteDir(File(Environment.getExternalStorageDirectory().absolutePath + "/ImageSqueezer/ImageSqueezer Pictures"))

        if (!allPermissionsGranted()) {
            runtimePermissions
        }
    }

    private fun setIntentForResultAndFinish() {
        val intent = Intent()
        if (lvResults == null || lvResults!!.adapter == null) {
            finish()
            return
        }
        val results: MediaResultsAdapter = lvResults!!.adapter as MediaResultsAdapter
        val files = ArrayList<Uri>()

        val first = retrieveFileFromChosenImageAdapter(0, results)
        lvResults.visibility = View.GONE
        loadingProgressBar.visibility = View.VISIBLE
        compressFile(first, results.count == 1)
        var uri = retrieveUriFromFile(first)
        setClipDataUri(uri, intent)

        for (i in 1 until results.count) {
            val file = retrieveFileFromChosenImageAdapter(i, results)
            uri = retrieveUriFromFile(file)
            setClipDataUri(uri, intent)
            files.add(uri)
        }

        if (first.parentFile.listFiles() != null) {
            for (f in first.parentFile.listFiles()) {
                if (f.name.contains("scale")) {
                    f.delete()
                }
            }
        }

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setResult(Activity.RESULT_OK, intent)
    }

    private fun retrieveFileFromChosenImageAdapter(atIndex: Int, results: MediaResultsAdapter): File {
        return File((results.getItem(atIndex) as ChosenImage).originalPath)
    }

    private fun retrieveUriFromFile(file: File): Uri {
        return FileProvider.getUriForFile(
            this@MainActivity,
            BuildConfig.APPLICATION_ID + ".provider",
            file
        )
    }

    private fun setClipDataUri(uri: Uri, intent: Intent) {
        val mimeTypes = arrayOf("image/*")
        if (intent.clipData == null) {
            intent.clipData = ClipData(
                ClipDescription("uri", mimeTypes),
                ClipData.Item(uri)
            )
        } else {
            intent.clipData!!.addItem(ClipData.Item(uri))
        }
    }

    fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) {
            val children = dir.list() ?: return true
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
        }

        return dir.delete()
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions) {
            if (!isPermissionGranted(this, permission)) {
                return false
            }
        }
        return true
    }


    private fun initializeImagePicker() {
        imagePicker = ImagePicker(this);
        imagePicker.allowMultiple();
        imagePicker.setImagePickerCallback(this);
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Picker.PICK_IMAGE_DEVICE) {
                if (imagePicker == null) {
                    initializeImagePicker()
                }
                loadingProgressBar.visibility = View.VISIBLE
                importPicturesButton.visibility = View.GONE
                imagePicker.submit(data)
            }
        }
    }

    override fun onError(p0: String?) {
    }

    override fun onImagesChosen(images: List<ChosenImage>) {
        val adapter = MediaResultsAdapter(images, this)
        adapter.lifecycleOwner = this
        lvResults?.adapter = adapter
        adapter.finishedLoadingLiveData.observe(this, Observer {
            if (adapter.finishedLoadingLiveData.value == 1) {
                loadingProgressBar.visibility = View.GONE
            }
        })

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "Permission granted!")
        if (allPermissionsGranted()) {
            //TODO implement
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private val TAG = MainActivity::class.simpleName
        private const val PERMISSION_REQUESTS = 1

        private fun isPermissionGranted(
            context: Context,
            permission: String?
        ): Boolean {
            if (ContextCompat.checkSelfPermission(context, permission!!)
                == PackageManager.PERMISSION_GRANTED
            ) {
                Log.i(TAG, "Permission granted: $permission")
                return true
            }
            Log.i(TAG, "Permission NOT granted: $permission")
            return false
        }
    }
}