package com.example.imagesqueezer

import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.helpers.UndoHelper
import com.mikepenz.fastadapter.select.SelectExtension
import com.mikepenz.fastadapter.select.getSelectExtension

class ActionBarCallBack(private val activity: AppCompatActivity, private val fastAdapter:
FastAdapter<ChosenImageRvItem>) :
    androidx
    .appcompat.view
    .ActionMode
    .Callback {

    private val mUndoHelper = UndoHelper(fastAdapter, object : UndoHelper.UndoListener<ChosenImageRvItem> {
        override fun commitRemove(
            positions: Set<Int>,
            removed: ArrayList<FastAdapter.RelativeInfo<ChosenImageRvItem>>
        ) {
            Log.e("UndoHelper", "Positions: " + positions.toString() + " Removed: " + removed.size)
        }
    })

    private var selectExtension: SelectExtension<ChosenImageRvItem> = fastAdapter.getSelectExtension()


    override fun onCreateActionMode(mode: androidx.appcompat.view.ActionMode?, menu: Menu?): Boolean {
        return true
    }

    override fun onPrepareActionMode(mode: androidx.appcompat.view.ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: androidx.appcompat.view.ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.item_delete -> {
                mUndoHelper.remove(activity.findViewById(android.R.id.content),
                        "Item removed",
                        "Undo",
                        Snackbar.LENGTH_LONG,
                        selectExtension.selections)
                        .addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            override fun onShown(transientBottomBar: Snackbar?) {
                                super.onShown(transientBottomBar)
                            }

                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                super.onDismissed(transientBottomBar, event)
                                if (event != Snackbar.Callback.DISMISS_EVENT_TIMEOUT) {
                                    selectExtension.deselect()
                                }
                            }
                        })
                mode!!.finish()
            }
            R.id.item_selectall -> {
                return if (!item.isChecked) {
                    performSelectAllClick(true, mode, item)
                    true
                } else {
                    performSelectAllClick(false, mode, item)
                    true
                }
            }
        }

        return true
    }

    override fun onDestroyActionMode(mode: androidx.appcompat.view.ActionMode?) {
    }

    private fun performSelectAllClick(checked: Boolean, mode: androidx.appcompat.view.ActionMode?, item: MenuItem?) {
        if (checked) {
            item?.icon = ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_checkbox_checked, activity.theme)
            for (i in 0..fastAdapter.itemCount) {
                selectExtension.select(i)
            }
            item?.isChecked = true
        } else {
            item?.icon = ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_checkbox_unchecked,
                    activity.theme)
            selectExtension.deselect()
            item?.isChecked = false
        }
        mode?.title = selectExtension.selectedItems.size.toString()
    }
}