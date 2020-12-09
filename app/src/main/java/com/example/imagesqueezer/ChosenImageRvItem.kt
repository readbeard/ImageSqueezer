package com.example.imagesqueezer

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.kbeanie.multipicker.api.entity.ChosenImage
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter.ui.utils.FastAdapterUIUtils
import java.io.File

class ChosenImageRvItem(val chosenImage: ChosenImage): AbstractItem<ChosenImageRvItem.ViewHolder>()  {

    class ViewHolder(private val view: View): FastAdapter.ViewHolder<ChosenImageRvItem>(view) {
        private val tvName = view.findViewById<TextView>(R.id.tvName)
        private val tvCompleteMimeType = view.findViewById<TextView>(R.id.tvCompleteMimeType)
        private val ivImage = view.findViewById<ImageView>(R.id.ivImage)
        private val tvDimension = view.findViewById<TextView>(R.id.tvDimension)
        private val tvMimeType = view.findViewById<TextView>(R.id.tvMimeType)
        private val tvSize = view.findViewById<TextView>(R.id.tvSize)
        private val tvOrientation = view.findViewById<TextView>(R.id.tvOrientation)
        var context: Context = view.context

        override fun bindView(item: ChosenImageRvItem, payloads: List<Any>) {
            val file = item.chosenImage
            tvName.text = file.displayName
            tvCompleteMimeType.text = file.mimeType
            tvDimension.text = String.format(FORMAT_IMAGE_VIDEO_DIMENSIONS, file.width, file.height)
            tvMimeType.text = file.fileExtensionFromMimeTypeWithoutDot
            tvSize.text = file.getHumanReadableSize(false)
            tvOrientation.text = String.format(FORMAT_ORIENTATION, file.orientationName)

            if (file.thumbnailSmallPath != null) {
                Glide.with(context).load(Uri.fromFile(File(item.chosenImage.thumbnailSmallPath))).into(ivImage)
            }

            view.background = FastAdapterUIUtils.getSelectableBackground(context, Color.RED, true)
        }

        override fun unbindView(itemChosen: ChosenImageRvItem) {
            tvName.text = null
            tvCompleteMimeType.text = null
            tvDimension.text = null
            tvMimeType.text = null
            tvSize.text = null
            tvOrientation.text = null
        }
    }

    override val layoutRes: Int
        get() = R.layout.img_adapter
    override val type: Int
        get() = R.id.fastadapter_item_id

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    companion object {
        private val TAG = ChosenImageRvItem::class.java.simpleName
        private const val FORMAT_IMAGE_VIDEO_DIMENSIONS = "%sw x %sh"
        private const val FORMAT_ORIENTATION = "Ortn: %s"
    }
}