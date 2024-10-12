package com.peakdevcol.project.hotelmanagement.ui.addorder.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.peakdevcol.project.hotelmanagement.databinding.ItemAddOrderBinding
import com.peakdevcol.project.hotelmanagement.domain.Product

class AddOrderAdapter(private var list: List<Product>, private val onItemDeleted: (Product) -> Unit) :
    RecyclerView.Adapter<AddOrderAdapter.MyViewHolder>() {


    fun updateList(newList: List<Product>) {
        val allOrderDiff = AddOrderDiffUtil(list, newList)
        val result = DiffUtil.calculateDiff(allOrderDiff)
        list = newList
        result.dispatchUpdatesTo(this)
    }


    /**
     * Este método se llama cuando el RecyclerView necesita una nueva vista para representar un ítem.
     * Aquí es donde se infla el diseño de los ítems del RecyclerView.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding =
            ItemAddOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount() = list.size


    /**
     * Este método se llama para asignar datos a las vistas infladas.
     * Usa el ViewHolder proporcionado para configurar la vista del ítem en la posición específica.
     */

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = list[position]
        holder.bind(item, onItemDeleted)
    }


    class MyViewHolder(private val binding: ItemAddOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Product, onItemDeleted: (Product) -> Unit) {
            binding.apply {
                val productName = "${item.productName} x${item.productQuantity}"
                addOrderProductName.text = productName
                addOrderAmount.text = item.productAmount
                imageDelete.setOnClickListener {
                    onItemDeleted.invoke(item)
                }
            }
        }
    }


}