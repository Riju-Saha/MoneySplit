package com.example.moneysplit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CardTableAdapter(
    private val cards: List<Card>,
    private val onLongClick: (Card) -> Unit
) : RecyclerView.Adapter<CardTableAdapter.CardViewHolder>() {

    class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val slNoText: TextView = view.findViewById(R.id.slNoText)
        val cardTypeText: TextView = view.findViewById(R.id.cardTypeText)
        val cardNumberText: TextView = view.findViewById(R.id.cardNumberText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_table_row, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cards[position]

        // ✅ Debug Log: check if data is mapped correctly
        println("DEBUG: Binding card at pos=$position -> ${card.cardType} ${card.cardLast4}")

        holder.slNoText.text = (position + 1).toString()
        holder.cardTypeText.text = card.cardType

        // Masking or formatting can be done here if needed
        holder.cardNumberText.text = card.cardLast4

        holder.itemView.setOnLongClickListener {
            println("DEBUG: Long click on card -> ${card.cardType} ${card.cardLast4}")
            onLongClick(card)
            true
        }
    }

    override fun getItemCount(): Int {
        println("DEBUG: Adapter has ${cards.size} cards")
        return cards.size
    }
}
