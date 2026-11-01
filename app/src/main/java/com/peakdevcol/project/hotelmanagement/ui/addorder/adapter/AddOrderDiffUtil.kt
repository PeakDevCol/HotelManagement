package com.peakdevcol.project.hotelmanagement.ui.addorder.adapter

import androidx.recyclerview.widget.DiffUtil
import com.peakdevcol.project.hotelmanagement.domain.Product


class AddOrderDiffUtil(private val oldList: List<Product>, private val newList: List<Product>) :
    DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // I can use whatever validations that i need
        return  oldList[oldItemPosition].productId == newList[newItemPosition].productId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {

        //In this method i can change the logic that it implement and use other that
        //works better to validation that items have the same content

        // I can use a method "when" to multiples validations
        return  oldList[oldItemPosition] == newList[newItemPosition]
    }


}