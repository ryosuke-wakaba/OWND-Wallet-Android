package com.ownd_project.tw2023_wallet_android

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView


class WalkthroughAdapter(
    private val images: List<Int>,
    private val itemClickListener: (action: Action) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalkthroughViewHolder {
//        val imageView = ImageView(parent.context)
//        imageView.layoutParams = ViewGroup.LayoutParams(
//            ViewGroup.LayoutParams.MATCH_PARENT,
//            ViewGroup.LayoutParams.MATCH_PARENT
//        )
//        return WalkthroughViewHolder(imageView)
//    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_FINAL) {
            // 最後のページ
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.walkthrough_final_page, parent, false)
            FinalViewHolder(view, itemClickListener)
        } else {
            // 通常のページ
            val imageView = ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            NormalViewHolder(imageView)
        }
    }

    override fun getItemCount(): Int = images.size
    override fun getItemViewType(position: Int): Int {
        return if (position == images.size - 1) TYPE_FINAL else TYPE_NORMAL
    }

    enum class Action {
        NEXT, PREVIOUS, SKIP_TO_FINAL, NONE, GOTO_MAIN, RESTORE
    }

    companion object {
        private const val TYPE_NORMAL = 0
        private const val TYPE_FINAL = 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NormalViewHolder) {
            holder.imageView.setImageResource(images[position])
            holder.imageView.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    val x = event.x
                    val y = event.y

                    val halfWidth = v.width / 2
                    val fifthHeight = v.height / 5
                    val thirdWidthFromRight = 2 * v.width / 3
                    val quarterHeight = v.height / 4

                    when (position) {
                        in 0..2 -> { // 1から3枚目の画像
                            when {
                                y > (v.height - fifthHeight) && x > thirdWidthFromRight -> itemClickListener(
                                    Action.SKIP_TO_FINAL
                                )

                                x < halfWidth -> itemClickListener(Action.PREVIOUS)
                                else -> itemClickListener(Action.NEXT)
                            }
                        }

                        3 -> { // 4枚目の画像
                            when {
                                y > quarterHeight -> itemClickListener(Action.GOTO_MAIN)
                                x < halfWidth -> itemClickListener(Action.PREVIOUS)
                                else -> itemClickListener(Action.NONE)
                            }
                        }
                    }
                }
                true
            }
        } else if (holder is FinalViewHolder) {
            // 最後のページの特別な設定（必要な場合）
        }
        // holder.imageView.setImageResource(images[position])

    }

    class NormalViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    class FinalViewHolder(view: View, itemClickListener: (action: Action) -> Unit) :
        RecyclerView.ViewHolder(view) {
        private val linkText1: TextView = view.findViewById(R.id.link_privacy_policy)
        private val linkText2: TextView = view.findViewById(R.id.link_term_of_use)
        private val linkText3: TextView = view.findViewById(R.id.restore_from_backup)
        private val context: Context = view.context
        private val imageArrowBack: ImageView = view.findViewById(R.id.imageArrowBack)

        init {
            // ここにボタンなどのクリックリスナーを設定
            view.findViewById<Button>(R.id.btnStart).setOnClickListener {
                itemClickListener(Action.GOTO_MAIN)
            }
            imageArrowBack.setOnClickListener {
                // ViewPager2の現在のアイテムを一つ前に設定
                itemClickListener(Action.PREVIOUS)
            }
            val htmlText1 = context.getString(R.string.privacy_policy_link)
            linkText1.text = HtmlCompat.fromHtml(htmlText1, HtmlCompat.FROM_HTML_MODE_LEGACY)
            linkText1.setOnClickListener {
                val url = context.getString(R.string.PRIVACY_POLICY_URL)
                openUrlInCustomTab(url)
            }

            val htmlText2 = context.getString(R.string.term_of_use_link)
            linkText2.text = HtmlCompat.fromHtml(htmlText2, HtmlCompat.FROM_HTML_MODE_LEGACY)
            linkText2.setOnClickListener {
                val url = context.getString(R.string.TERM_OF_USE_URL)
                openUrlInCustomTab(url)
            }

            val htmlText3 = context.getString(R.string.restore_from_backup)
            linkText3.text = HtmlCompat.fromHtml(htmlText3, HtmlCompat.FROM_HTML_MODE_LEGACY)
            linkText3.setOnClickListener {
                itemClickListener(Action.RESTORE)
            }
        }

        private fun openUrlInCustomTab(url: String) {
            Log.d("SettingsFragment", "Opening URL in Custom Tab: $url")
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(context, Uri.parse(url))
        }
    }
}
