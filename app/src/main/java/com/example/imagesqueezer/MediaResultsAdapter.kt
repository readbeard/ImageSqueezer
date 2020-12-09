package com.example.imagesqueezer


import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.bumptech.glide.annotation.GlideModule
import com.kbeanie.multipicker.api.entity.ChosenFile
import com.kbeanie.multipicker.api.entity.ChosenImage
import java.io.File


@GlideModule
class MediaResultsAdapter(private val files: List<ChosenFile>, private val context: Context) :
    BaseAdapter() {

    lateinit var lifecycleOwner: LifecycleOwner
    var finishedLoadingLiveData: MutableLiveData<Int> = MutableLiveData(0)

    override fun getCount(): Int {
        return files.size
    }

    override fun getItem(position: Int): Any {
        return files[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var mConvertView = convertView
        Log.d(TAG, "getView: " + files.size)
        val file = getItem(position) as ChosenFile
        val itemViewType = getItemViewType(position)
        if (mConvertView == null) {
            when (itemViewType) {
                TYPE_IMAGE -> mConvertView = LayoutInflater.from(
                    context
                ).inflate(R.layout.img_adapter, null)
                TYPE_FILE -> mConvertView = LayoutInflater.from(
                    context
                ).inflate(R.layout.file_adapter, null)
            }
        }
        when (itemViewType) {
            TYPE_IMAGE -> showImage(file, mConvertView!!)
        }

        if (position == files.size - 1) {
            finishedLoadingLiveData.value = 1
        }

        return mConvertView!!
    }


    private fun showImage(file: ChosenFile, view: View) {
        val image = file as ChosenImage

        val tvName = view.findViewById<View>(R.id.tvName) as TextView
        tvName.text = file.getDisplayName()
        val tvCompleteMimeType = view.findViewById<View>(R.id.tvCompleteMimeType) as TextView
        tvCompleteMimeType.text = file.getMimeType()
        val ivImage = view.findViewById<View>(R.id.ivImage) as ImageView

        if (image.thumbnailSmallPath != null) {
            Glide.with(context).load(Uri.fromFile(File(image.thumbnailSmallPath))).into(ivImage)
        }
        val tvDimension = view.findViewById<View>(R.id.tvDimension) as TextView
        tvDimension.text = String.format(FORMAT_IMAGE_VIDEO_DIMENSIONS, image.width, image.height)
        val tvMimeType = view.findViewById<View>(R.id.tvMimeType) as TextView
        tvMimeType.text = file.getFileExtensionFromMimeTypeWithoutDot()
        val tvSize = view.findViewById<View>(R.id.tvSize) as TextView
        tvSize.text = file.getHumanReadableSize(false)
        val tvOrientation = view.findViewById<View>(R.id.tvOrientation) as TextView
        tvOrientation.text =
            String.format(FORMAT_ORIENTATION, image.orientationName)

        view.setOnClickListener {
            Log.i(TAG, "onClick: Tapped: " + image.originalPath)
        }
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getItemViewType(position: Int): Int {
        val type = (getItem(position) as ChosenFile).type
        when (type) {
            "image" -> return TYPE_IMAGE
            "file" -> return TYPE_FILE
        }
        return TYPE_FILE
    }



    companion object {
        private val TAG = MediaResultsAdapter::class.java.simpleName
        private const val TYPE_IMAGE = 0
        private const val TYPE_FILE = 2
        private const val FORMAT_IMAGE_VIDEO_DIMENSIONS = "%sw x %sh"
        private const val FORMAT_ORIENTATION = "Ortn: %s"
    }
}
