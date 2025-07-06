package com.example.moneysplit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moneysplit.moneySplit_Database.Card

class CardAdapter(
    private val cards: List<Card>,
    private val onLongClick: (Card) -> Unit
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardInfo: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cards[position]
        holder.cardInfo.text = "${card.cardType}: ${card.cardLast4} \nAdded: ${card.addedDateTime}"

        holder.itemView.setOnLongClickListener {
            onLongClick(card)
            true
        }
    }

    override fun getItemCount() = cards.size
}
